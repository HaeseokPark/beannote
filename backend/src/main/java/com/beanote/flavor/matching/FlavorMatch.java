package com.beanote.flavor.matching;

import com.beanote.flavor.domain.MappingResult;

/**
 * 향미 표현 한 조각의 판정 결과.
 *
 * @param raw        입력 원문 조각
 * @param normalized 비교에 쓴 정규화 값
 * @param score      0~100. EXACT 는 100
 * @param entry      가장 가까운 사전 항목. UNMAPPED 이면 null, REVIEW 이면 "제안"
 */
public record FlavorMatch(String raw, String normalized, MappingResult result, double score, DictionaryEntry entry) {

	static FlavorMatch unmapped(String raw, String normalized, double score) {
		return new FlavorMatch(raw, normalized, MappingResult.UNMAPPED, score, null);
	}

}
