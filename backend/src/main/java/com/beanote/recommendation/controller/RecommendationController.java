package com.beanote.recommendation.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.beanote.global.response.ApiResponse;
import com.beanote.global.security.CurrentUserId;
import com.beanote.recommendation.dto.RecommendationResponse;
import com.beanote.recommendation.dto.RecommendationResponse.FeedbackRequest;
import com.beanote.recommendation.dto.RecommendationResponse.SimilarBean;
import com.beanote.recommendation.service.FeedbackService;
import com.beanote.recommendation.service.RecommendationService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Recommendation")
@RestController
@RequestMapping("/api/v1")
public class RecommendationController {

	private final RecommendationService recommendationService;
	private final FeedbackService feedbackService;

	public RecommendationController(RecommendationService recommendationService, FeedbackService feedbackService) {
		this.recommendationService = recommendationService;
		this.feedbackService = feedbackService;
	}

	/** 추천 목록 + 이유 (R-1, R-2) */
	@GetMapping("/recommendations")
	public ApiResponse<RecommendationResponse> recommendations(@CurrentUserId Long userId) {
		return ApiResponse.ok(recommendationService.recommend(userId));
	}

	/** 관심 없음 / 이미 마셔봄 (R-3) */
	@PostMapping("/recommendations/feedback")
	public ApiResponse<Void> feedback(@CurrentUserId Long userId, @Valid @RequestBody FeedbackRequest request) {
		feedbackService.give(userId, request.beanId(), request.type());
		return ApiResponse.ok();
	}

	/** 이 원두와 비슷한 원두 (R-4). 로그인 없이 볼 수 있다 */
	@GetMapping("/beans/{id:\\d+}/similar")
	public ApiResponse<List<SimilarBean>> similar(@PathVariable Long id) {
		return ApiResponse.ok(recommendationService.similar(id));
	}

}
