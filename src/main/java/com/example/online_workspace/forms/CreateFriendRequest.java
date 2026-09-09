package com.example.online_workspace.forms;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateFriendRequest(
	@NotNull @Positive Long userId
) {
}
