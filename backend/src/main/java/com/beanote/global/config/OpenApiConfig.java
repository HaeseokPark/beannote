package com.beanote.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI beanoteOpenApi() {
		return new OpenAPI().info(new Info()
				.title("Beanote API")
				.description("원두 취향 추천 + 시음 노트")
				.version("v1"));
	}

}
