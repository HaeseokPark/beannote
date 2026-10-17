package com.beanote.taste.model;

import java.util.Map;

import com.beanote.bean.domain.ProcessMethod;
import com.beanote.bean.domain.RoastLevel;

/**
 * 원두 벡터 계산 입력.
 *
 * @param officialCategoryCounts 공식 향미 태그의 대분류별 개수
 * @param noteCount              이 원두의 노트 수
 * @param noteAverageAxes        노트 맛 축 평균 (1~5, 5개). 노트가 없으면 null
 * @param noteCategoryCounts     노트에 붙은 향미 태그의 대분류별 개수
 */
public record BeanFeatures(ProcessMethod process, RoastLevel roastLevel, Map<String, Integer> officialCategoryCounts,
		int noteCount, double[] noteAverageAxes, Map<String, Integer> noteCategoryCounts) {
}
