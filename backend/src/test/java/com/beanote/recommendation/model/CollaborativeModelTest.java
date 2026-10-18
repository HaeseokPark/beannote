package com.beanote.recommendation.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.beanote.taste.model.SectionWeights;
import com.beanote.taste.model.VectorLayout;

/**
 * 협업 필터링 순수 계산 (5-3): 이웃 찾기와 협업 점수.
 */
class CollaborativeModelTest {

	private static final SectionWeights WEIGHTS = new SectionWeights(1.0, 0.6, 0.4, 0.8);

	@Test
	void 취향이_가까운_사용자가_이웃이_되고_반대_취향은_이웃이_아니다() {
		double[] bright = axis(1.0);
		double[] alsoBright = axis(0.8);
		double[] dark = axis(-1.0);
		Map<Long, List<Neighbor>> neighbors = new NeighborFinder(20, WEIGHTS)
				.findAll(Map.of(1L, bright, 2L, alsoBright, 3L, dark));

		assertThat(neighbors.get(1L)).extracting(Neighbor::userId).containsExactly(2L);
		assertThat(neighbors.get(3L)).isEmpty();
	}

	@Test
	void 이웃은_유사도_순으로_최대_k명() {
		Map<Long, double[]> vectors = Map.of(1L, axis(1.0), 2L, axis(0.9), 3L, mixed(0.9, 0.5), 4L, mixed(0.9, 1.0));
		List<Neighbor> top2 = new NeighborFinder(2, WEIGHTS).findAll(vectors).get(1L);

		assertThat(top2).hasSize(2);
		assertThat(top2).extracting(Neighbor::userId).containsExactly(2L, 3L);
	}

	@Test
	void 협업_점수는_이웃의_평균_대비_평점으로_계산한다() {
		// 이웃 A 는 후하게(평균 4.5), 이웃 B 는 짜게(평균 2.5) 주지만 둘 다 원두 10 을 자기 평균보다 1점 높게 줬다
		Map<Long, Map<Long, Double>> ratings = Map.of(
				1L, Map.of(10L, 5.5 - 0.5, 11L, 4.0, 12L, 4.5),
				2L, Map.of(10L, 3.5, 11L, 2.0, 12L, 2.0));
		List<Neighbor> neighbors = List.of(new Neighbor(1L, 0.9), new Neighbor(2L, 0.9));

		Map<Long, CollabSignal> signals = CollabScorer.score(neighbors, ratings);

		CollabSignal liked = signals.get(10L);
		// dev = +0.5 (A: 5.0−4.5) 과 +1.0 (B: 3.5−2.5) 의 평균 0.75, 두 명이므로 × 2/3 → 0.5, score = 0.5 + 0.5/4
		assertThat(liked.score()).isCloseTo(0.625, within(1e-9));
		assertThat(liked.raters()).isEqualTo(2);
		assertThat(signals.get(12L).score()).isLessThan(CollabSignal.NEUTRAL);
	}

	@Test
	void 한_명의_평가는_덜_믿는다() {
		Map<Long, Map<Long, Double>> ratings = Map.of(1L, Map.of(10L, 5.0, 11L, 3.0));
		Map<Long, CollabSignal> one = CollabScorer.score(List.of(new Neighbor(1L, 1.0)), ratings);
		// dev = +1.0, n=1 → × 1/2 = 0.5 → 0.625
		assertThat(one.get(10L).score()).isCloseTo(0.625, within(1e-9));
	}

	@Test
	void 협업을_쓰지_않으면_0이고_쓰면_평가_없는_원두는_중립() {
		RecommendationScorer scorer = new RecommendationScorer(new ScoreWeights(0.65, 0.25, 0.1, 0.3), WEIGHTS);
		Candidate bean = new Candidate(99, axis(1.0), java.util.Set.of("ET"), "ET",
				com.beanote.bean.domain.ProcessMethod.WASHED, com.beanote.bean.domain.RoastLevel.LIGHT);

		ScoredCandidate off = scorer.score(bean, UserContext.withoutCollab(axis(1.0), java.util.Set.of(),
				java.util.Set.of(), java.util.Set.of(), false));
		ScoredCandidate on = scorer.score(bean, new UserContext(axis(1.0), java.util.Set.of(), java.util.Set.of(),
				java.util.Set.of(), Map.of(), true, false));

		assertThat(off.collab()).isZero();
		assertThat(on.collab()).isEqualTo(CollabSignal.NEUTRAL);
	}

	private static double[] axis(double acidity) {
		double[] v = new double[VectorLayout.DIMENSIONS];
		v[VectorLayout.TASTE_START] = acidity;
		v[VectorLayout.ROAST_INDEX] = -acidity;
		return v;
	}

	private static double[] mixed(double acidity, double floral) {
		double[] v = axis(acidity);
		v[VectorLayout.categoryIndex("FLORAL")] = floral;
		return v;
	}

}
