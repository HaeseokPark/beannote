package com.beanote.taste.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.beanote.bean.domain.RoastLevel;
import com.beanote.taste.model.TasteVectorCalculator.Confidence;
import com.beanote.taste.model.TasteVectorCalculator.NoteSignal;
import com.beanote.taste.model.TasteVectorCalculator.SurveySignal;
import com.beanote.taste.model.TasteVectorCalculator.TasteResult;

/**
 * 취향 벡터 규칙 고정 테스트 (5-2, 9-3). Phase 4 완료 조건: 평점 기준 보정, 감쇠, 설문 혼합 구간.
 */
class TasteVectorCalculatorTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 17);
	private static final SectionWeights WEIGHTS = new SectionWeights(1.0, 0.6, 0.4, 0.8);
	private final TasteVectorCalculator calculator = new TasteVectorCalculator(0.97, 3, 10, WEIGHTS);

	// 서로 뚜렷이 다른 원두 벡터 3개: 밝은 산미·꽃 / 묵직·초콜릿 / 중간
	private static final double[] BRIGHT = vector(1.0, -0.5, "FLORAL", -1.0);
	private static final double[] HEAVY = vector(-1.0, 1.0, "NUTTY_COCOA", 1.0);
	private static final double[] MIDDLE = vector(0.0, 0.0, "SWEET", 0.0);

	@Test
	void 평점_기준이_다른_두_사용자가_같은_취향_벡터를_얻는다() {
		// 짜게 주는 사용자(3, 4, 5)와 후하게 주는 사용자(4, 4.5, 5): 순서와 간격 비율이 같다
		List<NoteSignal> strict = notes(new double[] { 3.0, 4.0, 5.0 });
		List<NoteSignal> generous = notes(new double[] { 4.0, 4.5, 5.0 });

		double[] a = calculator.calculate(strict, null, TODAY).vector();
		double[] b = calculator.calculate(generous, null, TODAY).vector();

		assertThat(a).containsExactly(b, within(1e-9));
	}

	@Test
	void 높게_준_커피_쪽으로_끌리고_낮게_준_커피에서_멀어진다() {
		double[] taste = calculator.calculate(List.of(
				new NoteSignal(5.0, TODAY, BRIGHT),
				new NoteSignal(1.0, TODAY, HEAVY),
				new NoteSignal(3.0, TODAY, MIDDLE)), null, TODAY).vector();

		assertThat(Similarity.cosine(taste, BRIGHT, WEIGHTS)).isGreaterThan(0.5);
		assertThat(Similarity.cosine(taste, HEAVY, WEIGHTS)).isLessThan(-0.5);
	}

	@Test
	void 오래된_기록일수록_영향이_줄어든다() {
		// 같은 별점 차이라도 1년 전에 좋아한 커피보다 어제 좋아한 커피 쪽으로 더 끌린다
		double[] taste = calculator.calculate(List.of(
				new NoteSignal(5.0, TODAY.minusDays(365), HEAVY),
				new NoteSignal(5.0, TODAY.minusDays(1), BRIGHT),
				new NoteSignal(1.0, TODAY.minusDays(1), MIDDLE),
				new NoteSignal(1.0, TODAY.minusDays(365), MIDDLE)), null, TODAY).vector();

		assertThat(Similarity.cosine(taste, BRIGHT, WEIGHTS))
				.isGreaterThan(Similarity.cosine(taste, HEAVY, WEIGHTS));
	}

	@Test
	void 모든_평점이_같으면_마신_원두의_평균을_쓴다() {
		// Σ|w| = 0 이면 0 으로 나누지 않고 평균으로 대신한다
		TasteResult result = calculator.calculate(List.of(
				new NoteSignal(4.0, TODAY, BRIGHT),
				new NoteSignal(4.0, TODAY, BRIGHT),
				new NoteSignal(4.0, TODAY, MIDDLE)), null, TODAY);

		assertThat(result.vector()).doesNotContain(Double.NaN);
		assertThat(Similarity.cosine(result.vector(), BRIGHT, WEIGHTS)).isGreaterThan(0.8);
	}

	@Test
	void 노트가_3개_미만이면_설문_벡터만_쓴다() {
		SurveySignal survey = brightSurvey();
		TasteResult withTwoNotes = calculator.calculate(notes(new double[] { 5.0, 1.0 }), survey, TODAY);

		assertThat(withTwoNotes.confidence()).isEqualTo(Confidence.SURVEY);
		assertThat(withTwoNotes.notesWeight()).isZero();
		assertThat(withTwoNotes.vector()).containsExactly(calculator.surveyVector(survey), within(1e-12));
	}

	@Test
	void 노트_3개부터_9개까지는_설문과_섞고_10개부터는_노트만_쓴다() {
		SurveySignal survey = brightSurvey();
		double previous = 0;
		for (int n = 3; n <= 9; n++) {
			TasteResult result = calculator.calculate(manyNotes(n), survey, TODAY);
			assertThat(result.confidence()).as("n=%d", n).isEqualTo(Confidence.FORMING);
			assertThat(result.notesWeight()).as("n=%d", n).isGreaterThan(previous).isLessThan(1.0);
			previous = result.notesWeight();
		}
		TasteResult stable = calculator.calculate(manyNotes(10), survey, TODAY);
		assertThat(stable.confidence()).isEqualTo(Confidence.STABLE);
		assertThat(stable.notesWeight()).isEqualTo(1.0);
		assertThat(stable.vector()).containsExactly(calculator.notesVector(manyNotes(10), TODAY), within(1e-12));
	}

	@Test
	void 설문을_건너뛰고_노트도_부족하면_취향이_없다() {
		TasteResult result = calculator.calculate(notes(new double[] { 5.0 }), null, TODAY);
		assertThat(result.confidence()).isEqualTo(Confidence.NONE);
		assertThat(result.vector()).isNull();
	}

	@Test
	void 설문_벡터는_좋아하는_향과_낮은_쓴맛을_반영한다() {
		double[] v = calculator.surveyVector(brightSurvey());
		assertThat(v[VectorLayout.TASTE_START]).isEqualTo(1.0);            // 산미 5 → 1
		assertThat(v[VectorLayout.TASTE_START + 3]).isEqualTo(-1.0);       // 쓴맛 1 → −1
		assertThat(v[VectorLayout.categoryIndex("FLORAL")]).isEqualTo(0.5);
		assertThat(v[VectorLayout.ROAST_INDEX]).isEqualTo(-1.0);           // LIGHT
		// 쓴맛을 싫어하는 설문은 무거운 원두와 반대 방향
		assertThat(Similarity.cosine(v, HEAVY, WEIGHTS)).isNegative();
		assertThat(Similarity.cosine(v, BRIGHT, WEIGHTS)).isPositive();
	}

	private static SurveySignal brightSurvey() {
		return new SurveySignal(new int[] { 5, 3, 2, 1, 3 }, Set.of("FLORAL", "CITRUS"), RoastLevel.LIGHT);
	}

	/** BRIGHT, HEAVY, MIDDLE 순서로 같은 날 마신 노트 */
	private static List<NoteSignal> notes(double[] ratings) {
		double[][] beans = { BRIGHT, HEAVY, MIDDLE };
		return IntStream.range(0, ratings.length)
				.mapToObj(i -> new NoteSignal(ratings[i], TODAY, beans[i % 3]))
				.toList();
	}

	private static List<NoteSignal> manyNotes(int n) {
		return IntStream.range(0, n)
				.mapToObj(i -> new NoteSignal(i % 2 == 0 ? 5.0 : 2.0, TODAY.minusDays(i), i % 2 == 0 ? HEAVY : MIDDLE))
				.toList();
	}

	private static double[] vector(double acidity, double body, String category, double roast) {
		double[] v = new double[VectorLayout.DIMENSIONS];
		v[VectorLayout.TASTE_START] = acidity;
		v[VectorLayout.TASTE_START + 2] = body;
		v[VectorLayout.TASTE_START + 3] = body;
		v[VectorLayout.categoryIndex(category)] = 1.0;
		v[VectorLayout.ROAST_INDEX] = roast;
		return v;
	}

}
