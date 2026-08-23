package com.example.online_workspace.configs.security;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
public class OperationsSecurityConfig {

	@Bean
	@Order(0)
	public SecurityFilterChain operationsSecurityFilterChain(
		HttpSecurity http,
		@Value("${app.management.api-key:}") String apiKey
	) throws Exception {
		RequestMatcher validApiKey = request -> {
			String suppliedKey = request.getHeader("X-API-Key");
			return !apiKey.isBlank()
				&& suppliedKey != null
				&& MessageDigest.isEqual(apiKey.getBytes(UTF_8), suppliedKey.getBytes(UTF_8));
		};

		http
			.securityMatcher("/health", "/metrics")
			.csrf(csrf -> csrf.disable())
			.requestCache(cache -> cache.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(validApiKey).permitAll()
				.anyRequest().denyAll()
			)
			.exceptionHandling(exceptions -> exceptions
				.authenticationEntryPoint((request, response, exception) -> response.sendError(401))
				.accessDeniedHandler((request, response, exception) -> response.sendError(401))
			);

		return http.build();
	}
}
