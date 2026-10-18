package com.beanote.recommendation.eval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import com.beanote.bean.domain.ProcessMethod;
import com.beanote.recommendation.eval.SyntheticWorld.Bean;
import com.beanote.recommendation.eval.SyntheticWorld.Note;
import com.beanote.recommendation.model.Candidate;
import com.beanote.recommendation.model.CollabScorer;
import com.beanote.recommendation.model.CollabSignal;
import com.beanote.recommendation.model.Neighbor;
import com.beanote.recommendation.model.NeighborFinder;
import com.beanote.recommendation.model.RecommendationScorer;
import com.beanote.recommendation.model.ScoreWeights;
import com.beanote.recommendation.model.ScoredCandidate;
import com.beanote.recommendation.model.UserContext;
import com.beanote.taste.config.RecommendationProperties;
import com.beanote.taste.model.BeanFeatures;
import com.beanote.taste.model.BeanVectorCalculator;
import com.beanote.taste.model.TasteVectorCalculator;
import com.beanote.taste.model.TasteVectorCalculator.NoteSignal;

/**
 * 오프라인 평가 (기획서 5-5).
 *
 * <ol>
 * <li>노트 10개 이상인 사용자마다 최근 노트의 20%를 숨긴다 (시간 순서를 지킨 분할).</li>
 * <li>원두 벡터·취향 벡터·이웃은 남은 학습용 노트만으로 계산한다 (숨긴 노트가 새지 않게).</li>
 * <li>학습 노트를 쓴 원두를 뺀 전체 원두에서 상위 10개를 추천한다.</li>
 * <li>숨긴 노트 중 별점 4.0 이상인 원두를 정답으로 보고 Precision@5, Recall@10, NDCG@10 을 잰다.</li>
 * </ol>
 * 운영 코드와 같은 순수 계산 클래스(BeanVectorCalculator, TasteVectorCalculator, NeighborFinder, CollabScorer,
 * RecommendationScorer)를 그대로 쓴다.
 */
public class OfflineEvaluator {

	static final double RELEVANT_RATING = 4.0;
	static final double HOLDOUT_RATIO = 0.2;
	static final int MIN_NOTES = 10;
	static final int K = 10;

	public record Metrics(double precisionAt5, double recallAt10, double ndcgAt10, int users) {
	}

	private final SyntheticWorld world;
	private final RecommendationProperties properties;
	private final BeanVectorCalculator beanVectors;
	private final TasteVectorCalculator tasteVectors;

	private final Map<Long, List<Note>> train = new HashMap<>();
	private final Map<Long, List<Note>> test = new HashMap<>();
	private final Map<Long, double[]> beanVector = new HashMap<>();
	private final Map<Long, double[]> userTaste = new HashMap<>();
	private Map<Long, List<Neighbor>> neighbors = Map.of();
	private final Map<Long, Map<Long, Double>> trainRatings = new HashMap<>();

	public OfflineEvaluator(SyntheticWorld world, RecommendationProperties properties,
			BeanVectorCalculator beanVectors, TasteVectorCalculator tasteVectors) {
		this.world = world;
		this.properties = properties;
		this.beanVectors = beanVectors;
		this.tasteVectors = tasteVectors;
		split();
		buildBeanVectors();
		buildUserModels();
	}

	// ---- 준비 ----

	private void split() {
		Map<Long, List<Note>> byUser = world.notes.stream()
				.collect(Collectors.groupingBy(Note::userId, LinkedHashMap::new, Collectors.toList()));
		byUser.forEach((userId, notes) -> {
			List<Note> sorted = notes.stream().sorted(Comparator.comparing(Note::tastedOn)).toList();
			int holdout = notes.size() >= MIN_NOTES ? (int) Math.ceil(notes.size() * HOLDOUT_RATIO) : 0;
			train.put(userId, sorted.subList(0, sorted.size() - holdout));
			test.put(userId, sorted.subList(sorted.size() - holdout, sorted.size()));
			Map<Long, Double> ratings = new HashMap<>();
			train.get(userId).forEach(n -> ratings.put(n.beanId(), n.rating()));
			trainRatings.put(userId, ratings);
		});
	}

