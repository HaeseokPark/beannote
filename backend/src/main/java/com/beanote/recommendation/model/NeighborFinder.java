package com.beanote.recommendation.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.beanote.taste.model.SectionWeights;
import com.beanote.taste.model.Similarity;

/**
 * 협업 필터링 이웃 (5-3): 취향 벡터의 가중 코사인이 높은 상위 k명. 유사도가 0 이하인 사용자는 이웃이 아니다.
 * JPA 에 의존하지 않는 순수 클래스 (매일 새벽 배치와 오프라인 평가가 같은 코드를 쓴다).
 */
public class NeighborFinder {

	private final int k;
	private final SectionWeights weights;

	public NeighborFinder(int k, SectionWeights weights) {
		if (k <= 0) {
			throw new IllegalArgumentException("이웃 수는 1 이상이어야 합니다.");
		}
		this.k = k;
		this.weights = weights;
	}

	/** 모든 사용자의 이웃. 사용자 n 명이면 n² / 2 번 비교한다 (1,000명 기준 수십 ms). */
	public Map<Long, List<Neighbor>> findAll(Map<Long, double[]> tasteVectors) {
		List<Long> ids = new ArrayList<>(tasteVectors.keySet());
		Map<Long, List<Neighbor>> candidates = new HashMap<>();
		ids.forEach(id -> candidates.put(id, new ArrayList<>()));
		for (int i = 0; i < ids.size(); i++) {
			for (int j = i + 1; j < ids.size(); j++) {
				long a = ids.get(i);
				long b = ids.get(j);
				double similarity = Similarity.cosine(tasteVectors.get(a), tasteVectors.get(b), weights);
				if (similarity > 0) {
					candidates.get(a).add(new Neighbor(b, similarity));
					candidates.get(b).add(new Neighbor(a, similarity));
				}
			}
		}
		Map<Long, List<Neighbor>> result = new HashMap<>();
		candidates.forEach((id, list) -> result.put(id, top(list)));
		return result;
	}

	private List<Neighbor> top(List<Neighbor> neighbors) {
		return neighbors.stream()
				.sorted(Comparator.comparingDouble(Neighbor::similarity).reversed()
						.thenComparingLong(Neighbor::userId))
				.limit(k)
				.toList();
	}

}
