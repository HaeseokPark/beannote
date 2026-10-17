package com.beanote.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.beanote.global.error.BusinessException;
import com.beanote.global.error.ErrorCode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * 로그인 시도 제한 (9-1). 같은 IP, 같은 계정 각각에 대해 window 안에서 maxAttempts 회까지 허용한다.
 * 서버 1대 기준의 로컬 메모리 구현이며, 서버를 늘리면 Redis로 옮긴다.
 */
@Component
public class LoginAttemptLimiter {

	private final int maxAttempts;
	private final Duration window;
	private final Clock clock;
	private final Cache<String, Window> windows;

	public LoginAttemptLimiter(@Value("${beanote.auth.login-limit.max-attempts}") int maxAttempts,
			@Value("${beanote.auth.login-limit.window}") Duration window, Clock clock) {
		this.maxAttempts = maxAttempts;
		this.window = window;
		this.clock = clock;
		this.windows = Caffeine.newBuilder().expireAfterWrite(window.multipliedBy(2)).maximumSize(100_000).build();
	}

	/**
	 * 시도를 1회 기록하고, 한도를 넘었으면 예외를 던진다.
	 */
	public void recordAttempt(String ip, String email) {
		boolean ipExceeded = increment("ip:" + ip) > maxAttempts;
		boolean emailExceeded = increment("email:" + email) > maxAttempts;
		if (ipExceeded || emailExceeded) {
			throw new BusinessException(ErrorCode.TOO_MANY_LOGIN_ATTEMPTS);
		}
	}

	private int increment(String key) {
		Instant now = clock.instant();
		Window updated = windows.asMap().compute(key, (k, current) -> {
			if (current == null || !now.isBefore(current.start().plus(window))) {
				return new Window(now, 1);
			}
			return new Window(current.start(), current.count() + 1);
		});
		return updated.count();
	}

	private record Window(Instant start, int count) {
	}

}
