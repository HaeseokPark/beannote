package com.beanote.recommendation.model;

/**
 * 취향이 가까운 다른 사용자.
 *
 * @param similarity 취향 벡터 가중 코사인 (0 초과 ~ 1)
 */
public record Neighbor(long userId, double similarity) {
}
