package com.example.online_workspace.form;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
/**
 * UserForm
 */

@Data
public class UserForm {
    @NotNull(message = "名前は必須です。")
    private String name;

    @NotNull(message = "メールアドレスは必須です。")
    @Email(message = "正しいメールアドレスを入力してください。")
    private String email;

    @NotNull(message = "パスワードは必須です。")
    private String password;

    @NotNull(message = "確認パスワードは必須です。")
    private String passwordConfirmation;
}
