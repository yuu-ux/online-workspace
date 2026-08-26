package com.example.online_workspace.configs.security;

import static java.nio.charset.StandardCharsets.UTF_8;

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
	 * BCryptを使い、入力をUTF-8で72バイト以内に制限するパスワードエンコーダーを生成する。
	 *
	 * @return BCryptを使うパスワードエンコーダー
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		BCryptPasswordEncoder delegate = new BCryptPasswordEncoder();
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
}
