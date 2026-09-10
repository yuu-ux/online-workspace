import gleam/dynamic/decode
import gleam/int
import gleam/json
import gleam/option
import lustre/effect
import types/room.{
  Cat1,
  Cat2,
  Cat3,
  CasualChat,
  DescriptionType,
  Public,
  Quiet,
  RoomId,
  RoomDetail,
  RoomInfo,
  RoomNameType,
  type CategoryType,
  type DescriptionType,
  type RoomDetail,
  type RoomId,
  type RoomInfo,
  type RoomNameType,
  type WorkStyleType,
}
import types/user.{UserId, UserInfo, type UserInfo}
import wrap/api as api

pub fn create_room(
  roomname: RoomNameType,
  description: DescriptionType,
  category_type: CategoryType,
  workstyle_type: WorkStyleType,
  max_number_of_member: Int,
  to_msg: fn(Result(RoomId, api.ApiError)) -> msg,
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
  to_msg: fn(Result(List(RoomInfo), api.ApiError)) -> msg,
) -> effect.Effect(msg) {
  api.json_request(
    "GET",
    "/api/v1/rooms",
    "",
    rooms_decoder(),
    to_msg,
  )
}

pub fn get_room(
  room_id: RoomId,
  to_msg: fn(Result(RoomDetail, api.ApiError)) -> msg,
) -> effect.Effect(msg) {
  api.json_request(
    "GET",
    "/api/v1/rooms/" <> room_path(room_id),
    "",
    room_detail_decoder(),
    to_msg,
  )
}

fn rooms_decoder() -> decode.Decoder(List(RoomInfo)) {
  use rooms <- decode.field("items", decode.list(room_decoder()))
  decode.success(rooms)
}

pub fn room_info_decoder() -> decode.Decoder(RoomInfo) {
  use id <- decode.field("id", decode.int)
  use name <- decode.field("name", decode.string)
  use category_id <- decode.field(
    "category",
    category_id_decoder(),
  )
  use work_style <- decode.field("workStyle", decode.string)
  use max_members <- decode.field("maxMembers", decode.int)
  use current_members <- decode.field("currentMembers", decode.int)
  use status <- decode.field("status", decode.string)
  use joinable <- decode.field("joinable", decode.bool)
  use join_restriction <- decode.optional_field(
    "joinRestriction",
    option.None,
    decode.optional(decode.string),
  )
  use created_at <- decode.field("createdAt", decode.string)
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
    current_members: current_members,
    status: status,
    joinable: joinable,
    join_restriction: join_restriction,
    created_at: created_at,
  ))
}

fn room_decoder() -> decode.Decoder(RoomInfo) {
  room_info_decoder()
}

