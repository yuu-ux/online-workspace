package com.example.online_workspace.services.auth;

import java.util.Locale;

import com.example.online_workspace.forms.auth.UserRegistrationForm;
import com.example.online_workspace.models.users.UserAccount;
import com.example.online_workspace.repositories.users.UserRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ユーザー登録の業務処理を担当する。
 */
@Service
public class UserRegistrationService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	/**
	 * ユーザー登録サービスを生成する。
	 *
	 * @param userRepository usersテーブルへのアクセス
	 * @param passwordEncoder パスワードのハッシュ化器
	 */
	public UserRegistrationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * 入力値を正規化し、パスワードをハッシュ化してユーザーを登録する。
	 *
	 * @param form ユーザー登録の入力値
	 * @throws DuplicateUserEmailException メールアドレスが登録済みの場合
	 */
	@Transactional
	public void register(UserRegistrationForm form) {
		String email = normalizeEmail(form.email());
		if (userRepository.existsByEmail(email)) {
			throw new DuplicateUserEmailException();
		}

		UserAccount user = new UserAccount(
			form.name().trim(),
			email,
			passwordEncoder.encode(form.password())
		);

		try {
			userRepository.insert(user);
		} catch (DuplicateKeyException exception) {
			// 事前確認後の同時登録でも、DBの一意制約をAPIエラーへ変換する。
			throw new DuplicateUserEmailException();
		}
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
