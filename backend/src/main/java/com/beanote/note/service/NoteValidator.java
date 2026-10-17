package com.beanote.note.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import com.beanote.flavor.matching.FlavorMatcher;
import com.beanote.global.config.ClockConfig;
import com.beanote.global.error.BusinessException;
import com.beanote.global.error.ErrorCode;
import com.beanote.note.dto.NoteDtos.NoteRequest;

/**
 * 노트 입력 규칙 중 Bean Validation 으로 표현하기 어렵거나 전용 에러 코드가 필요한 것.
 * 향미 태그 개수(최대 5개)는 FlavorService.resolveTags 에서 검증한다.
 */
@Component
public class NoteValidator {

	public static final int MAX_FREE_FLAVORS = 5;

	private static final BigDecimal MIN_RATING = new BigDecimal("0.5");
	private static final BigDecimal MAX_RATING = new BigDecimal("5.0");
	private static final BigDecimal TWO = BigDecimal.valueOf(2);

	private final Clock clock;

	public NoteValidator(Clock clock) {
		this.clock = clock;
	}

	public void validate(NoteRequest request) {
		validateRating(request.rating());
		validateTastedOn(request.tastedOn());
	}

	public void validateRating(BigDecimal rating) {
		boolean inRange = rating.compareTo(MIN_RATING) >= 0 && rating.compareTo(MAX_RATING) <= 0;
		boolean halfStep = rating.multiply(TWO).stripTrailingZeros().scale() <= 0;
		if (!inRange || !halfStep) {
			throw new BusinessException(ErrorCode.RATING_OUT_OF_RANGE);
		}
	}

	public void validateTastedOn(LocalDate tastedOn) {
		// 사용자는 한국 기준 날짜로 입력한다. 시차를 고려해 하루 뒤까지 허용한다
		if (tastedOn.isAfter(LocalDate.now(clock.withZone(ClockConfig.SERVICE_ZONE)).plusDays(1))) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "마신 날짜는 미래일 수 없습니다.");
		}
	}

	public List<String> normalizeFreeFlavors(List<String> freeFlavors) {
		if (freeFlavors == null) {
			return List.of();
		}
		// "자두, 꿀" 처럼 한 칸에 여러 표현을 쓰면 나눠서 각각 저장·표준화한다
		List<String> distinct = freeFlavors.stream()
				.flatMap(text -> FlavorMatcher.splitSegments(text).stream())
				.distinct()
				.toList();
		if (distinct.size() > MAX_FREE_FLAVORS) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "향미 자유 입력은 최대 5개까지 가능합니다.");
		}
		return distinct;
	}

}
