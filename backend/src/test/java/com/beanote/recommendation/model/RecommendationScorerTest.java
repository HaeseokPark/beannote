package com.beanote.recommendation.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.beanote.bean.domain.ProcessMethod;
import com.beanote.bean.domain.RoastLevel;
import com.beanote.global.util.Josa;
import com.beanote.taste.model.SectionWeights;
import com.beanote.taste.model.VectorLayout;

/**
 * 추천 점수·이유 규칙 고정 테스트 (5-3, 5-4, 9-3).
 */
class RecommendationScorerTest {

	private static final ScoreWeights WEIGHTS = new ScoreWeights(0.65, 0.25, 0.10, 0.3);
	private static final SectionWeights SECTIONS = new SectionWeights(1.0, 0.6, 0.4, 0.8);
	private final RecommendationScorer scorer = new RecommendationScorer(WEIGHTS, SECTIONS);
	private final ReasonGenerator reasons = new ReasonGenerator(
			Map.of("BERRY", "베리", "FERMENTED", "발효·와인", "FLORAL", "꽃"), WEIGHTS);

	// 취향: 밝은 산미, 베리, 라이트 로스팅
	private static final double[] TASTE = vector(1.0, Map.of("BERRY", 1.0), -1.0, null);

	@Test
	void 싫어하는_향은_감점되지만_추천_후보에서_빠지지는_않는다() {
		Candidate berryAndWine = candidate(1, vector(1.0, Map.of("BERRY", 0.5, "FERMENTED", 0.5), -1.0,
				ProcessMethod.NATURAL));
		UserContext likesAll = user(Set.of());
		UserContext dislikesWine = user(Set.of("FERMENTED"));

		ScoredCandidate normal = scorer.score(berryAndWine, likesAll);
		ScoredCandidate penalized = scorer.score(berryAndWine, dislikesWine);

		assertThat(penalized.dislikePenalty()).isEqualTo(0.5 * 0.3);
		assertThat(penalized.score()).isEqualTo(normal.score() - 0.15);
		assertThat(scorer.rank(List.of(berryAndWine), dislikesWine, 10))
				.extracting(s -> s.candidate().beanId()).containsExactly(1L);
	}

	@Test
	void 취향과_가까운_원두가_먼저_온다() {
		Candidate close = candidate(1, vector(0.8, Map.of("BERRY", 1.0), -1.0, ProcessMethod.WASHED));
		Candidate far = candidate(2, vector(-1.0, Map.of("FERMENTED", 1.0), 1.0, ProcessMethod.WASHED));

		assertThat(scorer.rank(List.of(far, close), user(Set.of()), 10))
				.extracting(s -> s.candidate().beanId()).containsExactly(1L, 2L);
	}

