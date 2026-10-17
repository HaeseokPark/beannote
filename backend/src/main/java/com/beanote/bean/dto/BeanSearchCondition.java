package com.beanote.bean.dto;

import com.beanote.bean.domain.OriginCountry;
import com.beanote.bean.domain.ProcessMethod;
import com.beanote.bean.domain.RoastLevel;

/**
 * 원두 목록 검색 조건 (B-1). 모든 조건은 선택이며 AND 로 결합한다.
 *
 * @param q              원두 이름·로스터리 이름·지역·원산지 국가 이름 검색어
 * @param origin         원산지 국가 (블렌드는 구성 원산지 중 하나라도 일치하면 포함)
 * @param flavorCategory 공식 향미에 이 대분류가 포함된 원두
 */
public record BeanSearchCondition(String q, OriginCountry origin, ProcessMethod process, RoastLevel roastLevel,
		String flavorCategory, BeanSort sort, String cursor, int size) {

	public static final int DEFAULT_SIZE = 20;
	public static final int MAX_SIZE = 50;

	public BeanSearchCondition {
		sort = sort == null ? BeanSort.LATEST : sort;
		size = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
	}

}
