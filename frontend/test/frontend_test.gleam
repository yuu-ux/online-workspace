import gleeunit
import gleeunit/should
import frontend
import gleam/json
import lustre/element
import gleam/string
import gleam/option.{Some}
import pages/friend
import pages/mypage
import pages/room
import components/userinfo
import wrap/api.{ApiError}
import types/session.{Authenticated, Token}
import types/room as room_t
import types/user.{UserInfo, UserId}
import wrap/user as user_wrap

pub fn main() {
  gleeunit.main()
}

pub fn room_presence_updates_member_list_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let #(model, _) = room.init(session, room_t.RoomId(10))
  let #(with_member, _) = room.update(
    model,
    room.MembersLoaded(Ok([UserInfo("Alice", UserId("2"))])),
  )

  let #(joined_model, _) = room.update(
    with_member,
    room.WsMessageReceived(
      "{\"type\":\"presence\",\"room_id\":10,\"user_id\":3,\"user\":\"Bob\",\"online\":true}",
    ),
  )
  joined_model.member_list
  |> should.equal([UserInfo("Bob", UserId("3")), UserInfo("Alice", UserId("2"))])

  let #(left_model, _) = room.update(
    joined_model,
    room.WsMessageReceived(
      "{\"type\":\"presence\",\"room_id\":10,\"user_id\":3,\"user\":\"Bob\",\"online\":false}",
    ),
  )
  left_model.member_list |> should.equal([UserInfo("Alice", UserId("2"))])
}

pub fn friend_loaded_message_updates_friend_page_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let #(friend_model, _) = friend.init(session)
  let model = frontend.Model(
    current_page: frontend.Friend(friend_model),
    session: session,
  )

  let #(updated_model, _) = frontend.update(
    model,
    frontend.FriendMsg(friend.FriendsLoaded(Ok([
      UserInfo("test", UserId("1")),
    ]))),
  )

  case updated_model.current_page {
    frontend.Friend(updated_friend_model) ->
      updated_friend_model.friends |> should.equal([
        UserInfo("test", UserId("1")),
      ])
    _ -> should.fail()
  }
}

pub fn friend_error_message_is_rendered_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let target = UserInfo("test2", UserId("2"))
  let #(model, _) = userinfo.init(session, target)
  let #(updated_model, _) = userinfo.update(
    model,
    userinfo.FriendUpdated(False, Error(ApiError("既にフレンドです。"))),
  )

  userinfo.view(updated_model)
  |> element.to_string
  |> string.contains("既にフレンドです。")
  |> should.equal(True)
}

pub fn own_profile_does_not_render_friend_control_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let #(model, _) = userinfo.init(session, UserInfo("me", UserId("1")))

  userinfo.view(model)
  |> element.to_string
  |> string.contains("type=\"checkbox\"")
  |> should.equal(False)
}

pub fn loaded_friendship_status_checks_friend_control_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let target = UserInfo("friend", UserId("2"))
  let #(model, _) = userinfo.init(session, target)
  let profile = user_wrap.UserProfile(
    name: "friend",
    icon_url: "",
    is_public: True,
    bio: "",
    work_category: "",
    friendship: "FRIEND",
  )

  let #(updated_model, _) = userinfo.update(
    model,
    userinfo.ProfileLoaded(Ok(profile)),
  )

  updated_model.is_friend |> should.equal(True)
}

pub fn checking_friend_updates_friendship_label_immediately_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let target = UserInfo("friend", UserId("2"))
  let #(model, _) = userinfo.init(session, target)
  let profile = user_wrap.UserProfile(
    name: "friend",
    icon_url: "",
    is_public: True,
    bio: "",
    work_category: "",
    friendship: "NONE",
  )
  let #(loaded_model, _) = userinfo.update(
    model,
    userinfo.ProfileLoaded(Ok(profile)),
  )

  let #(updated_model, _) = userinfo.update(
    loaded_model,
    userinfo.FriendOnChecked(True),
  )

  userinfo.view(updated_model)
  |> element.to_string
  |> string.contains("フレンド状態: FRIEND")
  |> should.equal(True)

  let #(unchecked_model, _) = userinfo.update(
    updated_model,
    userinfo.FriendOnChecked(False),
  )

  userinfo.view(unchecked_model)
  |> element.to_string
  |> string.contains("フレンド状態: NONE")
  |> should.equal(True)
}

