package com.beanote.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.beanote.auth.dto.AuthDtos.LoginRequest;
import com.beanote.auth.dto.AuthDtos.LogoutRequest;
import com.beanote.auth.dto.AuthDtos.RefreshRequest;
import com.beanote.auth.dto.AuthDtos.SignupRequest;
import com.beanote.auth.dto.AuthDtos.TokenResponse;
import com.beanote.auth.service.AuthService;
import com.beanote.global.response.ApiResponse;
import com.beanote.global.security.CurrentUserId;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<TokenResponse> signup(@Valid @RequestBody SignupRequest request) {
		return ApiResponse.ok(authService.signup(request));
	}

	@PostMapping("/login")
	public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
		return ApiResponse.ok(authService.login(request, http.getRemoteAddr()));
	}

	@PostMapping("/refresh")
	public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
		return ApiResponse.ok(authService.refresh(request.refreshToken()));
	}

	@PostMapping("/logout")
	public ApiResponse<Void> logout(@CurrentUserId Long userId, @Valid @RequestBody LogoutRequest request) {
		authService.logout(userId, request.refreshToken());
		return ApiResponse.ok();
	}

}
