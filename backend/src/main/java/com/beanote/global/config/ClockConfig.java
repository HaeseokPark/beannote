package com.beanote.global.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

	/** 사용자가 날짜(마신 날 등)를 입력하는 기준 시간대 */
	public static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}

}
