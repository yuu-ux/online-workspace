package com.example.online_workspace.services;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.example.online_workspace.models.WithdrawalAccount;
import com.example.online_workspace.repositories.AccountWithdrawalRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountWithdrawalService {

	private final AccountWithdrawalRepository repository;
	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	public AccountWithdrawalService(AccountWithdrawalRepository repository) {
		this.repository = repository;
	}

	@Transactional
	public void withdraw(String email, String password) {
		WithdrawalAccount account = repository.findActiveByEmail(email)
			.orElseThrow(() -> new ResponseStatusException(
				HttpStatus.CONFLICT,
				"退会できるアカウントがありません"
			));

		if (!passwordMatches(password, account.passwordHash())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "パスワードが正しくありません");
		}

		OffsetDateTime withdrawnAt = OffsetDateTime.now(ZoneOffset.UTC);
		if (repository.markWithdrawn(account.id(), withdrawnAt) != 1) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "アカウントはすでに退会済みです");
		}

		repository.leaveActiveRooms(account.id(), withdrawnAt);
		repository.finishActiveWorkSessions(account.id(), withdrawnAt);
		repository.invalidateRoomInvites(account.id(), withdrawnAt);
	}

	private boolean passwordMatches(String rawPassword, String storedHash) {
		String bcryptHash = storedHash.startsWith("{bcrypt}")
			? storedHash.substring("{bcrypt}".length())
			: storedHash;
		return passwordEncoder.matches(rawPassword, bcryptHash);
	}
}
