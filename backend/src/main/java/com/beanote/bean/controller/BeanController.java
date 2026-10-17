package com.beanote.bean.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.beanote.bean.domain.OriginCountry;
import com.beanote.bean.domain.ProcessMethod;
import com.beanote.bean.domain.RoastLevel;
import com.beanote.bean.dto.BeanDtos.BeanCreateRequest;
import com.beanote.bean.dto.BeanDtos.BeanDetailResponse;
import com.beanote.bean.dto.BeanDtos.BeanSummaryResponse;
import com.beanote.bean.dto.BeanDtos.DuplicateCandidate;
import com.beanote.bean.dto.BeanSearchCondition;
import com.beanote.bean.dto.BeanSort;
import com.beanote.bean.service.BeanService;
import com.beanote.global.response.ApiResponse;
import com.beanote.global.response.CursorPage;
import com.beanote.global.security.CurrentUserId;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

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

	/**
	 * 원두 등록 화면에서 이름을 입력할 때 보여 줄 중복 후보.
	 */
	@GetMapping("/duplicates")
	public ApiResponse<List<DuplicateCandidate>> duplicates(
			@RequestParam(required = false) Long roasteryId,
			@RequestParam(required = false) String roasteryName,
			@RequestParam String name) {
		return ApiResponse.ok(beanService.findDuplicates(roasteryId, roasteryName, name));
	}

	/**
	 * 사용자 원두 등록. 비슷한 원두가 있으면 BEAN_DUPLICATE_SUSPECTED, force=true 면 그대로 등록한다.
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<BeanDetailResponse> register(@CurrentUserId Long userId,
			@Valid @RequestBody BeanCreateRequest request) {
		Long beanId = beanService.register(userId, request);
		return ApiResponse.ok(beanService.getDetail(beanId));
	}

}
