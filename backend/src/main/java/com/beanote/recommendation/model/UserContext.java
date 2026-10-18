package com.beanote.recommendation.model;

import java.util.Map;
import java.util.Set;

import com.beanote.bean.domain.ProcessMethod;

/**
 * 추천 계산에 필요한 사용자 정보.
 *
 * @param taste              취향 벡터
 * @param triedOrigins       노트를 쓴 원두들의 원산지 (비어 있으면 탐험 가점을 주지 않는다)
 * @param triedProcesses     노트를 쓴 원두들의 가공 방식
 * @param dislikedCategories 설문에서 싫어한다고 고른 향미 대분류 코드
 * @param collab             원두별 협업 신호. 이웃이 평가하지 않은 원두는 없다
 * @param collabEnabled      협업 점수를 쓰는지 (사용자 수가 기준 미만이면 false → 협업 항목 0)
 * @param surveyBased        취향이 주로 설문에서 왔는지 (추천 이유 문구가 달라진다)
 */
public record UserContext(double[] taste, Set<String> triedOrigins, Set<ProcessMethod> triedProcesses,
		Set<String> dislikedCategories, Map<Long, CollabSignal> collab, boolean collabEnabled, boolean surveyBased) {

	/** 협업 점수 없이 (사용자가 적거나 이웃이 없을 때) */
	public static UserContext withoutCollab(double[] taste, Set<String> triedOrigins,
			Set<ProcessMethod> triedProcesses, Set<String> dislikedCategories, boolean surveyBased) {
		return new UserContext(taste, triedOrigins, triedProcesses, dislikedCategories, Map.of(), false, surveyBased);
	}

}
