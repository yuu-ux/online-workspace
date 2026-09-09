import types/user.{type UserId, UserId, type UserInfo, UserInfo}
import types/session.{type Session, Token}
import gleam/list
import types/room.{
  type RoomId,
}

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

