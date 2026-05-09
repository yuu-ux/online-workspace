package com.example.online_workspace.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MessageForm {
    @NotBlank(message = "メッセージは必須です。")
    @Size(max = 500, message = "メッセージは500文字以内で入力してください。")
    private String content;
}
