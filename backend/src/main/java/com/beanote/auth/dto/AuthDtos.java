package com.beanote.auth.dto;

import com.beanote.user.dto.UserResponse;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

	private AuthDtos() {
	}

	public record SignupRequest(
			@NotBlank @Email @Size(max = 255) String email,
			@NotBlank @Size(min = 8, max = 64)
			@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "영문과 숫자를 모두 포함해야 합니다.")
			String password,
			@NotBlank @Size(min = 2, max = 20) String nickname) {
	}

	public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
	}

	public record RefreshRequest(@NotBlank String refreshToken) {
	}

	public record LogoutRequest(@NotBlank String refreshToken) {
	}

	public record TokenResponse(String accessToken, long accessTokenExpiresIn, String refreshToken,
			UserResponse user) {
	}

}
