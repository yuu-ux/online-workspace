import types/user.{type UserId, UserId, type UserInfo, UserInfo}
import types/session.{type Session, Token}
import gleam/dynamic/decode
import gleam/list
import gleam/int
import lustre/effect
import types/room.{
  type RoomId,
}
import wrap/api

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
  SearchApiErr(api.ApiError)
}

/// userを名前から検索する
pub fn search_user(
  user_name: String,
  to_msg: fn(Result(List(UserInfo), SearchErr)) -> msg,
) -> effect.Effect(msg) {
  api.json_request(
    "GET",
    "/api/v1/users?query=" <> user_name <> "&page=0&size=20",
    "",
    search_decoder(),
    fn(result) {
      case result {
        Ok(users) -> to_msg(Ok(users))
        Error(err) -> to_msg(Error(SearchApiErr(err)))
      }
    },
  )
}

fn search_decoder() -> decode.Decoder(List(UserInfo)) {
  use users <- decode.field("items", decode.list(user_summary_decoder()))
  decode.success(users)
}

fn user_summary_decoder() -> decode.Decoder(UserInfo) {
  use id <- decode.field("id", decode.int)
  use name <- decode.field("name", decode.string)
  decode.success(UserInfo(name: name, user_id: UserId(int.to_string(id))))
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

pub type GetFriendErr {
  GetFriendDummyErr
}

/// すべてのフレンドを取得する
pub fn get_friends(session: Session) -> Result(List(UserInfo), GetFriendErr) {
  // TODO SERVER API
  Ok([
    UserInfo(name: "Tom", user_id: UserId("xxx")),
    UserInfo(name: "Alice", user_id: UserId("xyz")),
    UserInfo(name: "Bob", user_id: UserId("123")),
  ])
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

