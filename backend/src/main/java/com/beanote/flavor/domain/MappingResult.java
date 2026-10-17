package com.beanote.flavor.domain;

/**
 * 향미 표현 판정 단계 (F-3).
 * EXACT = 사전 일치, AUTO = 자동 매핑(유사도 기준 이상), REVIEW = 검토 필요, UNMAPPED = 매핑 실패.
 */
public enum MappingResult {

	EXACT, AUTO, REVIEW, UNMAPPED;

	/** 사람 확인 없이 표준 태그로 써도 되는 판정인지 */
	public boolean isConfident() {
		return this == EXACT || this == AUTO;
	}

}
