package com.example.online_workspace.forms;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
	@NotBlank @Size(max = 100) String name,
	@Size(max = 500)
	@Pattern(regexp = "(?i)^https?://\\S+$", message = "アイコンURLはhttpまたはhttpsで入力してください。")
	String iconUrl,
	@NotNull @Size(max = 500) String bio,
	@Positive Long workCategoryId,
	@NotNull Boolean isPublic
) {
}
