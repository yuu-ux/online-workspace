package com.example.online_workspace.forms;

import jakarta.validation.constraints.NotEmpty;

public record WithdrawRequest(
	@NotEmpty(message = "パスワードを入力してください")
	String password
) {
}
