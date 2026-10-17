package com.beanote.recommendation.model;

import java.util.Set;

import com.beanote.bean.domain.ProcessMethod;
import com.beanote.bean.domain.RoastLevel;

/**
 * 추천 후보 원두.
 *
 * @param origins     원산지 국가 코드 (블렌드는 여러 개)
 * @param singleOrigin 싱글오리진이면 그 국가, 블렌드면 null
 */
public record Candidate(long beanId, double[] vector, Set<String> origins, String singleOrigin,
		ProcessMethod process, RoastLevel roastLevel) {
}
