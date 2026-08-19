package com.example.online_workspace.configs.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Web画面とSwagger UI向けの認証設定を構成する。
 */
@Configuration
public class WebSecurityConfig {

	/**
	 * Web用のSecurityFilterChainを生成する。
	 *
	 * @param http HTTPセキュリティの設定オブジェクト
	 * @param securityContextRepository セキュリティコンテキストの保存先
	 * @return Web用のSecurityFilterChain
	 * @throws Exception セキュリティ設定に失敗した場合
	 */
	@Bean
	@Order(2)
	public SecurityFilterChain webSecurityFilterChain(
		HttpSecurity http,
		SecurityContextRepository securityContextRepository
	) throws Exception {
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
			.securityContext(securityContext -> securityContext
				.securityContextRepository(securityContextRepository)
			)
			// React側のログイン画面が実装されるまでは、Swagger UIなどのWeb利用向けに
			// Spring Securityのログイン画面を維持する。React画面統合時にformLoginを削除する。
			.formLogin(Customizer.withDefaults());

		return http.build();
	}
}
