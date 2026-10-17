package com.beanote.flavor.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.beanote.flavor.dto.FlavorDtos.CategoryResponse;
import com.beanote.flavor.dto.NormalizeDtos.MatchResponse;
import com.beanote.flavor.dto.NormalizeDtos.NormalizeRequest;
import com.beanote.flavor.service.FlavorNormalizationService;
import com.beanote.flavor.service.FlavorService;
import com.beanote.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Flavor")
@RestController
@RequestMapping("/api/v1/flavors")
public class FlavorController {

	private final FlavorService flavorService;
	private final FlavorNormalizationService normalizationService;

	public FlavorController(FlavorService flavorService, FlavorNormalizationService normalizationService) {
		this.flavorService = flavorService;
		this.normalizationService = normalizationService;
	}

	@GetMapping
	public ApiResponse<List<CategoryResponse>> getAll() {
		return ApiResponse.ok(flavorService.getAll());
	}

	/**
	 * 자유 표현 → 표준 태그 후보 (F-2, F-3). 저장하지 않고 판정 결과만 돌려준다.
	 * 예: "자스민향, 살짝 자두 같은 느낌" → [자스민(AUTO), 자두(AUTO)]
	 */
	@PostMapping("/normalize")
	public ApiResponse<List<MatchResponse>> normalize(@Valid @RequestBody NormalizeRequest request) {
		return ApiResponse.ok(normalizationService.preview(request.text()));
	}

}
