package com.beanote.taste.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntToDoubleFunction;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beanote.bean.domain.Bean;
import com.beanote.bean.domain.OriginCountry;
import com.beanote.bean.domain.ProcessMethod;
import com.beanote.bean.domain.RoastLevel;
import com.beanote.flavor.domain.FlavorCategory;
import com.beanote.flavor.domain.FlavorTag;
import com.beanote.flavor.repository.FlavorCategoryRepository;
import com.beanote.global.config.ClockConfig;
import com.beanote.note.domain.TastingNote;
import com.beanote.note.repository.TastingNoteRepository;
import com.beanote.survey.domain.FlavorPreference;
import com.beanote.survey.domain.TasteSurvey;
import com.beanote.survey.repository.TasteSurveyRepository;
import com.beanote.taste.config.RecommendationProperties;
import com.beanote.taste.domain.TasteProfile;
import com.beanote.taste.dto.TasteProfileResponse;
import com.beanote.taste.dto.TasteProfileResponse.AttributeRating;
import com.beanote.taste.dto.TasteProfileResponse.CategoryScore;
import com.beanote.taste.dto.TasteProfileResponse.FlavorScore;
import com.beanote.taste.dto.TasteProfileResponse.Radar;
import com.beanote.taste.model.TasteVectorCalculator;
import com.beanote.taste.model.TasteVectorCalculator.NoteSignal;
import com.beanote.taste.model.TasteVectorCalculator.SurveySignal;
import com.beanote.taste.model.TasteVectorCalculator.TasteResult;
import com.beanote.taste.repository.TasteProfileRepository;

/**
 * 사용자 취향 벡터 계산·저장 (5-2) 과 취향 프로필 화면 데이터 (4-5).
 */
@Service
@Transactional
public class TasteProfileService {

	private static final int TOP = 5;

	private final TastingNoteRepository noteRepository;
	private final TasteSurveyRepository surveyRepository;
	private final FlavorCategoryRepository categoryRepository;
	private final TasteProfileRepository profileRepository;
	private final BeanVectorService beanVectorService;
	private final TasteVectorCalculator calculator;
	private final RecommendationProperties properties;
	private final Clock clock;

	public TasteProfileService(TastingNoteRepository noteRepository, TasteSurveyRepository surveyRepository,
			FlavorCategoryRepository categoryRepository, TasteProfileRepository profileRepository,
			BeanVectorService beanVectorService, TasteVectorCalculator calculator,
			RecommendationProperties properties, Clock clock) {
		this.noteRepository = noteRepository;
		this.surveyRepository = surveyRepository;
		this.categoryRepository = categoryRepository;
		this.profileRepository = profileRepository;
		this.beanVectorService = beanVectorService;
		this.calculator = calculator;
		this.properties = properties;
		this.clock = clock;
	}

	/** 취향 벡터를 다시 계산해 저장한다. 본인 노트·설문이 바뀔 때 호출된다. */
	public TasteResult refresh(Long userId) {
		TasteResult result = compute(userId);
		TasteProfile profile = profileRepository.findById(userId).orElseGet(() -> new TasteProfile(userId));
		profile.update(result.vector(), result.noteCount(), result.confidence());
		profileRepository.save(profile);
		return result;
	}

	/** 저장된 취향을 읽는다. 아직 없으면 계산한다. */
	public TasteResult current(Long userId) {
		return profileRepository.findById(userId)
				.map(p -> new TasteResult(p.vector(), p.getConfidence(), p.getNoteCount(),
						notesWeight(p.getNoteCount(), surveyRepository.existsById(userId))))
				.orElseGet(() -> refresh(userId));
	}

	public TasteResult compute(Long userId) {
		List<TastingNote> notes = noteRepository.findAllByUserId(userId);
		Map<Long, double[]> vectors = beanVectorService
				.loadVectors(notes.stream().map(n -> n.getBean().getId()).collect(Collectors.toSet()));
		List<NoteSignal> signals = notes.stream()
				.map(n -> new NoteSignal(n.getRating().doubleValue(), n.getTastedOn(), vectors.get(n.getBean().getId())))
				.toList();
		SurveySignal survey = surveyRepository.findById(userId).map(this::surveySignal).orElse(null);
		return calculator.calculate(signals, survey, today());
	}

