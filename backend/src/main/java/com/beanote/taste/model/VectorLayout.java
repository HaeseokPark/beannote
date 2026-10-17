package com.beanote.taste.model;

import java.util.List;

import com.beanote.bean.domain.ProcessMethod;
import com.beanote.bean.domain.RoastLevel;

/**
 * 원두·취향 벡터의 차원 배치 (5-1). 21차원.
 *
 * <pre>
 * [0..4]   맛 축 5개      (점수 − 3) / 2  → −1 ~ 1
 * [5..14]  향미 대분류 10개 해당 대분류 태그 비율 → 0 ~ 1 (합 1)
 * [15..19] 가공 방식 5개   원-핫
 * [20]     로스팅 포인트   LIGHT=−1 … DARK=1 (순서형)
 * </pre>
 *
 * 맛 축과 로스팅은 기획서의 0~1 대신 가운데를 0 으로 둔다. 그래야 "쓴맛을 싫어함(−)"과 "쓴맛이 강한 원두(+)"의 곱이
 * 음수가 되어 감점되고, 라이트 로스팅(−) 선호자에게 라이트 원두(−)가 가점이 된다. 0~1 로 두면 낮은 값을 좋아하는 취향이
 * 내적에 전혀 반영되지 않는다.
 */
public final class VectorLayout {

	public static final List<String> AXES = List.of("ACIDITY", "SWEETNESS", "BODY", "BITTERNESS", "AFTERTASTE");
	public static final List<String> CATEGORY_CODES = List.of("FLORAL", "BERRY", "CITRUS", "STONE_TROPICAL", "SWEET",
			"NUTTY_COCOA", "SPICE", "ROASTED", "FERMENTED", "GREEN");

	public static final int TASTE_START = 0;
	public static final int FLAVOR_START = TASTE_START + AXES.size();
	public static final int PROCESS_START = FLAVOR_START + CATEGORY_CODES.size();
	public static final int ROAST_INDEX = PROCESS_START + ProcessMethod.values().length;
	public static final int DIMENSIONS = ROAST_INDEX + 1;

	private VectorLayout() {
	}

	public enum Section {
		TASTE, FLAVOR, PROCESS, ROAST
	}

	public static Section sectionOf(int index) {
		if (index < FLAVOR_START) {
			return Section.TASTE;
		}
		if (index < PROCESS_START) {
			return Section.FLAVOR;
		}
		if (index < ROAST_INDEX) {
			return Section.PROCESS;
		}
		return Section.ROAST;
	}

	/** 맛 축 점수(1~5) → −1~1 */
	public static double axisValue(double score) {
		return (score - 3.0) / 2.0;
	}

	/** 로스팅 → −1(LIGHT) ~ 1(DARK) */
	public static double roastValue(RoastLevel level) {
		return level.ordinal() / 2.0 - 1.0;
	}

	public static int categoryIndex(String categoryCode) {
		int index = CATEGORY_CODES.indexOf(categoryCode);
		if (index < 0) {
			throw new IllegalArgumentException("알 수 없는 향미 대분류: " + categoryCode);
		}
		return FLAVOR_START + index;
	}

	public static int processIndex(ProcessMethod process) {
		return PROCESS_START + process.ordinal();
	}

}
