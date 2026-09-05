package com.example.online_workspace.configs.security;

import com.example.online_workspace.exceptions.ApiErrorWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;

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
	 * @param securityContextRepository セキュリティコンテキストの保存先
	 * @return API用のSecurityFilterChain
	 * @throws Exception セキュリティ設定に失敗した場合
	 */
	@Bean
	@Order(1)
	public SecurityFilterChain apiSecurityFilterChain(
		HttpSecurity http,
		ApiErrorWriter apiErrorWriter,
		SecurityContextRepository securityContextRepository
	) throws Exception {
		CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		csrfTokenRepository.setHeaderName("X-CSRF-TOKEN");
		CsrfTokenRequestAttributeHandler csrfRequestHandler = new CsrfTokenRequestAttributeHandler();

		http
			.securityMatcher("/api/v1/**")
			.csrf(csrf -> csrf
				.csrfTokenRepository(csrfTokenRepository)
				.csrfTokenRequestHandler(csrfRequestHandler)
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
			.securityContext(securityContext -> securityContext
				.securityContextRepository(securityContextRepository)
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
			);

		return http.build();
	}

	@Bean
	public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
		return new ChangeSessionIdAuthenticationStrategy();
	}

	/**
	 * API認証で発生する成功・失敗イベントの発行元を構成する。
	 *
	 * @param applicationEventPublisher Springのアプリケーションイベント発行元
	 * @return Spring Securityの認証イベント発行元
	 */
	@Bean
	public AuthenticationEventPublisher authenticationEventPublisher(
		ApplicationEventPublisher applicationEventPublisher
	) {
		return new DefaultAuthenticationEventPublisher(applicationEventPublisher);
	}
}
