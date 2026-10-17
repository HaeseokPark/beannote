package com.beanote.bean.service;

import java.util.Comparator;
import java.util.List;

import com.beanote.bean.domain.Bean;
import com.beanote.global.util.TextNormalizer;

/**
 * 같은 로스터리 안에서 이름이 비슷한 원두를 찾는다 (B-3 중복 방지).
 * 정규화한 이름이 서로를 포함하거나, 편집 거리 기반 유사도가 기준 이상이면 후보로 본다.
 */
final class DuplicateBeanDetector {

	static final double SIMILARITY_THRESHOLD = 0.7;
	static final int MAX_CANDIDATES = 5;

	private DuplicateBeanDetector() {
	}

	static List<Bean> findCandidates(String name, List<Bean> sameRoasteryBeans) {
		String target = TextNormalizer.normalize(name);
		if (target.isEmpty()) {
			return List.of();
		}
		return sameRoasteryBeans.stream()
				.filter(bean -> isSimilar(target, bean.getNameNormalized()))
				.sorted(Comparator.comparingDouble((Bean bean) -> similarity(target, bean.getNameNormalized()))
						.reversed())
				.limit(MAX_CANDIDATES)
				.toList();
	}

	static boolean isSimilar(String a, String b) {
		return a.contains(b) || b.contains(a) || similarity(a, b) >= SIMILARITY_THRESHOLD;
	}

	/** 1 - (편집 거리 / 긴 쪽 길이). 같으면 1.0 */
	static double similarity(String a, String b) {
		int longer = Math.max(a.length(), b.length());
		if (longer == 0) {
			return 1.0;
		}
		return 1.0 - (double) levenshtein(a, b) / longer;
	}

	private static int levenshtein(String a, String b) {
		int[] previous = new int[b.length() + 1];
		int[] current = new int[b.length() + 1];
		for (int j = 0; j <= b.length(); j++) {
			previous[j] = j;
		}
		for (int i = 1; i <= a.length(); i++) {
			current[0] = i;
			for (int j = 1; j <= b.length(); j++) {
				int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
				current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
			}
			int[] swap = previous;
			previous = current;
			current = swap;
		}
		return previous[b.length()];
	}

}
