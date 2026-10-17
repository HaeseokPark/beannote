package com.beanote.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beanote.global.error.BusinessException;
import com.beanote.global.error.ErrorCode;
import com.beanote.user.dto.UserResponse;
import com.beanote.user.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public UserResponse getMe(Long userId) {
		return userRepository.findById(userId)
				.filter(user -> !user.isDeleted())
				.map(UserResponse::from)
				.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
	}

}
