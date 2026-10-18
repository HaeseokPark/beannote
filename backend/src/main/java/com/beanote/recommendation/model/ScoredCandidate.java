package com.beanote.recommendation.model;

/**
 * 점수가 매겨진 후보.
 *
 * @param collabSignal  이웃 평가 (없으면 null)
 * @param contentSim    취향–원두 가중 코사인을 0~1 로 옮긴 값
 * @param contributions 최종 점수에 대한 차원별 기여도 (α × 코사인 기여도 / 2)
 * @param newOrigin     아직 안 마셔 본 원산지인지
 * @param newProcess    아직 안 마셔 본 가공 방식인지
 */
public record ScoredCandidate(Candidate candidate, double score, double contentSim, double collab,
		CollabSignal collabSignal, double explore,
		double dislikePenalty, double[] contributions, boolean newOrigin, boolean newProcess) {
}
