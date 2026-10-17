package com.beanote.taste.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import com.beanote.bean.domain.RoastLevel;

/**
 * 사용자 취향 벡터 (5-2).
 *
 * <pre>
 * wᵢ = (평점ᵢ − 사용자 평균 평점) × decay^(경과 일수ᵢ / 7)
 * tasteVector = Σ (wᵢ × beanVectorᵢ) / Σ |wᵢ|
 * </pre>
 *
 * <ul>
 * <li>평균보다 높게 준 커피 쪽으로 끌리고 낮게 준 커피에서 멀어진다. 평균을 빼고 Σ|w| 로 나누므로 평점을 짜게 주든 후하게 주든
 * 상대적인 순서가 같으면 같은 벡터가 나온다.</li>
 * <li>모든 평점이 같아 Σ|w| = 0 이면 선호 신호가 없으므로, 감쇠 가중치로 낸 "마신 원두 평균"을 쓴다.</li>
 * <li>노트가 surveyMinNotes 개 미만이면 설문 벡터, surveyMinNotes~stableNotes 사이는 설문과 선형으로 섞고,
 * stableNotes 개 이상이면 노트만 쓴다. 두 벡터는 크기가 달라 길이를 1 로 맞춘 뒤 섞는다.</li>
 * </ul>
 */
public class TasteVectorCalculator {

	private static final double EPSILON = 1e-9;

	private final double decayPerWeek;
	private final int surveyMinNotes;
	private final int stableNotes;
	private final SectionWeights weights;

	public TasteVectorCalculator(double decayPerWeek, int surveyMinNotes, int stableNotes, SectionWeights weights) {
		if (!(0 < decayPerWeek && decayPerWeek <= 1) || !(0 < surveyMinNotes && surveyMinNotes < stableNotes)) {
			throw new IllegalArgumentException("decay 는 (0, 1], 0 < surveyMinNotes < stableNotes 여야 합니다.");
		}
		this.decayPerWeek = decayPerWeek;
		this.surveyMinNotes = surveyMinNotes;
		this.stableNotes = stableNotes;
		this.weights = weights;
	}

	public record NoteSignal(double rating, LocalDate tastedOn, double[] beanVector) {
	}

	/**
	 * @param preferences    맛 축 선호 1~5 (5개)
	 * @param likedCategories 좋아하는 향미 대분류 코드
	 * @param preferredRoast  평소 마시는 커피로 추정한 로스팅
	 */
	public record SurveySignal(int[] preferences, Set<String> likedCategories, RoastLevel preferredRoast) {
	}

	public enum Confidence {
		/** 설문을 건너뛰었고 노트도 부족 → 인기순 추천 */
		NONE,
		/** 설문 기반 */
		SURVEY,
		/** 형성 중 */
		FORMING,
		/** 안정 */
		STABLE
	}

	/**
	 * @param vector      취향 벡터. NONE 이면 null
	 * @param notesWeight 노트가 차지하는 비중 0~1 (나머지는 설문)
	 */
	public record TasteResult(double[] vector, Confidence confidence, int noteCount, double notesWeight) {
	}

	public TasteResult calculate(List<NoteSignal> notes, SurveySignal survey, LocalDate today) {
		int n = notes.size();
		Confidence confidence = confidenceOf(n, survey != null);
		double[] surveyVector = survey == null ? null : surveyVector(survey);

		if (n < surveyMinNotes) {
			return new TasteResult(surveyVector, confidence, n, 0);
		}
		double[] notesVector = notesVector(notes, today);
		if (surveyVector == null || n >= stableNotes) {
			return new TasteResult(notesVector, confidence, n, 1);
		}
		// n = surveyMinNotes 에서 노트 비중이 조금 생기고, n = stableNotes 에서 1 이 된다
		double t = (n - surveyMinNotes + 1) / (double) (stableNotes - surveyMinNotes + 1);
		double[] a = Similarity.normalize(surveyVector, weights);
		double[] b = Similarity.normalize(notesVector, weights);
		double[] mixed = new double[VectorLayout.DIMENSIONS];
		for (int i = 0; i < mixed.length; i++) {
			mixed[i] = (1 - t) * a[i] + t * b[i];
		}
		return new TasteResult(mixed, confidence, n, t);
	}

	public Confidence confidenceOf(int noteCount, boolean hasSurvey) {
		if (noteCount >= stableNotes) {
			return Confidence.STABLE;
		}
		if (noteCount >= surveyMinNotes) {
			return Confidence.FORMING;
		}
		return hasSurvey ? Confidence.SURVEY : Confidence.NONE;
	}

	double[] notesVector(List<NoteSignal> notes, LocalDate today) {
		double mean = notes.stream().mapToDouble(NoteSignal::rating).average().orElse(0);
		double[] sum = new double[VectorLayout.DIMENSIONS];
		double absSum = 0;
		for (NoteSignal note : notes) {
			double w = (note.rating() - mean) * decay(note.tastedOn(), today);
			add(sum, note.beanVector(), w);
			absSum += Math.abs(w);
		}
		if (absSum > EPSILON) {
			return scale(sum, 1 / absSum);
		}

		// 모든 평점이 같으면 선호 차이를 알 수 없으므로 마신 원두들의 (감쇠) 평균을 쓴다
		double[] average = new double[VectorLayout.DIMENSIONS];
		double total = 0;
		for (NoteSignal note : notes) {
			double w = decay(note.tastedOn(), today);
			add(average, note.beanVector(), w);
			total += w;
		}
		return scale(average, 1 / total);
	}

	double[] surveyVector(SurveySignal survey) {
		double[] v = new double[VectorLayout.DIMENSIONS];
		for (int i = 0; i < survey.preferences().length; i++) {
			v[VectorLayout.TASTE_START + i] = VectorLayout.axisValue(survey.preferences()[i]);
		}
		for (String code : survey.likedCategories()) {
			v[VectorLayout.categoryIndex(code)] = 1.0 / survey.likedCategories().size();
		}
		// 가공 방식은 설문으로 알 수 없어 0
		v[VectorLayout.ROAST_INDEX] = VectorLayout.roastValue(survey.preferredRoast());
		return v;
	}

	private double decay(LocalDate tastedOn, LocalDate today) {
		long days = Math.max(0, ChronoUnit.DAYS.between(tastedOn, today));
		return Math.pow(decayPerWeek, days / 7.0);
	}

	private static void add(double[] target, double[] v, double w) {
		for (int i = 0; i < target.length; i++) {
			target[i] += w * v[i];
		}
	}

	private static double[] scale(double[] v, double factor) {
		double[] result = new double[v.length];
		for (int i = 0; i < v.length; i++) {
			result[i] = v[i] * factor;
		}
		return result;
	}

}
