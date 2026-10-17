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
 * @param collabScores       원두별 협업 점수 0~1 (Phase 7). 없으면 빈 맵
 * @param surveyBased        취향이 주로 설문에서 왔는지 (추천 이유 문구가 달라진다)
 */
public record UserContext(double[] taste, Set<String> triedOrigins, Set<ProcessMethod> triedProcesses,
		Set<String> dislikedCategories, Map<Long, Double> collabScores, boolean surveyBased) {
}
