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

pub type ReportErr {
  // TODO 後で消す
  DummyErr
}

/// userを通報する
pub fn report_user(session: Session, user_info: UserInfo, report_reason: ReportReason, report_details: String) -> Result(Nil, ReportErr) {

}

/// userを名前から検索する
pub fn search_user(user_name: String) -> List(UserInfo) {
  // TODO SERVER API
  []
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

/// フレンドを取得する
pub fn get_friends(session: Session) -> List(UserInfo) {
  // TODO SERVER API
  []
}
