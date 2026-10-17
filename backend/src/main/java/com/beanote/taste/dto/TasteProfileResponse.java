package com.beanote.taste.dto;

import java.math.BigDecimal;
import java.util.List;

import com.beanote.taste.model.TasteVectorCalculator.Confidence;

/**
 * 내 취향 프로필 (4-5 T-1 ~ T-4).
 *
 * @param confidence      T-4 취향 신뢰도 (NONE·SURVEY·FORMING·STABLE)
 * @param notesUntilStable "노트 N개 더 쓰면 취향이 안정돼요" 의 N
 * @param radar           T-1 맛 축 레이더 (1~5)
 * @param radarSource     레이더 근거: NOTES(높게 평가한 노트) · SURVEY(설문) · NONE
 */
public record TasteProfileResponse(
		Confidence confidence,
		int noteCount,
		int notesUntilStable,
		Radar radar,
		String radarSource,
		List<FlavorScore> likedFlavors,
		List<FlavorScore> dislikedFlavors,
		List<CategoryScore> likedCategories,
		List<CategoryScore> dislikedCategories,
		List<AttributeRating> processes,
		List<AttributeRating> origins,
		List<AttributeRating> roastLevels) {

	public record Radar(BigDecimal acidity, BigDecimal sweetness, BigDecimal body, BigDecimal bitterness,
			BigDecimal aftertaste) {
	}

	/** T-2 소분류 단위. score 는 평균 대비 별점 차이의 합(감쇠 반영) */
	public record FlavorScore(String code, String name, String categoryCode, BigDecimal score) {
	}

	/** T-2 대분류 단위. fromSurvey = 설문에서 고른 항목 */
	public record CategoryScore(String code, String name, BigDecimal score, boolean fromSurvey) {
	}

	/** T-3 가공 방식·원산지·로스팅별 평균 별점 */
	public record AttributeRating(String code, String name, BigDecimal avgRating, int noteCount) {
	}

}
