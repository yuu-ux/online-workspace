package com.example.online_workspace.services;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
	name = "app.data-retention.scheduling-enabled",
	havingValue = "true",
	matchIfMissing = true
)
public class DataRetentionCleanupScheduler {

	private final DataRetentionCleanupService cleanupService;
	private final Clock clock;

	public DataRetentionCleanupScheduler(
		DataRetentionCleanupService cleanupService,
		@Qualifier("dataRetentionClock") Clock clock
	) {
		this.cleanupService = cleanupService;
		this.clock = clock;
	}

	@Scheduled(
		cron = "${app.data-retention.cleanup-cron:0 0 3 * * *}",
		zone = "${app.data-retention.zone:UTC}"
	)
	public void cleanupExpiredData() {
		cleanupService.cleanupExpiredData(OffsetDateTime.now(clock));
	}
}
