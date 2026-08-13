package com.example.online_workspace.configs;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.example.online_workspace.services.DataRetentionCleanupService;
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
	@ConditionalOnProperty(name = "app.data-retention.run-once", havingValue = "true")
	ApplicationRunner dataRetentionRunOnce(DataRetentionCleanupService cleanupService) {
		return arguments -> cleanupService.cleanupExpiredData(OffsetDateTime.now(ZoneOffset.UTC));
	}
}
