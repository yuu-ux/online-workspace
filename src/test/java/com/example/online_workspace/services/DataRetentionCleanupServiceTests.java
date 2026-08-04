package com.example.online_workspace.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.example.online_workspace.configs.DataRetentionProperties;
import com.example.online_workspace.models.DataRetentionCleanupResult;
import com.example.online_workspace.repositories.DataRetentionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.TransientDataAccessResourceException;

class DataRetentionCleanupServiceTests {

	private final DataRetentionRepository repository = org.mockito.Mockito.mock(DataRetentionRepository.class);
	private final DataRetentionProperties properties = new DataRetentionProperties();
	private final DataRetentionCleanupService service =
		new DataRetentionCleanupService(repository, properties);

	@Test
	void calculatesCalendarMonthAndDayCutoffsFromExecutionTime() {
		OffsetDateTime executionTime = OffsetDateTime.of(2026, 8, 31, 3, 0, 0, 0, ZoneOffset.UTC);
		when(repository.deleteMessagesSentBefore(executionTime.minusMonths(3))).thenReturn(4);
		when(repository.deleteWorkSessionsForUsersWithdrawnBefore(executionTime.minusDays(30))).thenReturn(2);

		DataRetentionCleanupResult result = service.cleanupExpiredData(executionTime);

		assertThat(result.chatHistoryCutoff())
			.isEqualTo(OffsetDateTime.of(2026, 5, 31, 3, 0, 0, 0, ZoneOffset.UTC));
		assertThat(result.withdrawnWorkHistoryCutoff()).isEqualTo(executionTime.minusDays(30));
		assertThat(result.deletedMessages()).isEqualTo(4);
		assertThat(result.deletedWorkSessions()).isEqualTo(2);
	}

	@Test
	void canBeRetriedAfterTransientFailure() {
		OffsetDateTime executionTime = OffsetDateTime.of(2026, 8, 4, 3, 0, 0, 0, ZoneOffset.UTC);
		OffsetDateTime workHistoryCutoff = executionTime.minusDays(30);
		when(repository.deleteWorkSessionsForUsersWithdrawnBefore(workHistoryCutoff))
			.thenThrow(new TransientDataAccessResourceException("temporary failure"))
			.thenReturn(3);

		assertThatThrownBy(() -> service.cleanupExpiredData(executionTime))
			.isInstanceOf(TransientDataAccessResourceException.class);
		assertThat(service.cleanupExpiredData(executionTime).deletedWorkSessions()).isEqualTo(3);

		verify(repository, times(2)).deleteMessagesSentBefore(executionTime.minusMonths(3));
		verify(repository, times(2)).deleteWorkSessionsForUsersWithdrawnBefore(workHistoryCutoff);
	}
}
