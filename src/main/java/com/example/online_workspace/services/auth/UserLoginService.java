package com.example.online_workspace.services.auth;

import com.example.online_workspace.exceptions.InvalidLoginCredentialsException;
import com.example.online_workspace.models.users.UserAuthentication;
import com.example.online_workspace.repositories.users.UserRepository;
import java.time.Instant;
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

	public UserAuthentication authenticate(String email, String password) {
		UserAuthentication user = userRepository.findAuthenticationByEmail(email);
		String passwordHash = user == null ? DUMMY_PASSWORD_HASH : user.passwordHash();
		boolean unavailable = user == null
			|| !"ACTIVE".equals(user.accountStatus())
			|| user.suspendedUntil() != null && user.suspendedUntil().isAfter(Instant.now());
		if (!passwordEncoder.matches(password, passwordHash) || unavailable) {
			throw new InvalidLoginCredentialsException();
		}
		return user;
	}
}