pub fn room_detail_decoder() -> decode.Decoder(RoomDetail) {
  use id <- decode.field("id", decode.int)
  use name <- decode.field("name", decode.string)
  use description <- decode.field("description", decode.string)
  use category_id <- decode.field(
    "category",
    category_id_decoder(),
  )
  use work_style <- decode.field("workStyle", decode.string)
  use max_members <- decode.field("maxMembers", decode.int)
  use current_members <- decode.field("currentMembers", decode.int)
  use status <- decode.field("status", decode.string)
  use creator_id <- decode.subfield(["createdBy", "id"], decode.int)
  use creator_name <- decode.subfield(["createdBy", "name"], decode.string)
  use joinable <- decode.field("joinable", decode.bool)
  use join_restriction <- decode.optional_field(
    "joinRestriction",
    option.None,
    decode.optional(decode.string),
  )
  use member <- decode.field("member", decode.bool)
  use created_at <- decode.field("createdAt", decode.string)
  use updated_at <- decode.field("updatedAt", decode.string)
  decode.success(RoomDetail(
    room_id: RoomId(id),
    roomname: RoomNameType(name),
    description: DescriptionType(description),
    category: category_from_id(category_id),
    work_style: case work_style {
      "CHAT_OK" -> CasualChat
      _ -> Quiet
    },
    max_number_of_member: max_members,
    current_members: current_members,
    status: status,
    created_by: UserInfo(
      name: creator_name,
      user_id: UserId(int.to_string(creator_id)),
    ),
    joinable: joinable,
    join_restriction: join_restriction,
    member: member,
    created_at: created_at,
    updated_at: updated_at,
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

pub fn join_room(
  room_id: RoomId,
  to_msg: fn(Result(Nil, api.ApiError)) -> msg,
) -> effect.Effect(msg) {
  api.empty_request(
    "POST",
    "/api/v1/rooms/" <> room_path(room_id) <> "/members/me",
    "",
    to_msg,
  )
}

pub fn leave_room(
  room_id: RoomId,
  to_msg: fn(Result(Nil, api.ApiError)) -> msg,
) -> effect.Effect(msg) {
  api.empty_request(
    "DELETE",
    "/api/v1/rooms/" <> room_path(room_id) <> "/members/me",
    "",
    to_msg,
  )
}

pub fn get_room_members(
  room_id: RoomId,
  to_msg: fn(Result(List(UserInfo), api.ApiError)) -> msg,
) -> effect.Effect(msg) {
  api.json_request(
    "GET",
    "/api/v1/rooms/" <> room_path(room_id) <> "/members",
    "",
    decode.list(room_member_decoder()),
    to_msg,
  )
}

pub fn get_messages(
  room_id: RoomId,
  to_msg: fn(Result(List(Chat), api.ApiError)) -> msg,
) -> effect.Effect(msg) {
  api.json_request(
    "GET",
    "/api/v1/rooms/" <> room_path(room_id) <> "/messages?page=0&size=50",
    "",
    message_list_decoder(),
    to_msg,
  )
}

pub fn create_message(
  room_id: RoomId,
  message: String,
  to_msg: fn(Result(Nil, api.ApiError)) -> msg,
) -> effect.Effect(msg) {
  api.empty_request(
    "POST",
    "/api/v1/rooms/" <> room_path(room_id) <> "/messages",
    json.object([#("content", json.string(message))]) |> json.to_string,
    to_msg,
  )
}

fn room_path(room_id: RoomId) -> String {
  let RoomId(id) = room_id
  int.to_string(id)
}

fn room_member_decoder() -> decode.Decoder(UserInfo) {
  use id <- decode.subfield(["user", "id"], decode.int)
  use name <- decode.subfield(["user", "name"], decode.string)
  decode.success(UserInfo(name: name, user_id: UserId(int.to_string(id))))
}

fn message_list_decoder() -> decode.Decoder(List(Chat)) {
  use messages <- decode.field("items", decode.list(message_decoder()))
  decode.success(messages)
}

fn message_decoder() -> decode.Decoder(Chat) {
  use room_id <- decode.field("roomId", decode.int)
  use user <- decode.subfield(["sender", "name"], decode.string)
  use message <- decode.field("content", decode.string)
  decode.success(
    Chat(
      msg_type: "msg",
      room_id: RoomId(room_id),
      user: user,
      message: message,
    ),
  )
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

pub type Chat {
  Chat(
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
fn do_connect(room_id: Int, dispatch: fn(String) -> Nil) -> Nil

/// 送る
@external(javascript, "./../ffi/ws.js", "send_ws")
fn do_send(message: String) -> Bool

@external(javascript, "./../ffi/ws.js", "close_ws")
pub fn close_ws() -> Nil

/// チャット画面に入ったタイミングで接続を開始する
pub fn connect_to_server(room_id: RoomId, m: fn(String) -> msg) -> effect.Effect(msg) {
  let RoomId(id) = room_id
  effect.from(fn(dispatch) {
    let js_callback = fn(received_text: String) {
      dispatch(m(received_text))
    }

    // JS側の接続関数を呼び出す
    do_connect(id, js_callback)
  })
}

pub fn send_ws_message(message: String) -> Bool {
  case do_send(message) {
    True -> True
    False -> False
  }
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
