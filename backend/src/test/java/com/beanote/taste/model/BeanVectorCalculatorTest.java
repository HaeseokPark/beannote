package com.beanote.taste.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.beanote.bean.domain.ProcessMethod;
import com.beanote.bean.domain.RoastLevel;

/**
 * 원두 벡터 (5-1): 공식 정보 + 노트 평균의 가중 결합.
 */
class BeanVectorCalculatorTest {

	private final TasteEstimateTable table = new TasteEstimateTable(
			Map.of(RoastLevel.LIGHT, List.of(4.5, 3.0, 2.0, 1.5, 3.0),
					RoastLevel.MEDIUM_LIGHT, List.of(4.0, 3.5, 2.5, 2.0, 3.0),
					RoastLevel.MEDIUM, List.of(3.0, 3.5, 3.0, 3.0, 3.5),
					RoastLevel.MEDIUM_DARK, List.of(2.0, 3.0, 4.0, 4.0, 3.5),
					RoastLevel.DARK, List.of(1.5, 2.5, 4.5, 4.5, 4.0)),
			Map.of(ProcessMethod.WASHED, List.of(0.5, 0.0, -0.5, 0.0, 0.0)));
	private final BeanVectorCalculator calculator = new BeanVectorCalculator(table, 5);

	@Test
	void 노트가_없으면_추정표와_공식_향미로_만든다() {
		double[] v = calculator.calculate(new BeanFeatures(ProcessMethod.WASHED, RoastLevel.LIGHT,
				Map.of("FLORAL", 2, "CITRUS", 1), 0, null, Map.of()));

		// 라이트 + 워시드 → 산미 4.5+0.5 = 5 → 1.0, 바디 2.0−0.5 = 1.5 → −0.75
		assertThat(v[VectorLayout.TASTE_START]).isEqualTo(1.0);
		assertThat(v[VectorLayout.TASTE_START + 2]).isEqualTo(-0.75);
		assertThat(v[VectorLayout.categoryIndex("FLORAL")]).isCloseTo(2 / 3.0, within(1e-9));
		assertThat(v[VectorLayout.categoryIndex("CITRUS")]).isCloseTo(1 / 3.0, within(1e-9));
		assertThat(v[VectorLayout.processIndex(ProcessMethod.WASHED)]).isEqualTo(1.0);
		assertThat(v[VectorLayout.ROAST_INDEX]).isEqualTo(-1.0);
	}

	@Test
	void 노트가_많아질수록_사용자_평가를_더_믿는다() {
		double[] axes = { 1, 1, 5, 5, 5 };   // 공식 추정과 정반대 평가
		double acidityAt5 = acidity(5, axes);     // w = 5/10 = 0.5
		double acidityAt45 = acidity(45, axes);   // w = 45/50 = 0.9

		assertThat(acidityAt5).isCloseTo(0.5 * 1.0 + 0.5 * -1.0, within(1e-9));
		assertThat(acidityAt45).isCloseTo(0.1 * 1.0 + 0.9 * -1.0, within(1e-9));
	}

	@Test
	void 노트에_향미가_없으면_공식_향미를_유지한다() {
		double[] v = calculator.calculate(new BeanFeatures(ProcessMethod.WASHED, RoastLevel.LIGHT,
				Map.of("FLORAL", 1), 100, new double[] { 3, 3, 3, 3, 3 }, Map.of()));
		assertThat(v[VectorLayout.categoryIndex("FLORAL")]).isEqualTo(1.0);
	}

	private double acidity(int noteCount, double[] axes) {
		return calculator.calculate(new BeanFeatures(ProcessMethod.WASHED, RoastLevel.LIGHT, Map.of("FLORAL", 1),
				noteCount, axes, Map.of("NUTTY_COCOA", 3)))[VectorLayout.TASTE_START];
	}

}
