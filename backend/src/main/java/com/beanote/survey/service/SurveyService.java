package com.beanote.survey.service;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beanote.flavor.domain.FlavorCategory;
import com.beanote.flavor.repository.FlavorCategoryRepository;
import com.beanote.global.error.BusinessException;
import com.beanote.global.error.ErrorCode;
import com.beanote.survey.domain.FlavorPreference;
import com.beanote.survey.domain.TasteSurvey;
import com.beanote.survey.dto.SurveyDtos.CategoryLabel;
import com.beanote.survey.dto.SurveyDtos.Preferences;
import com.beanote.survey.dto.SurveyDtos.SurveyRequest;
import com.beanote.survey.dto.SurveyDtos.SurveyResponse;
import com.beanote.survey.repository.TasteSurveyRepository;
import com.beanote.taste.event.TasteInputChangedEvent;

/**
 * 온보딩 취향 설문 (4-2). 설문을 건너뛴 사용자(O-4)는 설문이 없고, 노트가 쌓이기 전까지 인기순 추천을 받는다.
 */
@Service
@Transactional
public class SurveyService {

	private final TasteSurveyRepository surveyRepository;
	private final FlavorCategoryRepository categoryRepository;
	private final ApplicationEventPublisher events;

	public SurveyService(TasteSurveyRepository surveyRepository, FlavorCategoryRepository categoryRepository,
			ApplicationEventPublisher events) {
		this.surveyRepository = surveyRepository;
		this.categoryRepository = categoryRepository;
		this.events = events;
	}

	@Transactional(readOnly = true)
	public SurveyResponse get(Long userId) {
		return surveyRepository.findById(userId).map(this::toResponse).orElse(null);
	}

	public SurveyResponse save(Long userId, SurveyRequest request) {
		Map<String, FlavorCategory> byCode = categoryRepository.findAll().stream()
				.collect(Collectors.toMap(FlavorCategory::getCode, Function.identity()));
		Set<String> liked = new HashSet<>(nullToEmpty(request.likedCategories()));
		Set<String> disliked = new HashSet<>(nullToEmpty(request.dislikedCategories()));
		if (liked.stream().anyMatch(disliked::contains)) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "같은 향을 좋아하면서 싫어할 수는 없어요.");
		}

		Map<Long, FlavorPreference> flavors = new LinkedHashMap<>();
		putAll(flavors, liked, byCode, FlavorPreference.LIKE);
		putAll(flavors, disliked, byCode, FlavorPreference.DISLIKE);

		TasteSurvey survey = surveyRepository.findById(userId).orElseGet(() -> new TasteSurvey(userId));
		survey.update(request.coffeeHabit(), request.preferences().toArray(), flavors);
		surveyRepository.save(survey);

		events.publishEvent(TasteInputChangedEvent.ofUser(userId));
		return toResponse(survey);
	}

	private static void putAll(Map<Long, FlavorPreference> target, Set<String> codes,
			Map<String, FlavorCategory> byCode, FlavorPreference preference) {
		for (String code : codes) {
			FlavorCategory category = byCode.get(code.strip());
			if (category == null) {
				throw new BusinessException(ErrorCode.INVALID_INPUT, "알 수 없는 향미 대분류입니다: " + code);
			}
			target.put(category.getId(), preference);
		}
	}

	private SurveyResponse toResponse(TasteSurvey survey) {
		Map<Long, FlavorCategory> byId = categoryRepository.findAllByOrderBySortOrder().stream()
				.collect(Collectors.toMap(FlavorCategory::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
		return new SurveyResponse(survey.getCoffeeHabit(), Preferences.of(survey.preferences()),
				labels(survey, byId, FlavorPreference.LIKE), labels(survey, byId, FlavorPreference.DISLIKE));
	}

	private static List<CategoryLabel> labels(TasteSurvey survey, Map<Long, FlavorCategory> byId,
			FlavorPreference preference) {
		return byId.values().stream()
				.filter(c -> survey.getFlavors().get(c.getId()) == preference)
				.map(c -> new CategoryLabel(c.getCode(), c.getNameKo()))
				.toList();
	}

	private static List<String> nullToEmpty(List<String> list) {
		return list == null ? List.of() : list;
	}

}
