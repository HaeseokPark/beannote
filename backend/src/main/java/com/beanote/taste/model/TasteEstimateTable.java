package com.beanote.taste.model;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.beanote.bean.domain.ProcessMethod;
import com.beanote.bean.domain.RoastLevel;

/**
 * 기본 맛 축 추정표 (5-1). 공식 정보에는 맛 축 점수가 없으므로 로스팅 포인트 기본값에 가공 방식 보정을 더해 초기값을 만든다.
 * 예: 라이트 + 워시드 → 산미 높음, 바디 낮음. 값은 설정 파일(application.yml)에서 온다.
 */
public class TasteEstimateTable {

	private final Map<RoastLevel, double[]> byRoast = new EnumMap<>(RoastLevel.class);
	private final Map<ProcessMethod, double[]> processAdjust = new EnumMap<>(ProcessMethod.class);

	public TasteEstimateTable(Map<RoastLevel, List<Double>> byRoast, Map<ProcessMethod, List<Double>> processAdjust) {
		for (RoastLevel level : RoastLevel.values()) {
			this.byRoast.put(level, toArray(byRoast.get(level), "roast " + level));
		}
		for (ProcessMethod process : ProcessMethod.values()) {
			List<Double> adjust = processAdjust.get(process);
			this.processAdjust.put(process, adjust == null ? new double[5] : toArray(adjust, "process " + process));
		}
	}

	/** 맛 축 5개 추정 점수 (1~5) */
	public double[] estimate(ProcessMethod process, RoastLevel roastLevel) {
		double[] base = byRoast.get(roastLevel);
		double[] adjust = processAdjust.get(process);
		double[] result = new double[5];
		for (int i = 0; i < 5; i++) {
			result[i] = Math.clamp(base[i] + adjust[i], 1.0, 5.0);
		}
		return result;
	}

	private static double[] toArray(List<Double> values, String name) {
		if (values == null || values.size() != 5) {
			throw new IllegalArgumentException("맛 축 추정값은 5개여야 합니다: " + name);
		}
		return values.stream().mapToDouble(Double::doubleValue).toArray();
	}

}