	private void buildBeanVectors() {
		Map<Long, List<Note>> byBean = train.values().stream().flatMap(List::stream)
				.collect(Collectors.groupingBy(Note::beanId));
		for (Bean bean : world.beans) {
			List<Note> notes = byBean.getOrDefault(bean.id(), List.of());
			double[] avg = null;
			Map<String, Integer> noteCategories = new HashMap<>();
			if (!notes.isEmpty()) {
				avg = new double[5];
				for (Note note : notes) {
					for (int i = 0; i < 5; i++) {
						avg[i] += note.axes()[i] / notes.size();
					}
					note.categories().forEach((code, count) -> noteCategories.merge(code, count, Integer::sum));
				}
			}
			beanVector.put(bean.id(), beanVectors.calculate(new BeanFeatures(bean.process(), bean.roast(),
					bean.officialCategories(), notes.size(), avg, noteCategories)));
		}
	}

	private void buildUserModels() {
		train.forEach((userId, notes) -> {
			List<NoteSignal> signals = notes.stream()
					.map(n -> new NoteSignal(n.rating(), n.tastedOn(), beanVector.get(n.beanId())))
					.toList();
			double[] vector = tasteVectors.calculate(signals, null, SyntheticWorld.TODAY).vector();
			if (vector != null) {
				userTaste.put(userId, vector);
			}
		});
		neighbors = new NeighborFinder(properties.neighborCount(), properties.sectionWeights()).findAll(userTaste);
	}

	// ---- 평가 대상 ----

	/** 추천기 (가중치만 바꿔 콘텐츠 단독·협업 단독·혼합을 만든다) */
	public Metrics evaluateScorer(ScoreWeights weights) {
		RecommendationScorer scorer = new RecommendationScorer(weights, properties.sectionWeights());
		boolean collab = weights.collab() > 0 && world.users.size() >= properties.collabMinUsers();
		return evaluate(userId -> {
			List<Note> history = train.get(userId);
			Set<Long> seen = history.stream().map(Note::beanId).collect(Collectors.toSet());
			Set<String> origins = new HashSet<>();
			Set<ProcessMethod> processes = new HashSet<>();
			history.forEach(n -> {
				Bean b = world.beanById.get(n.beanId());
				origins.add(b.origin());
				processes.add(b.process());
			});
			Map<Long, CollabSignal> signals = collab
					? CollabScorer.score(neighbors.getOrDefault(userId, List.of()), trainRatings)
					: Map.of();
			UserContext context = new UserContext(userTaste.get(userId), origins, processes, Set.of(), signals,
					collab, false);
			List<Candidate> candidates = world.beans.stream()
					.filter(b -> !seen.contains(b.id()))
					.map(b -> new Candidate(b.id(), beanVector.get(b.id()), Set.of(b.origin()), b.origin(),
							b.process(), b.roast()))
					.toList();
			return scorer.rank(candidates, context, K).stream().map(ScoredCandidate::candidate)
					.map(Candidate::beanId).toList();
		});
	}

	/** 기준선 ①: 인기순 (학습 노트 수) */
	public Metrics evaluatePopularity() {
		Map<Long, Long> counts = train.values().stream().flatMap(List::stream)
				.collect(Collectors.groupingBy(Note::beanId, Collectors.counting()));
		return evaluateGlobalRanking(Comparator.comparingLong((Bean b) -> counts.getOrDefault(b.id(), 0L)).reversed()
				.thenComparingLong(Bean::id));
	}

