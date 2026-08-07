package com.example.online_workspace.configs.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
public class SwaggerUiSecurityConfig {

	@Bean
	@Order(1)
	public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
		http
			.securityMatcher("/api/v1/**")
			.csrf(csrf -> csrf
				.csrfTokenRepository(
					CookieCsrfTokenRepository.withHttpOnlyFalse()
				)
			)
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(
					"/api/v1/auth/register",
					"/api/v1/auth/login",
					"/api/v1/auth/session",
					"/api/v1/auth/csrf"
				).permitAll()
				.anyRequest().authenticated()
			)
			.exceptionHandling(exceptions -> exceptions
				.authenticationEntryPoint(
					new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
				)
			);

		return http.build();
	}

	@Bean
	@Order(2)
	public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests((authorize) -> authorize
				.requestMatchers(
					"/swagger-ui.html",
					"/swagger-ui/**",
					"/openapi.yaml",
					"/v3/api-docs/swagger-config"
				).permitAll()
				// API以外はデフォルトで認証必須。
				// 将来、未認証アクセスを許可するエンドポイントを追加する場合は
				// anyRequest() より前に個別ルールを追加する。
				.anyRequest().authenticated()
			)
			.exceptionHandling((exceptions) -> exceptions
				.authenticationEntryPoint(
					new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
			);

		return http.build();
	}
}
