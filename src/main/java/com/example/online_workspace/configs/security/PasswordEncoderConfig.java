package com.example.online_workspace.configs.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * パスワードのハッシュ化方式を構成する。
 */
@Configuration
public class PasswordEncoderConfig {

	/**
	 * BCryptを使うパスワードエンコーダーを生成する。
	 *
	 * @return BCryptを使うパスワードエンコーダー
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