	/** 기준선 ②: 평균 평점순. 노트가 적은 원두의 우연한 고득점을 막으려 전체 평균 쪽으로 당긴다 (베이즈 평균, m=3) */
	public Metrics evaluateAverageRating() {
		List<Note> all = train.values().stream().flatMap(List::stream).toList();
		double globalMean = all.stream().mapToDouble(Note::rating).average().orElse(3);
		Map<Long, List<Double>> byBean = all.stream().collect(
				Collectors.groupingBy(Note::beanId, Collectors.mapping(Note::rating, Collectors.toList())));
		Map<Long, Double> score = new HashMap<>();
		world.beans.forEach(b -> {
			List<Double> r = byBean.getOrDefault(b.id(), List.of());
			score.put(b.id(), (r.stream().mapToDouble(Double::doubleValue).sum() + 3 * globalMean) / (r.size() + 3));
		});
		return evaluateGlobalRanking(Comparator.comparingDouble((Bean b) -> score.get(b.id())).reversed()
				.thenComparingLong(Bean::id));
	}

	/** 기준선 ③: 무작위 (사용자마다 다른 고정 시드) */
	public Metrics evaluateRandom(long seed) {
		return evaluate(userId -> {
			Set<Long> seen = train.get(userId).stream().map(Note::beanId).collect(Collectors.toSet());
			List<Long> ids = new ArrayList<>(world.beans.stream().map(Bean::id).filter(id -> !seen.contains(id)).toList());
			Collections.shuffle(ids, new Random(seed * 31 + userId));
			return ids.subList(0, K);
		});
	}

	private Metrics evaluateGlobalRanking(Comparator<Bean> order) {
		List<Long> ranked = world.beans.stream().sorted(order).map(Bean::id).toList();
		return evaluate(userId -> {
			Set<Long> seen = train.get(userId).stream().map(Note::beanId).collect(Collectors.toSet());
			return ranked.stream().filter(id -> !seen.contains(id)).limit(K).toList();
		});
	}

	// ---- 지표 ----

	private Metrics evaluate(java.util.function.Function<Long, List<Long>> recommender) {
		double p5 = 0;
		double r10 = 0;
		double ndcg = 0;
		int users = 0;
		for (Long userId : test.keySet()) {
			Set<Long> relevant = test.get(userId).stream()
					.filter(n -> n.rating() >= RELEVANT_RATING)
					.map(Note::beanId)
					.collect(Collectors.toSet());
			if (relevant.isEmpty() || !userTaste.containsKey(userId)) {
				continue;
			}
			List<Long> recs = recommender.apply(userId);
			p5 += hits(recs, relevant, 5) / 5.0;
			r10 += hits(recs, relevant, 10) / (double) relevant.size();
			ndcg += ndcg(recs, relevant, 10);
			users++;
		}
		return new Metrics(p5 / users, r10 / users, ndcg / users, users);
	}

	private static int hits(List<Long> recs, Set<Long> relevant, int k) {
		return (int) recs.stream().limit(k).filter(relevant::contains).count();
	}

	private static double ndcg(List<Long> recs, Set<Long> relevant, int k) {
		double dcg = 0;
		for (int i = 0; i < Math.min(k, recs.size()); i++) {
			if (relevant.contains(recs.get(i))) {
				dcg += 1 / log2(i + 2);
			}
		}
		double ideal = 0;
		for (int i = 0; i < Math.min(k, relevant.size()); i++) {
			ideal += 1 / log2(i + 2);
		}
		return dcg / ideal;
	}

	private static double log2(double x) {
		return Math.log(x) / Math.log(2);
	}

	// ---- 데이터 요약 (리포트용) ----

	int userCount() {
		return world.users.size();
	}

	int noteCount() {
		return world.notes.size();
	}

	int heldOutNotes() {
		return test.values().stream().mapToInt(List::size).sum();
	}

	double relevantShare() {
		long relevant = test.values().stream().flatMap(List::stream).filter(n -> n.rating() >= RELEVANT_RATING).count();
		return relevant / (double) heldOutNotes();
	}

}
