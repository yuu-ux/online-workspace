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
    visibility: VisibilityType,
    category: CategoryType,
    work_style: WorkStyleType,
    max_number_of_member: Int,
    room_id: RoomId
  )
}

