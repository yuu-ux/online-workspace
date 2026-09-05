import lustre/effect
import gleam/dynamic/decode
import gleam/json
import gleam/result
import types/room.{
  type CategoryType,
  type WorkStyleType,
  type VisibilityType,
  type RoomId,
  type RoomNameType,
  type DescriptionType,
  type RoomInfo,
  RoomNameType,
  DescriptionType,
  RoomId,
  Cat1,
  Cat2,
  Cat3,
  CasualChat,
  Quiet,
  Public,
  Invite,
  Friend,
  RoomInfo
}

import types/user.{type UserId}
import types/session.{type Session,type Token, Guest, Authenticated}
import wrap/api.{type ApiError}

pub fn create_room(
  roomname: RoomNameType,
  description: DescriptionType,
  category_type: CategoryType,
  workstyle_type: WorkStyleType,
  max_number_of_member: Int,
  to_msg: fn(Result(RoomId, ApiError)) -> msg,
) -> effect.Effect(msg) {
  let RoomNameType(name) = roomname
  let DescriptionType(description) = description
  api.json_request(
    "POST",
    "/api/v1/rooms",
    json.object([
      #("name", json.string(name)),
      #("description", json.string(description)),
      #("categoryId", json.int(category_id(category_type))),
      #("workStyle", json.string(work_style_code(workstyle_type))),
      #("maxMembers", json.int(max_number_of_member)),
    ])
    |> json.to_string,
    room_id_decoder(),
    to_msg,
  )
}

pub fn get_rooms(
  to_msg: fn(Result(List(RoomInfo), ApiError)) -> msg,
) -> effect.Effect(msg) {
  api.json_request(
    "GET",
    "/api/v1/rooms",
    "",
    rooms_decoder(),
    to_msg,
  )
}

fn rooms_decoder() -> decode.Decoder(List(RoomInfo)) {
  use rooms <- decode.field("items", decode.list(room_decoder()))
  decode.success(rooms)
}

fn room_decoder() -> decode.Decoder(RoomInfo) {
  use id <- decode.field("id", decode.int)
  use name <- decode.field("name", decode.string)
  use category_id <- decode.field(
    "category",
    category_id_decoder(),
  )
  use work_style <- decode.field("workStyle", decode.string)
  use max_members <- decode.field("maxMembers", decode.int)
  decode.success(RoomInfo(
    roomname: RoomNameType(name),
    visibility: Public,
    category: category_from_id(category_id),
    work_style: case work_style {
      "CHAT_OK" -> CasualChat
      _ -> Quiet
    },
    max_number_of_member: max_members,
    room_id: RoomId(id),
  ))
}

fn category_id_decoder() -> decode.Decoder(Int) {
  use id <- decode.field("id", decode.int)
  decode.success(id)
}

fn room_id_decoder() -> decode.Decoder(RoomId) {
  use id <- decode.field("id", decode.int)
  decode.success(RoomId(id))
}

fn category_id(category: CategoryType) -> Int {
  case category {
    Cat1 -> 1
    Cat2 -> 2
    Cat3 -> 3
  }
}

fn category_from_id(id: Int) -> CategoryType {
  case id {
    1 -> Cat1
    2 -> Cat2
    _ -> Cat3
  }
}

fn work_style_code(work_style: WorkStyleType) -> String {
  case work_style {
    CasualChat -> "CHAT_OK"
    Quiet -> "FOCUS"
  }
}

pub type RoomErr {
  RoomDummyError
}

pub type Chat {
  Chat(
    // from: String, // 誰から
    // timestamp: String,
    // comment: String, 

    msg_type: String,
    room_id: RoomId, 
    user: String,
    message: String,
  )
}

// ---------------------------------------------------------
// 1. FFI (JavaScriptの関数をインポート)
// ---------------------------------------------------------

/// 接続する
@external(javascript, "./../ffi/ws.js", "connect_ws")
fn do_connect(url: String, dispatch: fn(String) -> Nil) -> Nil

/// 送る
@external(javascript, "./../ffi/ws.js", "send_ws")
fn do_send(message: String) -> Bool

@external(javascript, "./../ffi/ws.js", "close_ws")
pub fn close_ws() -> Nil

/// チャット画面に入ったタイミングで接続を開始する
pub fn connect_to_server(m: fn(String) -> msg) -> effect.Effect(msg) {
  effect.from(fn(dispatch) {
    let js_callback = fn(received_text: String) {
      dispatch(m(received_text))
    }

    // JS側の接続関数を呼び出す
    do_connect("/ws-test", js_callback)
  })
}

pub fn room_send_msg_proc(
  user_name: String,
  room_id: RoomId,
  send_msg: String
) -> Result(Nil, RoomErr) {
  // TODO SERVER API
  let message = chat_to_json(
    Chat(
      msg_type: "msg",
      room_id: room_id,
      user: user_name,
      message: send_msg
    )
  )

  case do_send(message) {
    True -> Ok(Nil)
    False -> Error(RoomDummyError)
  }
}

// TODO テストデータ用構造体

// https://gleam-json.hexdocs.pm/

pub fn chat_to_json(chat: Chat) -> String {
  json.object([
    #("type", json.string(chat.msg_type)),
    #("room_id", json.int(case chat.room_id { RoomId(i) -> i })),
    #("user", json.string(chat.user)),
    #("message", json.string(chat.message)),
  ])
  |> json.to_string
}

pub fn chat_from_json(json_string: String) -> Result(Chat, json.DecodeError) {
  let chat_decoder = {
    use msg_type <- decode.field("type", decode.string)
    use room_id <- decode.field("room_id", decode.int)
    use user <- decode.field("user", decode.string)
    use message <- decode.field("message", decode.string)
    decode.success(Chat(
      msg_type: msg_type,
      room_id: RoomId(room_id),
      user: user,
      message: message
    ))
  }
  json.parse(from: json_string, using: chat_decoder)
}
