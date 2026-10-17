package com.beanote.bean.dto;

/**
 * 원두 목록 정렬 (B-1). POPULAR = 노트 수, RATING = 평균 별점. 같으면 최신 원두가 먼저.
 */
public enum BeanSort {
	LATEST, NAME, POPULAR, RATING
}
