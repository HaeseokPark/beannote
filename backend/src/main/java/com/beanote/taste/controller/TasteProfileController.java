package com.beanote.taste.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.beanote.global.response.ApiResponse;
import com.beanote.global.security.CurrentUserId;
import com.beanote.taste.dto.TasteProfileResponse;
import com.beanote.taste.service.TasteProfileService;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Taste Profile")
@RestController
@RequestMapping("/api/v1/taste-profile")
public class TasteProfileController {

	private final TasteProfileService tasteProfileService;

	public TasteProfileController(TasteProfileService tasteProfileService) {
		this.tasteProfileService = tasteProfileService;
	}

	@GetMapping("/me")
	public ApiResponse<TasteProfileResponse> me(@CurrentUserId Long userId) {
		return ApiResponse.ok(tasteProfileService.getProfile(userId));
	}

}
