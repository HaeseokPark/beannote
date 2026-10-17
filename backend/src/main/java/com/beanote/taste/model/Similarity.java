package com.beanote.taste.model;

/**
 * 구간 가중 코사인 유사도.
 *
 * <pre>
 * cos = Σ wᵢ aᵢ bᵢ / (√Σ wᵢ aᵢ² · √Σ wᵢ bᵢ²)
 * </pre>
 *
 * 차원별 기여도(contribution) wᵢ aᵢ bᵢ / (|a|·|b|) 의 합이 코사인과 같아서, 추천 이유를 만들 때 그대로 분해할 수 있다.
 */
public final class Similarity {

	private Similarity() {
	}

	public static double cosine(double[] a, double[] b, SectionWeights weights) {
		double denominator = norm(a, weights) * norm(b, weights);
		if (denominator == 0) {
			return 0;
		}
		double dot = 0;
		for (int i = 0; i < a.length; i++) {
			dot += weights.ofIndex(i) * a[i] * b[i];
		}
		return dot / denominator;
	}

	/** 차원별 기여도. 합하면 cosine 과 같다. */
	public static double[] contributions(double[] a, double[] b, SectionWeights weights) {
		double[] result = new double[a.length];
		double denominator = norm(a, weights) * norm(b, weights);
		if (denominator == 0) {
			return result;
		}
		for (int i = 0; i < a.length; i++) {
			result[i] = weights.ofIndex(i) * a[i] * b[i] / denominator;
		}
		return result;
	}

	/** 코사인(−1~1)을 0~1 로 옮긴다. 다른 점수 항목과 단위를 맞추기 위해 쓴다. */
	public static double toUnit(double cosine) {
		return (cosine + 1.0) / 2.0;
	}

	public static double norm(double[] v, SectionWeights weights) {
		double sum = 0;
		for (int i = 0; i < v.length; i++) {
			sum += weights.ofIndex(i) * v[i] * v[i];
		}
		return Math.sqrt(sum);
	}

	/** 가중 노름으로 나눠 길이를 1 로 맞춘다. 0 벡터는 그대로 */
	public static double[] normalize(double[] v, SectionWeights weights) {
		double n = norm(v, weights);
		double[] result = v.clone();
		if (n == 0) {
			return result;
		}
		for (int i = 0; i < result.length; i++) {
			result[i] /= n;
		}
		return result;
	}

}
