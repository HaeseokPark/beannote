package com.beanote.recommendation.eval;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import com.beanote.recommendation.eval.OfflineEvaluator.Metrics;
import com.beanote.recommendation.model.ScoreWeights;
import com.beanote.taste.config.RecommendationProperties;
import com.beanote.taste.model.BeanVectorCalculator;
import com.beanote.taste.model.TasteEstimateTable;
import com.beanote.taste.model.TasteVectorCalculator;

/**
 * 추천 오프라인 평가 (Phase 7 완료 조건). 가상 데이터로 기준선 3개와 추천 변형 4개를 비교하고
 * reports/recommendation-eval.md 를 다시 만든다. Spring·DB 없이 몇 초 안에 끝나서 일반 테스트에 포함한다.
 */
class RecommendationEvaluationTest {

	private static final long[] SEEDS = { 11, 23, 37 };
	private static final int BEANS = 300;
	private static final int[] USERS_PER_TYPE = { 40, 60 };
	private static final int[] NOTES_PER_USER = { 10, 30 };
	private static final Path REPORT = Path.of("..", "reports", "recommendation-eval.md");

	@Test
	void hybridRecommenderBeatsBaselinesOnSyntheticData() throws IOException {
		RecommendationProperties props = loadProperties();
		TasteEstimateTable estimates = new TasteEstimateTable(props.roastTaste(), props.processTasteAdjust());
		BeanVectorCalculator beanVectors = new BeanVectorCalculator(estimates, props.priorK());
		TasteVectorCalculator tasteVectors = new TasteVectorCalculator(props.decayPerWeek(), props.surveyMinNotes(),
				props.stableNotes(), props.sectionWeights());

		ScoreWeights configured = props.scoreWeights();
		Map<String, Function<OfflineEvaluator, Metrics>> methods = new LinkedHashMap<>();
		methods.put("무작위", e -> e.evaluateRandom(7));
		methods.put("인기순 (노트 수)", OfflineEvaluator::evaluatePopularity);
		methods.put("평균 평점순", OfflineEvaluator::evaluateAverageRating);
		methods.put("콘텐츠 기반 단독", e -> e.evaluateScorer(new ScoreWeights(1, 0, 0, configured.dislikePenalty())));
		methods.put("협업 필터링 단독", e -> e.evaluateScorer(new ScoreWeights(0, 1, 0, configured.dislikePenalty())));
		methods.put("혼합 (콘텐츠 + 협업)", e -> e.evaluateScorer(
				new ScoreWeights(configured.content(), configured.collab(), 0, configured.dislikePenalty())));
		methods.put("최종 (혼합 + 탐험)", e -> e.evaluateScorer(configured));

		Map<String, List<Metrics>> results = new LinkedHashMap<>();
		methods.keySet().forEach(name -> results.put(name, new ArrayList<>()));
		List<OfflineEvaluator> evaluators = new ArrayList<>();
		for (long seed : SEEDS) {
			SyntheticWorld world = SyntheticWorld.generate(seed, estimates, BEANS, USERS_PER_TYPE, NOTES_PER_USER);
			OfflineEvaluator evaluator = new OfflineEvaluator(world, props, beanVectors, tasteVectors);
			evaluators.add(evaluator);
			methods.forEach((name, method) -> results.get(name).add(method.apply(evaluator)));
		}

		// 분석 ①: 협업 가중치 β 를 바꿔 가며 (α = 1 − β, 탐험 없음)
		Map<Double, List<Metrics>> betaSweep = new LinkedHashMap<>();
		for (double beta : new double[] { 0, 0.25, 0.5, 0.75, 1.0 }) {
			ScoreWeights w = new ScoreWeights(1 - beta, beta, 0, configured.dislikePenalty());
			betaSweep.put(beta, evaluators.stream().map(e -> e.evaluateScorer(w)).toList());
		}

		// 분석 ②: 숨은 원산지 요인을 없앤 세계에서 같은 비교
		Map<String, List<Metrics>> noHidden = new LinkedHashMap<>();
		List.of("인기순 (노트 수)", "콘텐츠 기반 단독", "협업 필터링 단독", "혼합 (콘텐츠 + 협업)")
				.forEach(name -> noHidden.put(name, new ArrayList<>()));
		for (long seed : SEEDS) {
			OfflineEvaluator evaluator = new OfflineEvaluator(
					SyntheticWorld.generate(seed, estimates, BEANS, USERS_PER_TYPE, NOTES_PER_USER, 0.0),
					props, beanVectors, tasteVectors);
			noHidden.forEach((name, list) -> list.add(methods.get(name).apply(evaluator)));
		}

		writeReport(results, evaluators.getFirst(), configured);
		appendAnalysis(betaSweep, noHidden, configured);

		double hybrid = mean(results.get("혼합 (콘텐츠 + 협업)"), Metrics::ndcgAt10);
		double popularity = mean(results.get("인기순 (노트 수)"), Metrics::ndcgAt10);
		double random = mean(results.get("무작위"), Metrics::ndcgAt10);
		assertThat(hybrid).as("혼합 NDCG@10 > 인기순").isGreaterThan(popularity);
		assertThat(hybrid).as("혼합 NDCG@10 > 무작위").isGreaterThan(random);
	}