	@Test
	void 점수_항목은_모두_0에서_1_사이_단위로_더해진다() {
		ScoredCandidate s = scorer.score(candidate(1, TASTE.clone(), ProcessMethod.WASHED), user(Set.of()));
		assertThat(s.contentSim()).isBetween(0.0, 1.0).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-9));
		assertThat(s.explore()).isBetween(0.0, 1.0);
		// 기여도의 합 = α × cos / 2
		double sum = java.util.Arrays.stream(s.contributions()).sum();
		assertThat(sum).isCloseTo(WEIGHTS.content() / 2, org.assertj.core.data.Offset.offset(1e-9));
	}

	@Test
	void 마셔본_기록이_있을_때만_탐험_가점을_준다() {
		Candidate anaerobicKenya = new Candidate(1, TASTE.clone(), Set.of("KE"), "KE", ProcessMethod.ANAEROBIC,
				RoastLevel.LIGHT);

		ScoredCandidate noHistory = scorer.score(anaerobicKenya, user(Set.of()));
		ScoredCandidate withHistory = scorer.score(anaerobicKenya, new UserContext(TASTE, Set.of("ET"),
				Set.of(ProcessMethod.WASHED), Set.of(), Map.of(), false));

		assertThat(noHistory.explore()).isZero();
		assertThat(withHistory.explore()).isEqualTo(1.0);
		assertThat(withHistory.newOrigin()).isTrue();
		assertThat(withHistory.newProcess()).isTrue();
	}

	@Test
	void 추천_이유는_기여도가_큰_두_가지를_서로_다른_유형으로_고른다() {
		Candidate bean = candidate(1, vector(1.0, Map.of("BERRY", 1.0), -1.0, ProcessMethod.WASHED));
		ScoredCandidate scored = scorer.score(bean, user(Set.of()));

		List<Reason> why = reasons.generate(scored, user(Set.of()));

		assertThat(why).hasSize(2);
		assertThat(why).extracting(Reason::type).doesNotHaveDuplicates();
		// 향미 근거가 있으면 둘 중 하나는 향미 이유다
		assertThat(why).extracting(Reason::type).contains(Reason.Type.FLAVOR);
		assertThat(why).extracting(Reason::text).containsAnyOf(
				"좋아하시는 밝은 산미가 특징이에요",
				"높게 평가하신 커피들처럼 베리 향이 있어요",
				"선호하시는 라이트 로스팅이에요");
	}

	@Test
	void 설문_기반이면_설문_문구를_쓴다() {
		Candidate bean = candidate(1, vector(0, Map.of("BERRY", 1.0), 0, ProcessMethod.WASHED));
		double[] berryOnly = vector(0, Map.of("BERRY", 1.0), 0, null);
		UserContext survey = new UserContext(berryOnly, Set.of(), Set.of(), Set.of(), Map.of(), true);

		assertThat(reasons.generate(scorer.score(bean, survey), survey))
				.extracting(Reason::text).contains("좋아한다고 고르신 베리 향이 있어요");
	}

	@Test
	void 탐험_이유_문장() {
		Candidate bean = new Candidate(1, TASTE.clone(), Set.of("KE"), "KE", ProcessMethod.ANAEROBIC,
				RoastLevel.LIGHT);
		// 탐험 가점이 크게 보이도록 취향 벡터와 거의 무관한 상황을 만든다
		UserContext ctx = new UserContext(vector(0, Map.of("FLORAL", 1.0), 0, null), Set.of("ET"),
				Set.of(ProcessMethod.WASHED), Set.of(), Map.of(), false);

		assertThat(reasons.generate(scorer.score(bean, ctx), ctx)).extracting(Reason::text)
				.contains("아직 안 마셔 본 무산소 가공이에요");
	}

	@Test
	void 조사는_받침에_따라_바뀐다() {
		assertThat(Josa.iGa("밝은 산미")).isEqualTo("밝은 산미가");
		assertThat(Josa.iGa("묵직한 바디감")).isEqualTo("묵직한 바디감이");
		assertThat(Josa.ieyo("케냐 원두")).isEqualTo("케냐 원두예요");
		assertThat(Josa.ieyo("무산소 가공")).isEqualTo("무산소 가공이에요");
		assertThat(Josa.ieyo("SL28")).isEqualTo("SL28예요");
	}

	private static UserContext user(Set<String> disliked) {
		return new UserContext(TASTE, Set.of(), Set.of(), disliked, Map.of(), false);
	}

	private static Candidate candidate(long id, double[] vector, ProcessMethod process) {
		return new Candidate(id, vector, Set.of("ET"), "ET", process, RoastLevel.LIGHT);
	}

	private static Candidate candidate(long id, double[] vector) {
		return candidate(id, vector, ProcessMethod.WASHED);
	}

	private static double[] vector(double acidity, Map<String, Double> flavors, double roast, ProcessMethod process) {
		double[] v = new double[VectorLayout.DIMENSIONS];
		v[VectorLayout.TASTE_START] = acidity;
		flavors.forEach((code, value) -> v[VectorLayout.categoryIndex(code)] = value);
		if (process != null) {
			v[VectorLayout.processIndex(process)] = 1.0;
		}
		v[VectorLayout.ROAST_INDEX] = roast;
		return v;
	}

}
