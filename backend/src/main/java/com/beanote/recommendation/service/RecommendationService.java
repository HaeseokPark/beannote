package com.beanote.recommendation.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beanote.bean.dto.BeanDtos.BeanSummaryResponse;
import com.beanote.bean.service.BeanService;
import com.beanote.flavor.domain.FlavorCategory;
import com.beanote.flavor.repository.FlavorCategoryRepository;
import com.beanote.global.error.BusinessException;
import com.beanote.global.error.ErrorCode;
import com.beanote.recommendation.dto.RecommendationResponse;
import com.beanote.recommendation.dto.RecommendationResponse.Item;
import com.beanote.recommendation.dto.RecommendationResponse.SimilarBean;
import com.beanote.recommendation.model.Candidate;
import com.beanote.recommendation.model.Reason;
import com.beanote.recommendation.model.ReasonGenerator;
import com.beanote.recommendation.model.RecommendationScorer;
import com.beanote.recommendation.model.ScoredCandidate;
import com.beanote.recommendation.model.UserContext;
import com.beanote.recommendation.repository.RecommendationQueries;
import com.beanote.recommendation.repository.RecommendationQueries.TriedAttributes;
import com.beanote.taste.config.RecommendationProperties;
import com.beanote.taste.model.Similarity;
import com.beanote.taste.model.TasteVectorCalculator.TasteResult;
import com.beanote.taste.service.BeanVectorService;
import com.beanote.taste.service.TasteProfileService;

/**
 * 원두 추천 (4-7 R-1 ~ R-4, 5장).
 */
@Service
@Transactional
public class RecommendationService {

	private final RecommendationQueries queries;
	private final TasteProfileService tasteProfileService;
	private final BeanVectorService beanVectorService;
	private final BeanService beanService;
	private final FlavorCategoryRepository categoryRepository;
	private final RecommendationCache cache;
	private final RecommendationProperties properties;
	private final RecommendationScorer scorer;

	public RecommendationService(RecommendationQueries queries, TasteProfileService tasteProfileService,
			BeanVectorService beanVectorService, BeanService beanService, FlavorCategoryRepository categoryRepository,
			RecommendationCache cache, RecommendationProperties properties) {
		this.queries = queries;
		this.tasteProfileService = tasteProfileService;
		this.beanVectorService = beanVectorService;
		this.beanService = beanService;
		this.categoryRepository = categoryRepository;
		this.cache = cache;
		this.properties = properties;
		this.scorer = new RecommendationScorer(properties.scoreWeights(), properties.sectionWeights());
	}

	/** 추천 목록. 사용자별로 캐시한다. */
	public RecommendationResponse recommend(Long userId) {
		return cache.get(userId, this::compute);
	}

	/**
	 * 캐시를 거치지 않고 계산한다.
	 * 제외: 이미 노트를 쓴 원두, 피드백을 준 원두, 판매 종료·승인 대기·병합된 원두.
	 */
	public RecommendationResponse compute(Long userId) {
		TasteResult taste = tasteProfileService.current(userId);
		Set<Long> excluded = queries.excludedBeanIds(userId);

		if (taste.vector() == null) {
			return popular(taste, excluded);
		}

		TriedAttributes tried = queries.triedAttributes(userId);
		// 협업 점수는 Phase 7 에서 이웃 배치와 함께 채운다. 사용자가 collabMinUsers 명 미만이면 쓰지 않는다
		UserContext context = new UserContext(taste.vector(), tried.origins(), tried.processes(),
				queries.dislikedCategories(userId), Map.of(), taste.notesWeight() < 0.5);

		List<Candidate> candidates = queries.loadCandidates().stream()
				.filter(c -> !excluded.contains(c.beanId()))
				.toList();
		List<ScoredCandidate> ranked = scorer.rank(candidates, context, properties.size());

		ReasonGenerator reasons = new ReasonGenerator(categoryNames(), properties.scoreWeights());
		Map<Long, BeanSummaryResponse> beans = summariesById(ranked.stream().map(s -> s.candidate().beanId()).toList());
		List<Item> items = ranked.stream()
				.filter(s -> beans.containsKey(s.candidate().beanId()))
				.map(s -> {
					List<Reason> why = reasons.generate(s, context);
					boolean exploration = why.stream().anyMatch(r -> r.type() == Reason.Type.EXPLORE);
					return new Item(beans.get(s.candidate().beanId()), round(s.score()), why, exploration);
				})
				.toList();
		return new RecommendationResponse(taste.confidence(), taste.noteCount(), false, items);
	}

	/** 비슷한 원두 (R-4). 원두 벡터끼리 가중 코사인이 높은 순서 (벡터가 없으면 계산·저장하므로 쓰기 트랜잭션) */
	public List<SimilarBean> similar(Long beanId) {
		if (!beanService.isListed(beanId)) {
			throw new BusinessException(ErrorCode.BEAN_NOT_FOUND);
		}
		double[] target = beanVectorService.loadVectors(List.of(beanId)).get(beanId);
		List<Candidate> candidates = queries.loadCandidates();
		List<Map.Entry<Long, Double>> top = candidates.stream()
				.filter(c -> c.beanId() != beanId)
				.map(c -> Map.entry(c.beanId(), Similarity.cosine(target, c.vector(), properties.sectionWeights())))
				.sorted(Map.Entry.<Long, Double>comparingByValue(Comparator.reverseOrder())
						.thenComparing(Map.Entry.comparingByKey()))
				.limit(properties.similarSize())
				.toList();
		Map<Long, BeanSummaryResponse> beans = summariesById(top.stream().map(Map.Entry::getKey).toList());
		return top.stream()
				.filter(e -> beans.containsKey(e.getKey()))
				.map(e -> new SimilarBean(beans.get(e.getKey()), round(e.getValue())))
				.toList();
	}

	private RecommendationResponse popular(TasteResult taste, Set<Long> excluded) {
		List<BeanSummaryResponse> beans = beanService.summaries(queries.popularBeanIds(excluded, properties.size()));
		List<Item> items = beans.stream()
				.map(bean -> new Item(bean, null, List.of(bean.noteCount() > 0
						? new Reason(Reason.Type.POPULAR, "많은 분들이 기록한 원두예요")
						: new Reason(Reason.Type.POPULAR, "취향 설문을 하거나 노트를 쓰면 더 잘 맞는 원두를 찾아 드려요")),
						false))
				.toList();
		return new RecommendationResponse(taste.confidence(), taste.noteCount(), true, items);
	}

	private Map<String, String> categoryNames() {
		return categoryRepository.findAll().stream()
				.collect(Collectors.toMap(FlavorCategory::getCode, FlavorCategory::getNameKo));
	}

	private Map<Long, BeanSummaryResponse> summariesById(List<Long> ids) {
		return beanService.summaries(ids).stream()
				.collect(Collectors.toMap(BeanSummaryResponse::id, Function.identity()));
	}

	private static double round(double value) {
		return Math.round(value * 1000) / 1000.0;
	}

}
