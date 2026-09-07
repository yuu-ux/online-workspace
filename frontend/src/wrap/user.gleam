import gleam/dynamic/decode
import gleam/int
import gleam/json
import lustre/effect
import types/user.{type FriendInfo, FriendInfo, type UserId, UserId, type UserInfo, UserInfo}
import types/session.{type Session}
import types/room.{
  type RoomId,
}
import wrap/api.{type ApiError, ApiError}

/// get_all_user_info_list関数のエラー
pub type GetUserInfoListErr {
  GetUserInfoListAuthErr // roomにアクセスする権限が無い
}

/// ルームに所属するすべてのユーザーを表示する
pub fn get_all_user_info_list(session: Session, room_id: RoomId) -> Result(List(UserInfo), GetUserInfoListErr) {
  // TODO SERVER API
  Ok([
    UserInfo(name: "Tom", user_id: UserId("xxx")),
    UserInfo(name: "Alice", user_id: UserId("xyz")),
    UserInfo(name: "Bob", user_id: UserId("123")),
  ])
}

pub type ReportReason {
  ViolationOfTerms
  Other
}

pub type SearchErr {
  SearchDummyErr
}

/// userを名前から検索する
pub fn search_user(user_name: String) -> Result(List(UserInfo), SearchErr) {
  // TODO SERVER API
  Ok([
    UserInfo(name: "Tom", user_id: UserId("xxx")),
    UserInfo(name: "Alice", user_id: UserId("xyz")),
    UserInfo(name: "Bob", user_id: UserId("123")),
  ])
}

pub type InviteUserErr {
  InviteAuthErr // 招待権限が無い
  ExceedsMaxMember // ルームの人数上限を超える
}

/// userをroomに招待する
pub fn invite_user_to_room(session: Session, room_id: RoomId, user: UserId) -> Result(Nil, InviteUserErr) {
  // TODO SERVER API
  Ok(Nil)
}

/// すべてのフレンドを取得する
pub fn get_friends(
  to_msg: fn(Result(List(FriendInfo), ApiError)) -> msg,
) -> effect.Effect(msg) {
  api.json_request(
    "GET",
    "/api/v1/friends",
    "",
    friend_page_decoder(),
    to_msg,
  )
}

pub fn add_friend(
  user_id: UserId,
  to_msg: fn(Result(Nil, ApiError)) -> msg,
) -> effect.Effect(msg) {
  case parse_user_id(user_id) {
    Ok(id) -> api.empty_request(
      "POST",
      "/api/v1/friends",
      json.object([#("userId", json.int(id))]) |> json.to_string,
      to_msg,
    )
    Error(_) -> error_effect(to_msg, "ユーザーIDが不正です")
  }
}

pub fn remove_friend(
  user_id: UserId,
  to_msg: fn(Result(Nil, ApiError)) -> msg,
) -> effect.Effect(msg) {
  case parse_user_id(user_id) {
    Ok(id) -> api.empty_request(
      "DELETE",
      "/api/v1/friends/" <> int.to_string(id),
      "",
      to_msg,
    )
    Error(_) -> error_effect(to_msg, "ユーザーIDが不正です")
  }
}

/// self_user_idがother_user_idのフレンドかどうかを確かめる
pub fn is_friend(self_user_id: UserId, other_user_id: UserId) -> Bool {
  // TODO API SERVER
  False
}

/// self_user_idがother_user_idをブロックしているかどうかを確かめる
pub fn is_blocked(self_user_id: UserId, other_user_id: UserId) -> Bool {
  // TODO API SERVER
  False
}

fn friend_page_decoder() -> decode.Decoder(List(FriendInfo)) {
  use friends <- decode.field("items", decode.list(friend_decoder()))
  decode.success(friends)
}

fn friend_decoder() -> decode.Decoder(FriendInfo) {
  use user <- decode.field("user", user_info_decoder())
  use online <- decode.field("online", decode.bool)
  decode.success(FriendInfo(user: user, online: online))
}

fn user_info_decoder() -> decode.Decoder(UserInfo) {
  use id <- decode.field("id", decode.int)
  use name <- decode.field("name", decode.string)
  decode.success(UserInfo(name: name, user_id: UserId(int.to_string(id))))
}

fn parse_user_id(user_id: UserId) -> Result(Int, Nil) {
  let UserId(value) = user_id
  int.parse(value)
}

fn error_effect(
  to_msg: fn(Result(Nil, ApiError)) -> msg,
  message: String,
) -> effect.Effect(msg) {
  effect.from(fn(dispatch) {
    dispatch(to_msg(Error(ApiError(message))))
  })
}
