import types/user.{type UserId, UserId}

pub type Token {
  Token(String)
}

pub type Session {
  Guest
  Authenticated(jwt: Token, user_id: UserId)
}

/// TODO テスト用関数
pub fn stringify_session(session: Session) -> String {
  case session {
    Guest -> {
      "Guest"
    }
    Authenticated(jwt, user_id) -> {
      "Authenticated jwt: " <> case jwt { Token(m) -> {m}} <> ", user_id: " <> case user_id { UserId(a) -> {a} }
    }
  }
}

