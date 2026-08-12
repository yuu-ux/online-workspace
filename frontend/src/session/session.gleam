pub type Session {
  Guest
  Authenticated(jwt: String, user_id: String)
}

/// TODO テスト用関数
pub fn stringify_session(session: Session) -> String {
  case session {
    Guest -> {
      "Guest"
    }
    Authenticated(jwt, user_id) -> {
      "Authenticated jwt: " <> jwt <> ", user_id: " <> user_id
    }
  }
}
