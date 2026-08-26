
pub type RegisterErr {
  PasswordMismatch
}

/// ユーザーを登録する
pub fn register_user(username: String, email: String, password: String, password_confirm: String) -> Result(Nil, RegisterErr) {
  // TODO SERVER API
  case password == password_confirm {
    True -> {
      Ok(Nil)
    }
    False -> {
      Error(PasswordMismatch)
    }
  }
}
