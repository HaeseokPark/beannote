package com.beanote.bean.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.beanote.bean.domain.OriginCountry;
import com.beanote.bean.domain.ProcessMethod;
import com.beanote.bean.domain.RoastLevel;
import com.beanote.bean.dto.BeanDtos.BeanDetailResponse;
import com.beanote.bean.dto.BeanDtos.BeanSummaryResponse;
import com.beanote.bean.dto.BeanSearchCondition;
import com.beanote.bean.dto.BeanSort;
import com.beanote.bean.service.BeanService;
import com.beanote.global.response.ApiResponse;
import com.beanote.global.response.CursorPage;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Bean")
@RestController
@RequestMapping("/api/v1/beans")
public class BeanController {

	private final BeanService beanService;

	public BeanController(BeanService beanService) {
		this.beanService = beanService;
	}

	@GetMapping
	public ApiResponse<CursorPage<BeanSummaryResponse>> search(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) OriginCountry origin,
			@RequestParam(required = false) ProcessMethod process,
			@RequestParam(required = false) RoastLevel roastLevel,
			@RequestParam(required = false) String flavorCategory,
			@RequestParam(required = false) BeanSort sort,
			@RequestParam(required = false) String cursor,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.ok(beanService.search(
				new BeanSearchCondition(q, origin, process, roastLevel, flavorCategory, sort, cursor, size)));
	}

	@GetMapping("/{id:\\d+}")
	public ApiResponse<BeanDetailResponse> detail(@PathVariable Long id) {
		return ApiResponse.ok(beanService.getDetail(id));
	}

}
