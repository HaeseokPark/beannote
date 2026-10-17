package com.beanote.roastery.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.beanote.global.response.ApiResponse;
import com.beanote.roastery.dto.RoasteryResponse;
import com.beanote.roastery.service.RoasteryService;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Roastery")
@RestController
@RequestMapping("/api/v1/roasteries")
public class RoasteryController {

	private final RoasteryService roasteryService;

	public RoasteryController(RoasteryService roasteryService) {
		this.roasteryService = roasteryService;
	}

	@GetMapping
	public ApiResponse<List<RoasteryResponse>> list(@RequestParam(required = false) String q) {
		return ApiResponse.ok(roasteryService.search(q));
	}

}
