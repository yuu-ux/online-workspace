import gleam/dynamic/decode
import gleam/int
import gleam/json
import gleam/result
import lustre/effect
import types/user.{UserId, UserInfo, type UserId, type UserInfo}
import wrap/api

pub type MyProfile {
  MyProfile(
    id: Int,
    name: String,
    email: String,
    bio: String,
    is_public: Bool,
  )
}

pub type UserProfile {
  UserProfile(
    name: String,
    is_friend: Bool,
  )
}

pub fn search_user(
  user_name: String,
  to_msg: fn(Result(List(UserInfo), api.ApiError)) -> msg,
) -> effect.Effect(msg) {
  let decoder = {
    use users <- decode.field("items", decode.list(user_summary_decoder()))
    decode.success(users)
  }
  api.json_request(
    "GET",
    "/api/v1/users?query=" <> user_name <> "&page=0&size=20",
    "",
    decoder,
    to_msg,
  )
}

pub fn get_friends(
  to_msg: fn(Result(List(UserInfo), api.ApiError)) -> msg,
) -> effect.Effect(msg) {
  let decoder = {
    use users <- decode.field("items", decode.list(friend_user_decoder()))
    decode.success(users)
  }
  api.json_request(
    "GET",
    "/api/v1/friends?page=0&size=50",
    "",
    decoder,
    to_msg,
  )
}

pub fn get_user_profile(
  user_id: UserId,
  to_msg: fn(Result(UserProfile, api.ApiError)) -> msg,
) -> effect.Effect(msg) {
  case user_id_path(user_id) {
    Ok(path) ->
      api.json_request(
        "GET",
        "/api/v1/users/" <> path,
        "",
        user_profile_decoder(),
        to_msg,
      )
    Error(err) -> effect.from(fn(dispatch) { dispatch(to_msg(Error(err))) })
  }
}

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

pub fn remove_friend(
  user_id: UserId,
  to_msg: fn(Result(Nil, api.ApiError)) -> msg,
) -> effect.Effect(msg) {
  case user_id_path(user_id) {
    Ok(path) -> api.empty_request("DELETE", "/api/v1/friends/" <> path, "", to_msg)
    Error(err) -> effect.from(fn(dispatch) { dispatch(to_msg(Error(err))) })
  }
}

pub fn get_my_profile(
  to_msg: fn(Result(MyProfile, api.ApiError)) -> msg,
) -> effect.Effect(msg) {
  api.json_request(
    "GET",
    "/api/v1/users/me/profile",
    "",
    my_profile_decoder(),
    to_msg,
  )
}

pub fn update_my_profile(
  profile: MyProfile,
  to_msg: fn(Result(MyProfile, api.ApiError)) -> msg,
) -> effect.Effect(msg) {
  api.json_request(
    "PUT",
    "/api/v1/users/me/profile",
    update_profile_payload(profile),
    my_profile_decoder(),
    to_msg,
  )
}

fn update_profile_payload(profile: MyProfile) -> String {
  json.object([
    #("name", json.string(profile.name)),
    #("iconUrl", json.null()),
    #("bio", json.string(profile.bio)),
    #("workCategoryId", json.null()),
    #("isPublic", json.bool(profile.is_public)),
  ])
  |> json.to_string
}

fn my_profile_decoder() -> decode.Decoder(MyProfile) {
  use id <- decode.field("id", decode.int)
  use name <- decode.field("name", decode.string)
  use email <- decode.field("email", decode.string)
  use bio <- decode.field("bio", decode.string)
  use is_public <- decode.field("isPublic", decode.bool)
  decode.success(MyProfile(id: id, name: name, email: email, bio: bio, is_public: is_public))
}

fn user_profile_decoder() -> decode.Decoder(UserProfile) {
  use name <- decode.field("name", decode.string)
  use friendship <- decode.optional_field("friendship", "NONE", decode.string)
  decode.success(UserProfile(name: name, is_friend: friendship == "FRIEND"))
}

fn user_summary_decoder() -> decode.Decoder(UserInfo) {
  use id <- decode.field("id", decode.int)
  use name <- decode.field("name", decode.string)
  decode.success(UserInfo(name: name, user_id: UserId(int.to_string(id))))
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

