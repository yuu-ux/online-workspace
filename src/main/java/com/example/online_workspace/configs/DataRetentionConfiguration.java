package com.example.online_workspace.configs;

import java.time.Clock;
import java.time.OffsetDateTime;

import com.example.online_workspace.services.DataRetentionCleanupService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(DataRetentionProperties.class)
public class DataRetentionConfiguration {

	@Bean
	Clock dataRetentionClock() {
		return Clock.systemUTC();
	}

	@Bean
	@ConditionalOnProperty(name = "app.data-retention.run-once", havingValue = "true")
	ApplicationRunner dataRetentionRunOnce(
		DataRetentionCleanupService cleanupService,
		@Qualifier("dataRetentionClock") Clock clock
	) {
		return arguments -> cleanupService.cleanupExpiredData(OffsetDateTime.now(clock));
	}
}
