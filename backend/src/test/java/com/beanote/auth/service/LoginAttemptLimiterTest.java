package com.beanote.auth.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.beanote.global.error.BusinessException;

class LoginAttemptLimiterTest {

	private final MutableClock clock = new MutableClock(Instant.parse("2026-10-17T00:00:00Z"));
	private final LoginAttemptLimiter limiter = new LoginAttemptLimiter(3, Duration.ofMinutes(5), clock);

	@Test
	void blocksAfterMaxAttemptsForSameAccount() {
		for (int i = 0; i < 3; i++) {
			int n = i;
			assertThatCode(() -> limiter.recordAttempt("10.0.0." + n, "a@test.com")).doesNotThrowAnyException();
		}
		assertThatThrownBy(() -> limiter.recordAttempt("10.0.0.9", "a@test.com"))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("로그인 시도가 너무 많습니다");
	}

	@Test
	void blocksAfterMaxAttemptsForSameIp() {
		for (int i = 0; i < 3; i++) {
			int n = i;
			assertThatCode(() -> limiter.recordAttempt("10.0.0.1", "user" + n + "@test.com"))
					.doesNotThrowAnyException();
		}
		assertThatThrownBy(() -> limiter.recordAttempt("10.0.0.1", "other@test.com"))
				.isInstanceOf(BusinessException.class);
	}

	@Test
	void allowsAgainAfterWindowPasses() {
		for (int i = 0; i < 3; i++) {
			limiter.recordAttempt("10.0.0.1", "a@test.com");
		}
		clock.advance(Duration.ofMinutes(5));

		assertThatCode(() -> limiter.recordAttempt("10.0.0.1", "a@test.com")).doesNotThrowAnyException();
	}

	private static final class MutableClock extends Clock {

		private Instant now;

		MutableClock(Instant now) {
			this.now = now;
		}

		void advance(Duration duration) {
			now = now.plus(duration);
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return now;
		}

	}

}