	private void writeReport(Map<String, List<Metrics>> results, OfflineEvaluator sample, ScoreWeights weights)
			throws IOException {
		double popNdcg = mean(results.get("인기순 (노트 수)"), Metrics::ndcgAt10);
		double popP5 = mean(results.get("인기순 (노트 수)"), Metrics::precisionAt5);

		StringBuilder md = new StringBuilder();
		md.append("# 추천 오프라인 평가\n\n")
				.append("> `RecommendationEvaluationTest` 가 테스트 실행 때마다 다시 만든다 (`./gradlew test`).\n")
				.append("> **가상 사용자 데이터로 측정한 결과이며 실제 사용자 만족도가 아니다.**\n\n");

		md.append("## 결과 (시드 ").append(SEEDS.length).append("개 평균 ± 표준편차)\n\n")
				.append("| 방법 | Precision@5 | Recall@10 | NDCG@10 | 인기순 대비 NDCG |\n|---|---|---|---|---|\n");
		results.forEach((name, list) -> {
			double ndcg = mean(list, Metrics::ndcgAt10);
			boolean highlight = name.startsWith("혼합") || name.startsWith("최종");
			md.append("| ").append(highlight ? "**" + name + "**" : name).append(" | ")
					.append(fmt(list, Metrics::precisionAt5)).append(" | ")
					.append(fmt(list, Metrics::recallAt10)).append(" | ")
					.append(highlight ? "**" + fmt(list, Metrics::ndcgAt10) + "**" : fmt(list, Metrics::ndcgAt10))
					.append(" | ").append(name.startsWith("인기순") ? "기준" : lift(ndcg, popNdcg)).append(" |\n");
		});
		double hybridP5 = mean(results.get("혼합 (콘텐츠 + 협업)"), Metrics::precisionAt5);
		md.append("\n혼합 추천의 Precision@5 는 인기순 대비 ").append(lift(hybridP5, popP5)).append(".\n\n");

		md.append("## 평가 방법 (기획서 5-5)\n\n")
				.append("1. 노트 10개 이상인 사용자마다 **최근** 노트의 20%를 숨긴다 (시간 순서를 지킨 분할).\n")
				.append("2. 원두 벡터·취향 벡터·협업 이웃은 남은 학습용 노트만으로 계산한다. 숨긴 노트는 어디에도 쓰지 않는다.\n")
				.append("3. 학습 노트를 쓴 원두를 뺀 원두 전체에서 상위 10개를 추천한다.\n")
				.append("4. 숨긴 노트 중 별점 4.0 이상 원두를 정답으로 보고 Precision@5, Recall@10, NDCG@10 을 사용자별로 재서 평균 낸다.\n")
				.append("   정답이 하나도 없는 사용자는 뺀다.\n\n")
				.append("운영 코드와 같은 순수 계산 클래스를 쓴다: `BeanVectorCalculator`, `TasteVectorCalculator`, `NeighborFinder`, ")
				.append("`CollabScorer`, `RecommendationScorer`. 가중치는 `application.yml` 값 그대로다 ")
				.append(String.format(Locale.ROOT, "(α=%.2f, β=%.2f, γ=%.2f).\n\n", weights.content(), weights.collab(),
						weights.explore()));

		md.append("## 가상 데이터 (기획서 13-1)\n\n")
				.append("| 항목 | 값 (시드 1개 기준) |\n|---|---|\n")
				.append("| 원두 | ").append(BEANS).append("개 (원산지 10, 가공 4, 로스팅 5, 공식 향미 3개) |\n")
				.append("| 사용자 | ").append(sample.userCount()).append("명 (취향 유형 6가지 × 40~60명) |\n")
				.append("| 노트 | ").append(sample.noteCount()).append("개 (사용자당 10~30개) |\n")
				.append("| 숨긴 노트 | ").append(sample.heldOutNotes()).append("개, 그중 정답(4.0 이상) ")
				.append(String.format(Locale.ROOT, "%.0f%%", sample.relevantShare() * 100)).append(" |\n\n")
				.append("취향 유형: ")
				.append(String.join(", ", java.util.Arrays.stream(SyntheticWorld.TasteType.values()).map(t -> t.label).toList()))
				.append(".\n\n")
				.append("평점이 원두 벡터에서만 나오면 콘텐츠 기반 추천이 당연히 이기는 순환 평가가 되므로, 추천기가 볼 수 없는 요인을 섞었다.\n\n")
				.append("- **숨은 요인**: 유형마다 선호 원산지가 있다. 원산지는 원두 벡터에 없어서 콘텐츠 기반은 알 수 없고, ")
				.append("같은 유형의 이웃이 남긴 평점(협업)으로만 드러난다.\n")
				.append("- **개인차**: 사람마다 향미 선호가 유형 평균과 ±0.35 정도 다르다.\n")
				.append("- **평점 습관**: 기준점(−0.6~+0.6)과 폭(0.6~1.3배)이 사람마다 다르다.\n")
				.append("- **잡음**: 같은 원두도 평점이 흔들린다 (표준편차 0.35).\n")
				.append("- **선택 편향**: 60%는 좋아할 만한 원두를, 40%는 아무 원두나 고른다.\n\n");

		md.append("## 읽는 법과 한계\n\n")
				.append("- 가상 데이터의 구조(유형·숨은 요인)를 만든 사람과 추천기를 만든 사람이 같으므로 결과는 낙관적일 수 있다. ")
				.append("실제 사용자 데이터가 쌓이면 같은 방법으로 다시 잰다 (14장 Q6).\n")
				.append("- 협업 필터링 단독은 이웃이 평가하지 않은 원두가 모두 같은 점수(중립 0.5)라 순위가 거칠다.\n")
				.append("- 탐험 가점(γ)은 정확도 지표를 조금 낮출 수 있다. 처음 보는 원산지·가공을 섞는 것은 정확도가 아니라 발견을 위한 장치다.\n")
				.append("- 무작위 기준선의 기대 Precision@5 ≈ 정답 수 / 후보 수 이므로 매우 낮다.\n");

		Files.createDirectories(REPORT.getParent());
		Files.writeString(REPORT, md.toString(), StandardCharsets.UTF_8);
	}