pub fn friend_page_displays_list_limit_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let model = friend.Model(
    session: session,
    friends: [],
    messages: [],
  )

  friend.view(model)
  |> element.to_string
  |> string.contains("最大50人")
  |> should.equal(True)
}

pub fn user_profile_decoder_accepts_nullable_fields_test() {
  json.parse(
    "{\"name\":\"Alice\",\"iconUrl\":null,\"isPublic\":true,\"bio\":\"alice bio\",\"workCategory\":null,\"friendship\":\"NONE\"}",
    user_wrap.user_profile_decoder(),
  )
  |> should.be_ok()
}

pub fn mypage_loaded_profile_renders_profile_fields_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let #(model, _) = mypage.init(session)
  let profile = user_wrap.MyProfile(
    name: "Alice",
    icon_url: "https://example.com/icon.png",
    is_public: True,
    bio: "alice bio",
    work_category_id: 1,
    work_category: "集中",
    email: "alice@example.com",
  )
  let #(updated_model, _) = mypage.update(
    model,
    mypage.MyProfileLoaded(Ok(profile)),
  )
  let rendered = mypage.view(updated_model) |> element.to_string

  rendered |> string.contains("名前: Alice") |> should.equal(True)
  rendered |> string.contains("src=\"https://example.com/icon.png\"") |> should.equal(True)
  rendered |> string.contains("自己紹介: alice bio") |> should.equal(True)
  rendered |> string.contains("作業カテゴリ: 集中") |> should.equal(True)
}

pub fn mypage_renders_gray_icon_fallback_when_icon_url_is_empty_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let #(model, _) = mypage.init(session)
  let profile = user_wrap.MyProfile(
    name: "Alice",
    icon_url: "",
    is_public: True,
    bio: "",
    work_category_id: 0,
    work_category: "",
    email: "alice@example.com",
  )
  let #(updated_model, _) = mypage.update(
    model,
    mypage.MyProfileLoaded(Ok(profile)),
  )
  let rendered = mypage.view(updated_model) |> element.to_string

  rendered |> string.contains("background-color: #d1d5db") |> should.equal(True)
  rendered |> string.contains("自己紹介: 未設定") |> should.equal(True)
  rendered |> string.contains("作業カテゴリ: 未設定") |> should.equal(True)
}

pub fn user_profile_renders_icon_url_as_image_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let target = UserInfo("Alice", UserId("2"))
  let #(model, _) = userinfo.init(session, target)
  let model =
    userinfo.Model(
      ..model,
      profile: Some(user_wrap.UserProfile(
        name: "Alice",
        icon_url: "https://example.com/icon.png",
        is_public: True,
        bio: "alice bio",
        work_category: "未分類",
        friendship: "FRIEND",
      )),
      loading: False,
    )

  userinfo.view(model)
  |> element.to_string
  |> string.contains("src=\"https://example.com/icon.png\"")
  |> should.equal(True)

  userinfo.view(model)
  |> element.to_string
  |> string.contains("alt=\"アイコン\"")
  |> should.equal(True)
}

pub fn user_profile_renders_fallback_when_icon_url_is_empty_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let target = UserInfo("Alice", UserId("2"))
  let #(model, _) = userinfo.init(session, target)
  let model =
    userinfo.Model(
      ..model,
      profile: Some(user_wrap.UserProfile(
        name: "Alice",
        icon_url: "",
        is_public: True,
        bio: "alice bio",
        work_category: "未分類",
        friendship: "FRIEND",
      )),
      loading: False,
    )

  userinfo.view(model)
  |> element.to_string
  |> string.contains("background-color: #d1d5db")
  |> should.equal(True)
}

pub fn user_profile_renders_gray_fallback_when_icon_loading_fails_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let target = UserInfo("Alice", UserId("2"))
  let #(model, _) = userinfo.init(session, target)
  let model = userinfo.Model(
    ..model,
    profile: Some(user_wrap.UserProfile(
      name: "Alice",
      icon_url: "https://example.com/missing-icon.png",
      is_public: True,
      bio: "alice bio",
      work_category: "未分類",
      friendship: "FRIEND",
    )),
    loading: False,
  )
  let #(failed_model, _) = userinfo.update(model, userinfo.IconLoadFailed)
  let rendered = userinfo.view(failed_model) |> element.to_string

  rendered
  |> string.contains("background-color: #d1d5db")
  |> should.equal(True)

  rendered
  |> string.contains("<img")
  |> should.equal(False)
}
