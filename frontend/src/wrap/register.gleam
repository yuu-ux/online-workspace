import gleam/json
import gleam/result
import lustre/effect
import wrap/api.{type ApiError}

pub type RegisterErr {
  PasswordMismatch
  RequestFailed(ApiError)
}

pub fn register_user(
  username: String,
  email: String,
  password: String,
  password_confirm: String,
  to_msg: fn(Result(Nil, RegisterErr)) -> msg,
) -> effect.Effect(msg) {
  case password == password_confirm {
    False -> effect.from(fn(dispatch) { dispatch(to_msg(Error(PasswordMismatch))) })
    True ->
      api.empty_request(
        "POST",
        "/api/v1/auth/register",
        json.object([
          #("name", json.string(username)),
          #("email", json.string(email)),
          #("password", json.string(password)),
        ])
        |> json.to_string,
        fn(response) { to_msg(response |> result.map_error(RequestFailed)) },
      )
  }
}