	@Transactional(readOnly = true)
	public TasteProfileResponse getProfile(Long userId) {
		List<TastingNote> notes = noteRepository.findAllByUserId(userId);
		TasteSurvey survey = surveyRepository.findById(userId).orElse(null);
		Map<Long, FlavorCategory> categories = categoryRepository.findAllByOrderBySortOrder().stream()
				.collect(Collectors.toMap(FlavorCategory::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
		Map<TastingNote, Double> weights = centeredWeights(notes);

		RadarResult radar = radar(notes, weights, survey);
		int n = notes.size();
		return new TasteProfileResponse(
				calculator.confidenceOf(n, survey != null),
				n,
				Math.max(0, properties.stableNotes() - n),
				radar.radar(),
				radar.source(),
				flavorScores(weights, true),
				flavorScores(weights, false),
				categoryScores(weights, survey, categories, FlavorPreference.LIKE),
				categoryScores(weights, survey, categories, FlavorPreference.DISLIKE),
				attributeRatings(notes, note -> List.of(note.getBean().getProcess().name()),
						code -> ProcessMethod.valueOf(code).nameKo()),
				attributeRatings(notes, note -> originsOf(note.getBean()),
						code -> OriginCountry.fromCode(code).map(OriginCountry::nameKo).orElse(code)),
				attributeRatings(notes, note -> List.of(note.getBean().getRoastLevel().name()),
						code -> RoastLevel.valueOf(code).nameKo()));
	}

	// ---- 계산 도우미 ----

	private SurveySignal surveySignal(TasteSurvey survey) {
		Map<Long, String> codes = categoryRepository.findAll().stream()
				.collect(Collectors.toMap(FlavorCategory::getId, FlavorCategory::getCode));
		Set<String> liked = survey.getFlavors().entrySet().stream()
				.filter(e -> e.getValue() == FlavorPreference.LIKE)
				.map(e -> codes.get(e.getKey()))
				.collect(Collectors.toSet());
		return new SurveySignal(survey.preferences(), liked, properties.habitRoast().get(survey.getCoffeeHabit()));
	}

	private double notesWeight(int noteCount, boolean hasSurvey) {
		if (noteCount < properties.surveyMinNotes()) {
			return 0;
		}
		if (!hasSurvey || noteCount >= properties.stableNotes()) {
			return 1;
		}
		return (noteCount - properties.surveyMinNotes() + 1)
				/ (double) (properties.stableNotes() - properties.surveyMinNotes() + 1);
	}

	/** 노트별 (평점 − 사용자 평균) × 감쇠. 취향 벡터와 같은 가중치 */
	private Map<TastingNote, Double> centeredWeights(List<TastingNote> notes) {
		double mean = notes.stream().mapToDouble(n -> n.getRating().doubleValue()).average().orElse(0);
		LocalDate today = today();
		Map<TastingNote, Double> weights = new LinkedHashMap<>();
		for (TastingNote note : notes) {
			weights.put(note, (note.getRating().doubleValue() - mean) * decay(note.getTastedOn(), today));
		}
		return weights;
	}

	private RadarResult radar(List<TastingNote> notes, Map<TastingNote, Double> weights, TasteSurvey survey) {
		if (!notes.isEmpty()) {
			// 평균보다 높게 평가한 노트들의 맛 축 경향. 모두 같은 평점이면 전체 평균
			double positive = weights.values().stream().filter(w -> w > 0).mapToDouble(Double::doubleValue).sum();
			double[] sum = new double[5];
			double total = 0;
			for (TastingNote note : notes) {
				double w = positive > 0 ? Math.max(0, weights.get(note)) : 1.0;
				int[] axes = { note.getScores().acidity(), note.getScores().sweetness(), note.getScores().body(),
						note.getScores().bitterness(), note.getScores().aftertaste() };
				for (int i = 0; i < 5; i++) {
					sum[i] += w * axes[i];
				}
				total += w;
			}
			double t = total;
			return new RadarResult(toRadar(i -> sum[i] / t), "NOTES");
		}
		if (survey != null) {
			int[] prefs = survey.preferences();
			return new RadarResult(toRadar(i -> prefs[i]), "SURVEY");
		}
		return new RadarResult(null, "NONE");
	}

	private static List<FlavorScore> flavorScores(Map<TastingNote, Double> weights, boolean liked) {
		Map<FlavorTag, Double> scores = new HashMap<>();
		weights.forEach((note, w) -> note.getFlavors().forEach(tag -> scores.merge(tag, w, Double::sum)));
		return scores.entrySet().stream()
				.filter(e -> liked ? e.getValue() > 0 : e.getValue() < 0)
				.sorted(liked ? Map.Entry.<FlavorTag, Double>comparingByValue().reversed()
						: Map.Entry.comparingByValue())
				.limit(TOP)
				.map(e -> new FlavorScore(e.getKey().getCode(), e.getKey().getNameKo(),
						e.getKey().getCategory().getCode(), round(e.getValue())))
				.toList();
	}

	private static List<CategoryScore> categoryScores(Map<TastingNote, Double> weights, TasteSurvey survey,
			Map<Long, FlavorCategory> categories, FlavorPreference direction) {
		Map<Long, Double> scores = new HashMap<>();
		weights.forEach((note, w) -> note.getFlavors().stream()
				.map(tag -> tag.getCategory().getId())
				.distinct()
				.forEach(categoryId -> scores.merge(categoryId, w, Double::sum)));
		Set<Long> fromSurvey = survey == null ? Set.of()
				: survey.getFlavors().entrySet().stream()
						.filter(e -> e.getValue() == direction)
						.map(Map.Entry::getKey)
						.collect(Collectors.toSet());

		boolean liked = direction == FlavorPreference.LIKE;
		List<CategoryScore> result = new ArrayList<>();
		categories.forEach((id, category) -> {
			double score = scores.getOrDefault(id, 0.0);
			boolean byNotes = liked ? score > 0 : score < 0;
			if (byNotes || fromSurvey.contains(id)) {
				result.add(new CategoryScore(category.getCode(), category.getNameKo(), round(score),
						fromSurvey.contains(id)));
			}
		});
		Comparator<CategoryScore> byScore = Comparator.comparing(CategoryScore::score);
		result.sort(liked ? byScore.reversed() : byScore);
		return result.stream().limit(TOP).toList();
	}

	private static List<AttributeRating> attributeRatings(List<TastingNote> notes,
			Function<TastingNote, List<String>> keys, Function<String, String> names) {
		Map<String, List<Double>> ratings = new LinkedHashMap<>();
		for (TastingNote note : notes) {
			for (String key : keys.apply(note)) {
				ratings.computeIfAbsent(key, k -> new ArrayList<>()).add(note.getRating().doubleValue());
			}
		}
		return ratings.entrySet().stream()
				.map(e -> new AttributeRating(e.getKey(), names.apply(e.getKey()),
						round(e.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0)),
						e.getValue().size()))
				.sorted(Comparator.comparing(AttributeRating::avgRating).reversed()
						.thenComparing(Comparator.comparingInt(AttributeRating::noteCount).reversed()))
				.toList();
	}

	private static List<String> originsOf(Bean bean) {
		return bean.isBlend() ? List.copyOf(bean.getBlendOrigins()) : List.of(bean.getOriginCountry());
	}

	private double decay(LocalDate tastedOn, LocalDate today) {
		long days = Math.max(0, ChronoUnit.DAYS.between(tastedOn, today));
		return Math.pow(properties.decayPerWeek(), days / 7.0);
	}

	private LocalDate today() {
		return LocalDate.now(clock.withZone(ClockConfig.SERVICE_ZONE));
	}

	private static Radar toRadar(IntToDoubleFunction axis) {
		return new Radar(round(axis.applyAsDouble(0)), round(axis.applyAsDouble(1)), round(axis.applyAsDouble(2)),
				round(axis.applyAsDouble(3)), round(axis.applyAsDouble(4)));
	}

	private static BigDecimal round(double value) {
		return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
	}

	private record RadarResult(Radar radar, String source) {
	}

}
