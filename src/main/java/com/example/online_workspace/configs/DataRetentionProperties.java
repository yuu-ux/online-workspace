package com.example.online_workspace.configs;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.data-retention")
public record DataRetentionProperties(
	@DefaultValue("3") @Min(1) int chatHistoryMonths,
	@DefaultValue("30") @Min(1) int withdrawnWorkHistoryDays
) {
}
