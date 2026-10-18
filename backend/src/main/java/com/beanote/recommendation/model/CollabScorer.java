package com.beanote.recommendation.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 협업 점수 (5-3 collabScore). 이웃이 이 원두에 준 "보정 평점"의 유사도 가중 평균.
 *
 * <pre>
 * dev      = Σ sim × (평점 − 그 이웃의 평균 평점) / Σ sim
 * shrunk   = dev × n / (n + 1)            n = 평가한 이웃 수 (한두 명의 평가는 덜 믿는다)
 * score    = clamp(0.5 + shrunk / 4, 0, 1) → 0 ~ 1 (±2점 차이가 양 끝)
 * </pre>
 *
 * 평점을 그 사람의 평균에서 빼서, 후하게 주는 이웃과 짜게 주는 이웃을 같은 기준으로 본다 (취향 벡터와 같은 보정).
 */
public final class CollabScorer {

	private static final double DEV_RANGE = 4.0;

	private CollabScorer() {
	}

	/**
	 * @param neighbors    대상 사용자의 이웃
	 * @param ratingsByUser 이웃별 원두 → 별점 (이웃의 평균을 내기 위해 그 사람의 노트 전체)
	 */
	public static Map<Long, CollabSignal> score(List<Neighbor> neighbors, Map<Long, Map<Long, Double>> ratingsByUser) {
		Map<Long, double[]> sums = new HashMap<>(); // beanId → [Σ sim·dev, Σ sim·rating, Σ sim, n]
		for (Neighbor neighbor : neighbors) {
			Map<Long, Double> ratings = ratingsByUser.getOrDefault(neighbor.userId(), Map.of());
			if (ratings.isEmpty()) {
				continue;
			}
			double mean = ratings.values().stream().mapToDouble(Double::doubleValue).average().orElse(0);
			ratings.forEach((beanId, rating) -> {
				double[] s = sums.computeIfAbsent(beanId, k -> new double[4]);
				s[0] += neighbor.similarity() * (rating - mean);
				s[1] += neighbor.similarity() * rating;
				s[2] += neighbor.similarity();
				s[3] += 1;
			});
		}
		Map<Long, CollabSignal> result = new HashMap<>();
		sums.forEach((beanId, s) -> {
			double dev = s[0] / s[2];
			double shrunk = dev * s[3] / (s[3] + 1);
			double score = Math.clamp(CollabSignal.NEUTRAL + shrunk / DEV_RANGE, 0.0, 1.0);
			result.put(beanId, new CollabSignal(score, s[1] / s[2], (int) s[3]));
		});
		return result;
	}

}
