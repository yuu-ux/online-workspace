package com.example.online_workspace.configs.security;

import com.example.online_workspace.exceptions.ApiErrorWriter;
import com.example.online_workspace.repositories.users.UserRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * API向けの認証・認可とCSRF保護を構成する。
 */
@Configuration
public class ApiSecurityConfig {

	@Bean
	SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

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
		SecurityContextRepository securityContextRepository,
		UserRepository userRepository,
		@Value("${app.security.api-key.value:}") String apiKey,
		@Value("${app.security.api-key.principal:}") String apiKeyPrincipal,
		@Value("${app.security.rate-limit.requests-per-minute:60}") int requestsPerMinute
	) throws Exception {
		ApiKeyAuthenticationFilter apiKeyAuthenticationFilter =
			new ApiKeyAuthenticationFilter(apiKey, apiKeyPrincipal, apiErrorWriter);
		ApiRateLimitFilter apiRateLimitFilter = new ApiRateLimitFilter(requestsPerMinute, apiErrorWriter);
		ActiveUserAuthenticationFilter activeUserAuthenticationFilter =
			new ActiveUserAuthenticationFilter(userRepository, securityContextRepository);
		CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		csrfTokenRepository.setHeaderName("X-CSRF-TOKEN");
		csrfTokenRepository.setCookieCustomizer(cookie -> cookie
			.secure(true)
			.sameSite("Lax")
		);
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
			.logout(logout -> logout
				.logoutRequestMatcher(authenticatedLogoutRequest())
				.deleteCookies("JSESSIONID")
				.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
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
			)
			.addFilterBefore(new SecurityAuditFilter("api"), CsrfFilter.class)
			.addFilterBefore(apiKeyAuthenticationFilter, CsrfFilter.class)
			.addFilterAfter(activeUserAuthenticationFilter, ApiKeyAuthenticationFilter.class)
			.addFilterAfter(apiRateLimitFilter, ApiKeyAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	public SessionAuthenticationStrategy sessionAuthenticationStrategy(SessionRegistry sessionRegistry) {
		return new CompositeSessionAuthenticationStrategy(List.of(
			new ChangeSessionIdAuthenticationStrategy(),
			new RegisterSessionAuthenticationStrategy(sessionRegistry)
		));
	}

	private RequestMatcher authenticatedLogoutRequest() {
		return request -> {
			String path = request.getRequestURI().substring(request.getContextPath().length());
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			return "POST".equals(request.getMethod())
				&& "/api/v1/auth/logout".equals(path)
				&& authentication != null
				&& authentication.isAuthenticated()
				&& !(authentication instanceof AnonymousAuthenticationToken);
		};
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
