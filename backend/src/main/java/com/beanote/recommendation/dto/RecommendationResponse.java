package com.beanote.recommendation.dto;

import java.util.List;

import com.beanote.bean.dto.BeanDtos.BeanSummaryResponse;
import com.beanote.recommendation.domain.FeedbackType;
import com.beanote.recommendation.model.Reason;
import com.beanote.taste.model.TasteVectorCalculator.Confidence;

import jakarta.validation.constraints.NotNull;

/**
 * GET /recommendations (7-4).
 *
 * @param popular 취향 정보가 없어 인기순으로 추천했는지 (O-4)
 */
public record RecommendationResponse(Confidence confidence, int basedOnNotes, boolean popular, List<Item> items) {

	/**
	 * @param score 최종 점수. 인기순 추천이면 null
	 */
	public record Item(BeanSummaryResponse bean, Double score, List<Reason> reasons, boolean isExploration) {
	}

	public record FeedbackRequest(@NotNull Long beanId, @NotNull FeedbackType type) {
	}

	public record SimilarBean(BeanSummaryResponse bean, double similarity) {
	}

}
