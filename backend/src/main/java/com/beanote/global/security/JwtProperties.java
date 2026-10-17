package com.beanote.global.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * beanote.jwt.* 설정. secret 은 환경변수(JWT_SECRET)로 주입하고 저장소에 커밋하지 않는다.
 */
@ConfigurationProperties(prefix = "beanote.jwt")
public record JwtProperties(String issuer, String secret, Duration accessTokenTtl, Duration refreshTokenTtl) {

	private static final int MIN_SECRET_BYTES = 32;

	public JwtProperties {
		if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
			throw new IllegalStateException(
					"beanote.jwt.secret(JWT_SECRET) 은 32바이트 이상이어야 합니다. backend/.env.example 을 참고하세요.");
		}
	}

}
