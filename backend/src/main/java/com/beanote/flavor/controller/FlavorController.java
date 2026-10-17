package com.beanote.flavor.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.beanote.flavor.dto.FlavorDtos.CategoryResponse;
import com.beanote.flavor.service.FlavorService;
import com.beanote.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Flavor")
@RestController
@RequestMapping("/api/v1/flavors")
public class FlavorController {

	private final FlavorService flavorService;

	public FlavorController(FlavorService flavorService) {
		this.flavorService = flavorService;
	}

	@GetMapping
	public ApiResponse<List<CategoryResponse>> getAll() {
		return ApiResponse.ok(flavorService.getAll());
	}

}
