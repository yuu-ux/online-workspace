package com.example.online_workspace.configs.security;

import com.example.online_workspace.exceptions.ApiErrorWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * API向けの認証・認可とCSRF保護を構成する。
 */
@Configuration
public class ApiSecurityConfig {

	/**
	 * API用のSecurityFilterChainを生成する。
	 *
	 * @param http HTTPセキュリティの設定オブジェクト
	 * @param apiErrorWriter APIエラーレスポンスの出力処理
	 * @return API用のSecurityFilterChain
	 * @throws Exception セキュリティ設定に失敗した場合
	 */
	@Bean
	@Order(1)
	public SecurityFilterChain apiSecurityFilterChain(
		HttpSecurity http,
		ApiErrorWriter apiErrorWriter,
		@Value("${app.security.api-key.value:}") String apiKey,
		@Value("${app.security.api-key.principal:}") String apiKeyPrincipal,
		@Value("${app.security.rate-limit.requests-per-minute:60}") int requestsPerMinute
	) throws Exception {
		ApiKeyAuthenticationFilter apiKeyAuthenticationFilter =
			new ApiKeyAuthenticationFilter(apiKey, apiKeyPrincipal, apiErrorWriter);
		ApiRateLimitFilter apiRateLimitFilter = new ApiRateLimitFilter(requestsPerMinute, apiErrorWriter);
		CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		csrfTokenRepository.setHeaderName("X-CSRF-TOKEN");
		CsrfTokenRequestAttributeHandler csrfRequestHandler = new CsrfTokenRequestAttributeHandler();

		http
			.securityMatcher("/api/v1/**")
			.csrf(csrf -> csrf
				.csrfTokenRepository(csrfTokenRepository)
				.csrfTokenRequestHandler(csrfRequestHandler)
				.ignoringRequestMatchers(apiKeyAuthenticationFilter::hasValidApiKey)
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
				.authenticationEntryPoint((request, response, exception) -> apiErrorWriter.write(
					request,
					response,
					HttpStatus.UNAUTHORIZED,
					"UNAUTHORIZED",
					"認証が必要です。"
				))
				.accessDeniedHandler((request, response, exception) -> apiErrorWriter.write(
					request,
					response,
					HttpStatus.FORBIDDEN,
					"FORBIDDEN",
					"この操作を行う権限がありません。"
				))
			)
			.addFilterBefore(apiKeyAuthenticationFilter, CsrfFilter.class)
			.addFilterAfter(apiRateLimitFilter, ApiKeyAuthenticationFilter.class);

		return http.build();
	}
}
