package com.beanote.user.dto;

import com.beanote.user.domain.Role;
import com.beanote.user.domain.User;

public record UserResponse(Long id, String email, String nickname, String profileImageUrl, Role role) {

	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getEmail(), user.getNickname(), user.getProfileImageUrl(),
				user.getRole());
	}

}
