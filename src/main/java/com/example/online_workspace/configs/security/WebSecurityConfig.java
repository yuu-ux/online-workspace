package com.example.online_workspace.configs.security;

import static java.nio.charset.StandardCharsets.UTF_8;

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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
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
			);

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		DelegatingPasswordEncoder delegate = (DelegatingPasswordEncoder) PasswordEncoderFactories.createDelegatingPasswordEncoder();
		delegate.setDefaultPasswordEncoderForMatches(new BCryptPasswordEncoder());
		return new PasswordEncoder() {
			@Override
			public String encode(CharSequence rawPassword) {
				if (!isWithinBcryptLimit(rawPassword)) {
					throw new IllegalArgumentException("password cannot exceed 72 UTF-8 bytes");
				}
				return delegate.encode(rawPassword);
			}

			@Override
			public boolean matches(CharSequence rawPassword, String encodedPassword) {
				return isWithinBcryptLimit(rawPassword) && delegate.matches(rawPassword, encodedPassword);
			}

			@Override
			public boolean upgradeEncoding(String encodedPassword) {
				return delegate.upgradeEncoding(encodedPassword);
			}

			private boolean isWithinBcryptLimit(CharSequence password) {
				return password != null && password.toString().getBytes(UTF_8).length <= 72;
			}
		};
	}

	@Bean
	public UserDetailsService userDetailsService(AccountWithdrawalRepository repository) {
		return email -> repository.findActiveByEmail(email)
			.map(account -> User.withUsername(email)
				.password(account.passwordHash())
				.roles("USER")
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
