import gleeunit/should
import pages/friend
import types/session.{Authenticated, Token}
import types/user.{FriendInfo, UserInfo, UserId}

pub fn friend_page_does_not_show_dummy_friends_before_loading_test() {
  let #(model, _) =
    friend.init(Authenticated(Token("session"), UserInfo("me", UserId("1"))))

  model.friends
    |> should.equal([])
}

pub fn friend_presence_updates_online_state_test() {
  let friend_user = UserInfo("friend", UserId("2"))
  let #(model, _) =
    friend.init(Authenticated(Token("session"), UserInfo("me", UserId("1"))))

  let #(loaded, _) =
    friend.update(
      model,
      friend.ProfilesLoaded(Ok([#(FriendInfo(friend_user, False), "")])),
    )
  let #(updated, _) =
    friend.update(loaded, friend.FriendPresenceChanged(UserId("2"), True))

  updated.friends
  |> should.equal([FriendInfo(friend_user, True)])
}
