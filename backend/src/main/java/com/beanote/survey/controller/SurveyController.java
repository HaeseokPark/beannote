package com.beanote.survey.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.beanote.global.response.ApiResponse;
import com.beanote.global.security.CurrentUserId;
import com.beanote.survey.dto.SurveyDtos.SurveyRequest;
import com.beanote.survey.dto.SurveyDtos.SurveyResponse;
import com.beanote.survey.service.SurveyService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Survey")
@RestController
@RequestMapping("/api/v1/survey")
public class SurveyController {

	private final SurveyService surveyService;

	public SurveyController(SurveyService surveyService) {
		this.surveyService = surveyService;
	}

	/** 내 설문. 아직 하지 않았거나 건너뛰었으면 data 가 null 이다. */
	@GetMapping
	public ApiResponse<SurveyResponse> get(@CurrentUserId Long userId) {
		return ApiResponse.ok(surveyService.get(userId));
	}

	/** 설문 저장·다시 하기 (O-1 ~ O-3, O-5) */
	@PutMapping
	public ApiResponse<SurveyResponse> save(@CurrentUserId Long userId, @Valid @RequestBody SurveyRequest request) {
		return ApiResponse.ok(surveyService.save(userId, request));
	}

}
