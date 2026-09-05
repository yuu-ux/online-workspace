import gleam/dynamic/decode
import gleam/json
import gleam/result
import lustre/effect

pub type ApiError {
  ApiError(message: String)
}

@external(javascript, "./../ffi/api.js", "request")
fn request(
  method: String,
  path: String,
  body: String,
  callback: fn(Int, String) -> Nil,
) -> Nil

pub fn json_request(
  method: String,
  path: String,
  body: String,
  decoder: decode.Decoder(a),
  to_msg: fn(Result(a, ApiError)) -> msg,
) -> effect.Effect(msg) {
  effect.from(fn(dispatch) {
    request(method, path, body, fn(status, response_body) {
      dispatch(to_msg(decode_response(status, response_body, decoder)))
    })
  })
}

pub fn empty_request(
  method: String,
  path: String,
  body: String,
  to_msg: fn(Result(Nil, ApiError)) -> msg,
) -> effect.Effect(msg) {
  effect.from(fn(dispatch) {
    request(method, path, body, fn(status, response_body) {
      let response = case status >= 200 && status < 300 {
        True -> Ok(Nil)
        False -> Error(api_error(status, response_body))
      }
      dispatch(to_msg(response))
    })
  })
}

fn decode_response(
  status: Int,
  body: String,
  decoder: decode.Decoder(a),
) -> Result(a, ApiError) {
  case status >= 200 && status < 300 {
    True ->
      json.parse(body, decoder)
      |> result.map_error(fn(_) { ApiError("サーバーの応答を読み取れませんでした") })
    False -> Error(api_error(status, body))
  }
}

fn api_error(status: Int, body: String) -> ApiError {
  let message_decoder = {
    use message <- decode.field("message", decode.string)
    decode.success(message)
  }
  case json.parse(body, message_decoder) {
    Ok(message) -> ApiError(message)
    Error(_) if status == 0 -> ApiError("サーバーに接続できませんでした")
    Error(_) -> ApiError("リクエストに失敗しました")
  }
}
