package com.beanote.global.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, ApiSecurityErrorHandler errorHandler)
			throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.httpBasic(basic -> basic.disable())
				.formLogin(form -> form.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/signup", "/api/v1/auth/login",
								"/api/v1/auth/refresh").permitAll()
						// 아래 공개 GET 규칙보다 먼저 와야 하는 로그인 필요 경로
						.requestMatchers(HttpMethod.GET, "/api/v1/beans/duplicates", "/api/v1/notes/me")
						.authenticated()
						.requestMatchers(HttpMethod.GET, "/api/v1/flavors", "/api/v1/beans", "/api/v1/beans/*",
								"/api/v1/beans/*/similar", "/api/v1/roasteries", "/api/v1/notes/*").permitAll()
						.requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
								"/error").permitAll()
						.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
						.authenticationEntryPoint(errorHandler)
						.accessDeniedHandler(errorHandler))
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint(errorHandler)
						.accessDeniedHandler(errorHandler));
		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	private JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
		authorities.setAuthoritiesClaimName(JwtTokenProvider.ROLE_CLAIM);
		authorities.setAuthorityPrefix("ROLE_");
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authorities);
		return converter;
	}

}