	private void appendAnalysis(Map<Double, List<Metrics>> betaSweep, Map<String, List<Metrics>> noHidden,
			ScoreWeights configured) throws IOException {
		StringBuilder md = new StringBuilder("\n## 분석 ① 협업 가중치 β\n\n")
				.append("α = 1 − β, 탐험 없음. 운영 설정은 β = ")
				.append(String.format(Locale.ROOT, "%.2f", configured.collab())).append(" 이다.\n\n")
				.append("| β | Precision@5 | NDCG@10 |\n|---|---|---|\n");
		betaSweep.forEach((beta, list) -> md.append(String.format(Locale.ROOT, "| %.2f | ", beta))
				.append(fmt(list, Metrics::precisionAt5)).append(" | ").append(fmt(list, Metrics::ndcgAt10)).append(" |\n"));

		md.append("\n## 분석 ② 숨은 요인을 없애면\n\n")
				.append("평점이 원두 벡터로 설명되는 요인(향미·로스팅·가공)만으로 정해지는 세계에서 같은 비교를 했다.\n\n")
				.append("| 방법 | Precision@5 | NDCG@10 |\n|---|---|---|\n");
		noHidden.forEach((name, list) -> md.append("| ").append(name).append(" | ")
				.append(fmt(list, Metrics::precisionAt5)).append(" | ").append(fmt(list, Metrics::ndcgAt10)).append(" |\n"));

		double contentHidden = mean(noHidden.get("콘텐츠 기반 단독"), Metrics::ndcgAt10);
		double collabHidden = mean(noHidden.get("협업 필터링 단독"), Metrics::ndcgAt10);
		md.append("\n### 해석\n\n")
				.append("- 숨은 요인이 있는 기본 세계에서는 협업 필터링이 가장 강하다. 원산지 선호는 원두 벡터에 없어서 ")
				.append("같은 유형 이웃의 평점으로만 드러나기 때문이다.\n")
				.append("- 숨은 요인을 없애면 콘텐츠 기반 NDCG@10 은 ")
				.append(String.format(Locale.ROOT, "%.3f", contentHidden)).append(", 협업 단독은 ")
				.append(String.format(Locale.ROOT, "%.3f", collabHidden)).append(" 이다. ")
				.append(contentHidden > collabHidden
						? "콘텐츠 기반이 앞서므로, 어느 쪽이 유리한지는 데이터에 벡터 밖 요인이 얼마나 있는지에 달려 있다.\n"
						: "이 경우에도 협업이 앞선다. 가상 사용자에게는 \"미디엄 로스팅을 좋아함\"처럼 **가운데 값을 좋아하는 선호**가 있는데, "
								+ "코사인 유사도는 밝음↔어두움처럼 한 방향의 선호만 표현해서 이를 담지 못한다. 이웃의 평점은 이런 선호도 그대로 반영한다. "
								+ "콘텐츠 기반을 개선하려면 로스팅을 순서형 1차원 대신 단계별 원-핫으로 나누는 방법을 검토할 수 있다.\n")
				.append("- 두 방식을 섞으면 어느 한쪽이 약한 상황에서도 크게 무너지지 않는다. 가상 데이터의 숨은 요인 세기는 ")
				.append("임의로 정한 값이므로, β 는 실제 사용자 데이터로 다시 평가한 뒤 조정한다 (지금은 기획서 기본값 유지).\n")
				.append("- 협업 점수는 사용자 50명 미만이면 쓰지 않는다. 서비스 초반에는 콘텐츠 기반이 추천을 맡는다.\n");
		Files.writeString(REPORT, md.toString(), StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND);
	}

	private static RecommendationProperties loadProperties() throws IOException {
		List<PropertySource<?>> sources = new YamlPropertySourceLoader().load("application",
				new ClassPathResource("application.yml"));
		return new Binder(ConfigurationPropertySources.from(sources))
				.bind("beanote.recommendation", RecommendationProperties.class)
				.get();
	}

	private static double mean(List<Metrics> list, Function<Metrics, Double> f) {
		return list.stream().mapToDouble(f::apply).average().orElse(0);
	}

	private static String fmt(List<Metrics> list, Function<Metrics, Double> f) {
		double mean = mean(list, f);
		double sd = Math.sqrt(list.stream().mapToDouble(m -> Math.pow(f.apply(m) - mean, 2)).average().orElse(0));
		return String.format(Locale.ROOT, "%.3f ± %.3f", mean, sd);
	}

	private static String lift(double value, double base) {
		return String.format(Locale.ROOT, "%+.0f%%", (value / base - 1) * 100);
	}

}
