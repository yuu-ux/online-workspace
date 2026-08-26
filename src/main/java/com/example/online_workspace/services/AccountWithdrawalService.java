package com.example.online_workspace.services;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.time.OffsetDateTime;

import com.example.online_workspace.models.WithdrawalAccount;
import com.example.online_workspace.repositories.AccountWithdrawalRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountWithdrawalService {

	private final AccountWithdrawalRepository repository;
	private final PasswordEncoder passwordEncoder;

	public AccountWithdrawalService(AccountWithdrawalRepository repository, PasswordEncoder passwordEncoder) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public void withdraw(String email, String password) {
		WithdrawalAccount account = repository.findWithdrawableByEmail(email)
			.orElseThrow(() -> new ResponseStatusException(
				HttpStatus.CONFLICT,
				"退会できるアカウントがありません"
			));

		if (password.getBytes(UTF_8).length > 72) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "パスワードはUTF-8で72バイト以内で入力してください");
		}

		if (!passwordEncoder.matches(password, account.passwordHash())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "パスワードが正しくありません");
		}

		OffsetDateTime withdrawnAt = repository.currentTimestamp();
		if (repository.markWithdrawn(account.id(), withdrawnAt) != 1) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "アカウントはすでに退会済みです");
		}

		repository.leaveActiveRooms(account.id(), withdrawnAt);
	}
}
