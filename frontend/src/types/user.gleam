pub type UserId {
  UserId(String)
}

pub type UserInfo {
  UserInfo(
    name: String,
    user_id: UserId
  )
}

