package com.example.online_workspace;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
public class SwaggerUiSecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests((authorize) -> authorize
				.requestMatchers(
					"/swagger-ui.html",
					"/swagger-ui/**",
					"/openapi.yaml",
					"/v3/api-docs/swagger-config",
					"/api/v1/auth/register",
					"/api/v1/auth/login",
					"/api/v1/auth/session",
					"/api/v1/auth/csrf"
				).permitAll()
				.anyRequest().authenticated()
			)
			// #13でApiErrorレスポンスが実装されるまで、ログイン画面へ
			// リダイレクトせず、本文なしの401を返す暫定設定。
			.exceptionHandling((exceptions) -> exceptions
				.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
			);
		return http.build();
	}
}
