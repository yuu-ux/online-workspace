package com.example.online_workspace.forms;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WithdrawRequest(
	@NotBlank(message = "パスワードを入力してください")
	@Size(max = 72, message = "パスワードは72文字以内で入力してください")
	String password
) {
}
