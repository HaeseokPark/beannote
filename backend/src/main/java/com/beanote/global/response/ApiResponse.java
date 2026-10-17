package com.beanote.global.response;

/**
 * 모든 API의 공통 응답 형식. { "success", "data", "error" }
 */
public record ApiResponse<T>(boolean success, T data, ErrorBody error) {

	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(true, data, null);
	}

	public static ApiResponse<Void> ok() {
		return new ApiResponse<>(true, null, null);
	}

	public static ApiResponse<Void> fail(String code, String message) {
		return new ApiResponse<>(false, null, new ErrorBody(code, message));
	}

	public record ErrorBody(String code, String message) {
	}

}
