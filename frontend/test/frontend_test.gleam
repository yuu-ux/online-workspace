import gleeunit
import gleeunit/should
import frontend
import gleam/json
import lustre/element
import gleam/string
import pages/friend
import components/userinfo
import wrap/api.{ApiError}
import types/session.{Authenticated, Token}
import types/user.{UserInfo, UserId}
import wrap/user as user_wrap

pub fn main() {
  gleeunit.main()
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
