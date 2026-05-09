package com.example.online_workspace.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoomCreateForm {
    @NotBlank(message = "ルーム名は必須です。")
    @Size(max = 100, message = "ルーム名は100文字以内で入力してください。")
    private String name;
}
