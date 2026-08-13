package com.example.online_workspace.services;

import java.time.OffsetDateTime;
import java.util.Objects;

import com.example.online_workspace.configs.DataRetentionProperties;
import com.example.online_workspace.models.DataRetentionCleanupResult;
import com.example.online_workspace.repositories.DataRetentionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataRetentionCleanupService {

	private static final Logger logger = LoggerFactory.getLogger(DataRetentionCleanupService.class);

	private final DataRetentionRepository repository;
	private final DataRetentionProperties properties;

	public DataRetentionCleanupService(
		DataRetentionRepository repository,
		DataRetentionProperties properties
	) {
		this.repository = repository;
		this.properties = properties;
	}

	@Transactional
	public DataRetentionCleanupResult cleanupExpiredData(OffsetDateTime executionTime) {
		Objects.requireNonNull(executionTime, "executionTime must not be null");

		OffsetDateTime chatHistoryCutoff = executionTime.minusMonths(properties.chatHistoryMonths());
		OffsetDateTime withdrawnWorkHistoryCutoff =
			executionTime.minusDays(properties.withdrawnWorkHistoryDays());

		int deletedMessages = repository.deleteMessagesSentBefore(chatHistoryCutoff);
		int deletedWorkSessions =
			repository.deleteWorkSessionsForUsersWithdrawnBefore(withdrawnWorkHistoryCutoff);

		DataRetentionCleanupResult result = new DataRetentionCleanupResult(
			executionTime,
			chatHistoryCutoff,
			withdrawnWorkHistoryCutoff,
			deletedMessages,
			deletedWorkSessions
		);
		logger.info(
			"Data retention cleanup completed: deletedMessages={}, deletedWorkSessions={}, executionTime={}",
			deletedMessages,
			deletedWorkSessions,
			executionTime
		);
		return result;
	}
}
