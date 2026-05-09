package com.example.online_workspace.form;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignUpForm {
    @NotBlank(message = "名前は必須です。")
    @Size(max = 50, message = "名前は50文字以内で入力してください。")
    private String name;

    @NotBlank(message = "メールアドレスは必須です。")
    @Email(message = "正しいメールアドレスを入力してください。")
    private String email;

    @NotBlank(message = "パスワードは必須です。")
    @Size(min = 8, max = 72, message = "パスワードは8文字以上72文字以下で入力してください。")
    private String password;

    @NotBlank(message = "確認パスワードは必須です。")
    private String passwordConfirmation;
}
