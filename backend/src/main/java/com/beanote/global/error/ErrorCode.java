package com.beanote.global.error;

import org.springframework.http.HttpStatus;

/**
 * API 에러 코드. 도메인 에러 코드(BEAN_NOT_FOUND 등)는 해당 Phase에서 추가한다.
 */
public enum ErrorCode {

	INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
	NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식입니다."),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

	// 인증
	EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
	TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
	TOO_MANY_LOGIN_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해 주세요."),

	// 원두
	BEAN_NOT_FOUND(HttpStatus.NOT_FOUND, "원두를 찾을 수 없습니다."),
	BEAN_DUPLICATE_SUSPECTED(HttpStatus.CONFLICT, "비슷한 원두가 이미 있습니다. 확인 후 그래도 등록하려면 force=true 로 다시 요청하세요."),

	// 노트
	NOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "노트를 찾을 수 없습니다."),
	FLAVOR_TAG_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "향미 태그는 최대 5개까지 선택할 수 있습니다."),
	RATING_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "별점은 0.5~5.0 사이에서 0.5 단위로 입력해 주세요.");

	private final HttpStatus status;
	private final String message;

	ErrorCode(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
	}

	public HttpStatus status() {
		return status;
	}

	public String message() {
		return message;
	}

}
