package com.beanote.recommendation.model;

/**
 * 이웃들이 한 원두를 어떻게 평가했는지.
 *
 * @param score     0~1. 0.5 = 이웃 평균과 같음, 1 = 이웃들이 자기 평균보다 2점 이상 높게 줌
 * @param avgRating 이웃들이 준 별점의 유사도 가중 평균 (추천 이유 문구용)
 * @param raters    평가한 이웃 수
 */
public record CollabSignal(double score, double avgRating, int raters) {

	/** 이웃이 아무도 평가하지 않은 원두: 가점도 감점도 하지 않는다 */
	public static final double NEUTRAL = 0.5;

}
