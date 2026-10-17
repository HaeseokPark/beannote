package com.beanote.recommendation.model;

/**
 * 최종 점수 가중치 (5-3).
 *
 * <pre>
 * score = content × contentSim + collab × collabScore + explore × explore − dislikePenalty × 싫어하는 향 비율
 * </pre>
 */
public record ScoreWeights(double content, double collab, double explore, double dislikePenalty) {
}
