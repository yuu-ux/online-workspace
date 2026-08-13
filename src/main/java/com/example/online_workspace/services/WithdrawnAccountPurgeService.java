package com.example.online_workspace.services;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.example.online_workspace.repositories.AccountWithdrawalRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WithdrawnAccountPurgeService {

	private final AccountWithdrawalRepository repository;
	private final long retentionDays;

	public WithdrawnAccountPurgeService(
		AccountWithdrawalRepository repository,
		@Value("${app.account-withdrawal.retention-days:30}") long retentionDays
	) {
		if (retentionDays < 1) {
			throw new IllegalArgumentException("retentionDays must be positive");
		}
		this.repository = repository;
		this.retentionDays = retentionDays;
	}

	@Scheduled(cron = "${app.account-withdrawal.purge-cron:0 0 3 * * *}", zone = "UTC")
	@Transactional
	public int purgeExpiredAccounts() {
		return purgeExpiredAccounts(OffsetDateTime.now(ZoneOffset.UTC));
	}

	@Transactional
	public int purgeExpiredAccounts(OffsetDateTime executionTime) {
		OffsetDateTime cutoff = executionTime.minusDays(retentionDays);
		List<Long> accountIds = repository.findAccountsReadyForPurge(cutoff);

		for (long accountId : accountIds) {
			repository.deleteWorkSessions(accountId);
			repository.deleteProfile(accountId);
			repository.deleteRoomMembershipHistory(accountId);
			repository.deleteRoomInvites(accountId);
			repository.deleteFriendRelationships(accountId);
			repository.deleteBlockRelationships(accountId);
			repository.anonymizeAccount(accountId, executionTime);
		}

		return accountIds.size();
	}
}
