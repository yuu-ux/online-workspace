import lustre/effect
import gleam/dynamic/decode
import gleam/json
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

pub type CreateRoomErr {
  DummyError
}

pub fn create_room(
  roomname: RoomNameType,
  description: DescriptionType,
  category_type: Result(CategoryType, Nil),
  workstyle_type: Result(WorkStyleType, Nil),
  visibility: Result(VisibilityType, Nil),
  max_number_of_member: Result(Int, Nil)
) -> Result(RoomId, CreateRoomErr) {
  // TODO SERVER API
  case category_type, workstyle_type, visibility, max_number_of_member {
    Ok(_), Ok(_), Ok(_), Ok(_) -> Ok(RoomId(0))
    _, _, _, _ -> Error(DummyError)
  }
}

pub fn get_rooms(jwt: Token, user_id: UserId) -> List(RoomInfo) {
  [
    RoomInfo(
      roomname: RoomNameType("Room1"),
      visibility: Public,
      category: Cat1,
      work_style: Quiet,
      max_number_of_member: 10,
      room_id: RoomId(1)
    ),

    RoomInfo(
      roomname: RoomNameType("Room2"),
      visibility: Friend,
      category: Cat2,
      work_style: CasualChat,
      max_number_of_member: 12,
      room_id: RoomId(2)
    ),
  ]
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
