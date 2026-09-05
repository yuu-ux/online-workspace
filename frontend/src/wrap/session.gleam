import gleam/dynamic/decode
import gleam/int
import gleam/json
import gleam/result
import lustre/effect
import types/session.{type Session, Authenticated, Guest, Token}
import types/user.{type UserInfo, UserId, UserInfo}
import wrap/api.{type ApiError}

pub fn login_proc(
  email: String,
  password: String,
  to_msg: fn(Result(Session, ApiError)) -> msg,
) -> effect.Effect(msg) {
  api.json_request(
    "POST",
    "/api/v1/auth/login",
    json.object([
      #("email", json.string(email)),
      #("password", json.string(password)),
    ])
    |> json.to_string,
    authenticated_user_decoder(),
    fn(response) { to_msg(response |> result.map(to_session)) },
  )
}

pub fn get_session(
  to_msg: fn(Result(Session, ApiError)) -> msg,
) -> effect.Effect(msg) {
  api.json_request(
    "GET",
    "/api/v1/auth/session",
    "",
    session_decoder(),
    to_msg,
  )
}

pub fn logout_proc(
  to_msg: fn(Result(Nil, ApiError)) -> msg,
) -> effect.Effect(msg) {
  api.empty_request("POST", "/api/v1/auth/logout", "", to_msg)
}

fn session_decoder() -> decode.Decoder(Session) {
  use authenticated <- decode.field("authenticated", decode.bool)
  case authenticated {
    False -> decode.success(Guest)
    True -> {
      use user <- decode.field("user", authenticated_user_decoder())
      decode.success(to_session(user))
    }
  }
}

fn authenticated_user_decoder() -> decode.Decoder(UserInfo) {
  use id <- decode.field("id", decode.int)
  use name <- decode.field("name", decode.string)
  decode.success(UserInfo(name: name, user_id: UserId(id |> int.to_string)))
}

fn to_session(user: UserInfo) -> Session {
  Authenticated(jwt: Token(""), user_id: user)
}
