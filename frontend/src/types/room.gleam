import gleam/option.{type Option}
import types/user.{type UserInfo}

pub type RoomNameType {
  RoomNameType(String)
}

pub type DescriptionType {
  DescriptionType(String)
}

pub type CategoryType {
  Cat1
  Cat2
  Cat3
}

pub type WorkStyleType {
  CasualChat
  Quiet
}

pub type VisibilityType {
  Public
  Invite
  Friend
}

pub type RoomId {
  RoomId(Int)
}

pub type RoomInfo {
  RoomInfo(
    roomname: RoomNameType,
    description: DescriptionType,
    visibility: VisibilityType,
    category: CategoryType,
    work_style: WorkStyleType,
    max_number_of_member: Int,
    room_id: RoomId,
    current_members: Int,
    status: String,
    joinable: Bool,
    join_restriction: Option(String),
    created_at: String,
  )
}

pub type RoomDetail {
  RoomDetail(
    room_id: RoomId,
    roomname: RoomNameType,
    description: DescriptionType,
    category: CategoryType,
    work_style: WorkStyleType,
    max_number_of_member: Int,
    current_members: Int,
    status: String,
    created_by: UserInfo,
    joinable: Bool,
    join_restriction: Option(String),
    member: Bool,
    created_at: String,
    updated_at: String,
  )
}
