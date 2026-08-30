package com.example.online_workspace.configs.security;

import com.example.online_workspace.repositories.AccountWithdrawalRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;

/**
 * Web画面とSwagger UI向けの認証設定を構成する。
 */
@Configuration
public class WebSecurityConfig {

	/**
	 * Web用のSecurityFilterChainを生成する。
	 *
	 * @param http HTTPセキュリティの設定オブジェクト
	 * @return Web用のSecurityFilterChain
	 * @throws Exception セキュリティ設定に失敗した場合
	 */
	@Bean
	@Order(2)
	public SecurityFilterChain webSecurityFilterChain(HttpSecurity http, SessionRegistry sessionRegistry) throws Exception {
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
			// React側のログイン画面とログインAPIが実装されるまでは、
			// Swagger UIなどのWeb利用向けにSpring Securityのログイン画面を維持する。
			// ログイン機能のReact移行時にformLoginを削除する。
			.formLogin(Customizer.withDefaults())
			.sessionManagement(session -> session
				.maximumSessions(-1)
				.sessionRegistry(sessionRegistry)
			)
			.addFilterBefore(new SecurityAuditFilter("web"), CsrfFilter.class);

		return http.build();
	}

	@Bean
	public UserDetailsService userDetailsService(AccountWithdrawalRepository repository) {
		return email -> repository.findActiveByEmail(email)
			.map(account -> User.withUsername(email)
				.password(account.passwordHash())
				.authorities(new String[0])
				.build())
			.orElseThrow(() -> new UsernameNotFoundException("有効なアカウントがありません"));
	}

	@Bean
	public SessionRegistry sessionRegistry() {
		return new SessionRegistryImpl();
	}

	@Bean
	public HttpSessionEventPublisher httpSessionEventPublisher() {
		return new HttpSessionEventPublisher();
	}
}
