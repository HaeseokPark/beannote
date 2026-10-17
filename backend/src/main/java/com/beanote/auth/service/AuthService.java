package com.beanote.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beanote.auth.domain.RefreshToken;
import com.beanote.auth.dto.AuthDtos.LoginRequest;
import com.beanote.auth.dto.AuthDtos.SignupRequest;
import com.beanote.auth.dto.AuthDtos.TokenResponse;
import com.beanote.auth.repository.RefreshTokenRepository;
import com.beanote.global.error.BusinessException;
import com.beanote.global.error.ErrorCode;
import com.beanote.global.security.JwtProperties;
import com.beanote.global.security.JwtTokenProvider;
import com.beanote.user.domain.User;
import com.beanote.user.dto.UserResponse;
import com.beanote.user.repository.UserRepository;

@Service
@Transactional
public class AuthService {

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final JwtProperties jwtProperties;
	private final LoginAttemptLimiter loginAttemptLimiter;
	private final Clock clock;
	// 존재하지 않는 이메일로 로그인할 때도 BCrypt 비교 시간을 들여 계정 존재 여부가 응답 시간으로 드러나지 않게 한다
	private final String dummyHash;

	public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
			PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider, JwtProperties jwtProperties,
			LoginAttemptLimiter loginAttemptLimiter, Clock clock) {
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenProvider = jwtTokenProvider;
		this.jwtProperties = jwtProperties;
		this.loginAttemptLimiter = loginAttemptLimiter;
		this.clock = clock;
		this.dummyHash = passwordEncoder.encode("beanote-dummy-password");
	}

	public TokenResponse signup(SignupRequest request) {
		String email = normalizeEmail(request.email());
		if (userRepository.existsByEmail(email)) {
			throw new BusinessException(ErrorCode.EMAIL_DUPLICATED);
		}
		User user = userRepository.save(
				User.createLocal(email, passwordEncoder.encode(request.password()), request.nickname().strip()));
		return issueTokens(user);
	}

	public TokenResponse login(LoginRequest request, String clientIp) {
		String email = normalizeEmail(request.email());
		loginAttemptLimiter.recordAttempt(clientIp, email);

		User user = userRepository.findByEmail(email).orElse(null);
		String hash = (user != null && user.getPasswordHash() != null) ? user.getPasswordHash() : dummyHash;
		boolean matches = passwordEncoder.matches(request.password(), hash);
		if (user == null || user.isDeleted() || user.getPasswordHash() == null || !matches) {
			throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
		}
		return issueTokens(user);
	}

	/**
	 * Refresh 토큰 교체(rotation). 이미 폐기된 토큰이 다시 쓰이면 탈취로 보고 그 사용자의 모든 토큰을 폐기한다.
	 */
	@Transactional(noRollbackFor = BusinessException.class)
	public TokenResponse refresh(String rawRefreshToken) {
		Instant now = clock.instant();
		RefreshToken token = refreshTokenRepository.findByTokenHash(RefreshTokens.hash(rawRefreshToken))
				.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

		if (token.isRevoked()) {
			refreshTokenRepository.revokeAllByUserId(token.getUserId(), now);
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		if (token.isExpired(now)) {
			throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
		}

		User user = userRepository.findById(token.getUserId())
				.filter(u -> !u.isDeleted())
				.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
		token.revoke(now);
		return issueTokens(user);
	}

	public void logout(Long userId, String rawRefreshToken) {
		refreshTokenRepository.findByTokenHash(RefreshTokens.hash(rawRefreshToken))
				.filter(token -> token.getUserId().equals(userId))
				.ifPresent(token -> token.revoke(clock.instant()));
	}

	private TokenResponse issueTokens(User user) {
		Instant now = clock.instant();
		String rawRefreshToken = RefreshTokens.generate();
		refreshTokenRepository.save(new RefreshToken(user.getId(), RefreshTokens.hash(rawRefreshToken), now,
				now.plus(jwtProperties.refreshTokenTtl())));
		return new TokenResponse(jwtTokenProvider.issueAccessToken(user), jwtTokenProvider.accessTokenTtlSeconds(),
				rawRefreshToken, UserResponse.from(user));
	}

	private static String normalizeEmail(String email) {
		return email.strip().toLowerCase(Locale.ROOT);
	}

}
