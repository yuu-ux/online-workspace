package com.example.online_workspace.services.auth;

import com.example.online_workspace.models.users.UserAuthentication;
import com.example.online_workspace.repositories.users.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * ユーザーログインの業務処理を担当する。
 */
@Service
public class UserLoginService {

	private static final String DUMMY_PASSWORD_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserLoginService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * 認証情報を照合する。
	 *
	 * @param email 正規化済みメールアドレス
	 * @param password 入力されたパスワード
	 * @return 認証済みユーザー情報
	 * @throws InvalidLoginCredentialsException 認証情報が不正な場合
	 */
	public UserAuthentication authenticate(String email, String password) {
		UserAuthentication user = userRepository.findByEmail(email);
		String passwordHash = user == null ? DUMMY_PASSWORD_HASH : user.passwordHash();
		boolean passwordMatches = passwordEncoder.matches(password, passwordHash);
		if (!passwordMatches || user == null || !"ACTIVE".equals(user.accountStatus())) {
			throw new InvalidLoginCredentialsException();
		}
		return user;
	}
}
