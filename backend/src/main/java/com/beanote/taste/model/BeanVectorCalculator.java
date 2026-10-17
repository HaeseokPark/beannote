package com.beanote.taste.model;

import java.util.Map;

/**
 * 원두 벡터 (5-1).
 *
 * <pre>
 * w = n / (n + K)          n = 원두의 노트 수, K = 설정값
 * beanVector = (1 − w) × officialVector + w × notesAverageVector
 * </pre>
 *
 * 노트가 적으면 공식 정보(추정 맛 축 + 공식 향미)를, 많아지면 사용자 평가를 더 믿는다.
 */
public class BeanVectorCalculator {

	private final TasteEstimateTable estimates;
	private final double priorK;

	public BeanVectorCalculator(TasteEstimateTable estimates, double priorK) {
		if (priorK <= 0) {
			throw new IllegalArgumentException("K 는 0보다 커야 합니다.");
		}
		this.estimates = estimates;
		this.priorK = priorK;
	}

	public double[] calculate(BeanFeatures features) {
		double[] official = official(features);
		if (features.noteCount() <= 0 || features.noteAverageAxes() == null) {
			return official;
		}
		double[] notes = notesAverage(features, official);
		double w = features.noteCount() / (features.noteCount() + priorK);
		double[] result = new double[VectorLayout.DIMENSIONS];
		for (int i = 0; i < result.length; i++) {
			result[i] = (1 - w) * official[i] + w * notes[i];
		}
		return result;
	}

	private double[] official(BeanFeatures features) {
		double[] v = new double[VectorLayout.DIMENSIONS];
		double[] axes = estimates.estimate(features.process(), features.roastLevel());
		for (int i = 0; i < axes.length; i++) {
			v[VectorLayout.TASTE_START + i] = VectorLayout.axisValue(axes[i]);
		}
		putCategoryRatios(v, features.officialCategoryCounts());
		putProcessAndRoast(v, features);
		return v;
	}

	private static double[] notesAverage(BeanFeatures features, double[] official) {
		double[] v = new double[VectorLayout.DIMENSIONS];
		double[] axes = features.noteAverageAxes();
		for (int i = 0; i < axes.length; i++) {
			v[VectorLayout.TASTE_START + i] = VectorLayout.axisValue(axes[i]);
		}
		if (total(features.noteCategoryCounts()) > 0) {
			putCategoryRatios(v, features.noteCategoryCounts());
		}
		else {
			// 노트에 향미 태그가 없으면 공식 향미를 그대로 쓴다
			System.arraycopy(official, VectorLayout.FLAVOR_START, v, VectorLayout.FLAVOR_START,
					VectorLayout.PROCESS_START - VectorLayout.FLAVOR_START);
		}
		putProcessAndRoast(v, features);
		return v;
	}

	private static void putCategoryRatios(double[] v, Map<String, Integer> counts) {
		int total = total(counts);
		if (total == 0) {
			return;
		}
		counts.forEach((code, count) -> v[VectorLayout.categoryIndex(code)] = count / (double) total);
	}

	private static void putProcessAndRoast(double[] v, BeanFeatures features) {
		v[VectorLayout.processIndex(features.process())] = 1.0;
		v[VectorLayout.ROAST_INDEX] = VectorLayout.roastValue(features.roastLevel());
	}

	private static int total(Map<String, Integer> counts) {
		return counts == null ? 0 : counts.values().stream().mapToInt(Integer::intValue).sum();
	}

}
