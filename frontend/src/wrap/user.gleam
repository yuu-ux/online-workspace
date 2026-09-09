import gleam/dynamic/decode
import gleam/int
import gleam/json
import gleam/option
import gleam/result
import lustre/effect
import types/user.{type UserId, UserId, type UserInfo, UserInfo}
import types/session.{type Session, Token}
import types/room.{type RoomId}
import wrap/api

pub type SearchErr {
  SearchApiErr(api.ApiError)
}

pub type UserProfile {
  UserProfile(
    name: String,
    icon_url: String,
    is_public: Bool,
    bio: String,
    work_category: String,
    friendship: String,
  )
}

pub type GetUserProfileErr {
  GetUserProfileApiErr(api.ApiError)
}

pub type MyProfile {
  MyProfile(
    name: String,
    icon_url: String,
    is_public: Bool,
    bio: String,
    work_category_id: Int,
    work_category: String,
    email: String,
  )
}

pub type MyProfileErr {
  MyProfileApiErr(api.ApiError)
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

pub fn get_user_profile(
  user_id: UserId,
  to_msg: fn(Result(UserProfile, GetUserProfileErr)) -> msg,
) -> effect.Effect(msg) {
  let UserId(raw_user_id) = user_id
  api.json_request(
    "GET",
    "/api/v1/users/" <> raw_user_id,
    "",
    user_profile_decoder(),
    fn(result) {
      case result {
        Ok(profile) -> to_msg(Ok(profile))
        Error(err) -> to_msg(Error(GetUserProfileApiErr(err)))
      }
    },
  )
}

pub fn user_profile_decoder() -> decode.Decoder(UserProfile) {
  use name <- decode.field("name", decode.string)
  use icon_url <- decode.optional_field("iconUrl", option.None, decode.optional(decode.string))
  use is_public <- decode.field("isPublic", decode.bool)
  use bio <- decode.optional_field("bio", "", decode.string)
  use friendship <- decode.optional_field("friendship", "", decode.string)
  use work_category <- decode.optional_field(
    "workCategory",
    option.None,
    decode.optional(work_category_name_decoder()),
  )
  decode.success(
    UserProfile(
      name: name,
      icon_url: option.unwrap(icon_url, ""),
      is_public: is_public,
      bio: bio,
      work_category: option.unwrap(work_category, ""),
      friendship: friendship,
    ),
  )
}

fn work_category_name_decoder() -> decode.Decoder(String) {
  use name <- decode.field("name", decode.string)
  decode.success(name)
}

pub fn get_my_profile(
  to_msg: fn(Result(MyProfile, MyProfileErr)) -> msg,
) -> effect.Effect(msg) {
  api.json_request(
    "GET",
    "/api/v1/users/me/profile",
    "",
    my_profile_decoder(),
    fn(result) {
      case result {
        Ok(profile) -> to_msg(Ok(profile))
        Error(err) -> to_msg(Error(MyProfileApiErr(err)))
      }
    },
  )
}

fn my_profile_decoder() -> decode.Decoder(MyProfile) {
  use name <- decode.field("name", decode.string)
  use icon_url <- decode.optional_field("iconUrl", option.None, decode.optional(decode.string))
  use is_public <- decode.field("isPublic", decode.bool)
  use bio <- decode.field("bio", decode.string)
  use work_category_id <- decode.optional_field(
    "workCategory",
    option.None,
    decode.optional(work_category_id_decoder()),
  )
  use work_category <- decode.optional_field(
    "workCategory",
    option.None,
    decode.optional(work_category_name_decoder()),
  )
  use email <- decode.field("email", decode.string)
  decode.success(
    MyProfile(
      name: name,
      icon_url: option.unwrap(icon_url, ""),
      is_public: is_public,
      bio: bio,
      work_category_id: option.unwrap(work_category_id, 0),
      work_category: option.unwrap(work_category, ""),
      email: email,
    ),
  )
}

pub fn update_my_profile(
  name: String,
  icon_url: String,
  bio: String,
  work_category_id: Int,
  is_public: Bool,
  to_msg: fn(Result(MyProfile, api.ApiError)) -> msg,
) -> effect.Effect(msg) {
  let icon = case icon_url {
    "" -> json.null()
    _ -> json.string(icon_url)
  }
  let category = case work_category_id {
    0 -> json.null()
    id -> json.int(id)
  }
  let body = json.object([
    #("name", json.string(name)),
    #("iconUrl", icon),
    #("bio", json.string(bio)),
    #("workCategoryId", category),
    #("isPublic", json.bool(is_public)),
  ]) |> json.to_string
  api.json_request(
    "PUT",
    "/api/v1/users/me/profile",
    body,
    my_profile_decoder(),
    to_msg,
  )
}

fn work_category_id_decoder() -> decode.Decoder(Int) {
  use id <- decode.field("id", decode.int)
  decode.success(id)
}

/// すべてのフレンドを取得する
pub fn get_friends(
  to_msg: fn(Result(List(UserInfo), api.ApiError)) -> msg,
) -> effect.Effect(msg) {
  let decoder = {
    use friends <- decode.field("items", decode.list(friend_user_decoder()))
    decode.success(friends)
  }
  api.json_request(
    "GET",
    "/api/v1/friends?page=0&size=50",
    "",
    decoder,
    to_msg,
  )
}

/// フレンドを追加する
pub fn add_friend(
  user_id: UserId,
  to_msg: fn(Result(Nil, api.ApiError)) -> msg,
) -> effect.Effect(msg) {
  case user_id_int(user_id) {
    Ok(id) ->
      api.empty_request(
        "POST",
        "/api/v1/friends",
        json.object([#("userId", json.int(id))]) |> json.to_string,
        to_msg,
      )
    Error(err) -> effect.from(fn(dispatch) { dispatch(to_msg(Error(err))) })
  }
}

/// フレンドを解除する
pub fn remove_friend(
  user_id: UserId,
  to_msg: fn(Result(Nil, api.ApiError)) -> msg,
) -> effect.Effect(msg) {
  case user_id_path(user_id) {
    Ok(path) -> api.empty_request("DELETE", "/api/v1/friends/" <> path, "", to_msg)
    Error(err) -> effect.from(fn(dispatch) { dispatch(to_msg(Error(err))) })
  }
}

fn friend_user_decoder() -> decode.Decoder(UserInfo) {
  use id <- decode.subfield(["user", "id"], decode.int)
  use name <- decode.subfield(["user", "name"], decode.string)
  decode.success(UserInfo(name: name, user_id: UserId(int.to_string(id))))
}

fn user_id_int(user_id: UserId) -> Result(Int, api.ApiError) {
  let UserId(id) = user_id
  case int.parse(id) {
    Ok(parsed) -> Ok(parsed)
    Error(_) -> Error(api.ApiError("ユーザーIDの形式が不正です。"))
  }
}

fn user_id_path(user_id: UserId) -> Result(String, api.ApiError) {
  user_id_int(user_id) |> result.map(int.to_string)
}
