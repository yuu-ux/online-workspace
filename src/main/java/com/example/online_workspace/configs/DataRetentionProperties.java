package com.example.online_workspace.configs;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.data-retention")
public class DataRetentionProperties {

	@Min(1)
	private int chatHistoryMonths = 3;

	@Min(1)
	private int withdrawnWorkHistoryDays = 30;

	public int getChatHistoryMonths() {
		return chatHistoryMonths;
	}

	public void setChatHistoryMonths(int chatHistoryMonths) {
		this.chatHistoryMonths = chatHistoryMonths;
	}

	public int getWithdrawnWorkHistoryDays() {
		return withdrawnWorkHistoryDays;
	}

	public void setWithdrawnWorkHistoryDays(int withdrawnWorkHistoryDays) {
		this.withdrawnWorkHistoryDays = withdrawnWorkHistoryDays;
	}
}
