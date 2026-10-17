package com.beanote.user.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.beanote.global.response.ApiResponse;
import com.beanote.global.security.CurrentUserId;
import com.beanote.user.dto.UserResponse;
import com.beanote.user.service.UserService;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/me")
	public ApiResponse<UserResponse> me(@CurrentUserId Long userId) {
		return ApiResponse.ok(userService.getMe(userId));
	}

}
