import types/session.{type Session, Authenticated, Token}
import types/user.{type UserId, UserId}

pub type LoginErr {
  DummyError // TODO 後で消す
}

/// ログイン処理を実行
/// session用のtokenを発行する
pub fn login_proc(email: String, password: String) -> Result(Session, LoginErr) {
  // TODO SERVER API
  case email, password {
    "fail", _ -> {
      Error(DummyError)
    }
    "tom@example.com", "0427" -> {
      Ok(Authenticated(jwt: Token("toms jwt"), user_id: UserId("Tom")))
    }
    _, _ -> {
      Ok(Authenticated(jwt: Token("dummy"), user_id: UserId("dummy_user id")))
    }
  }
}
