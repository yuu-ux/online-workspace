import gleeunit/should
import pages/friend
import types/session.{Authenticated, Token}
import types/user.{UserInfo, UserId}

pub fn friend_page_does_not_show_dummy_friends_before_loading_test() {
  let #(model, _) =
    friend.init(Authenticated(Token("session"), UserInfo("me", UserId("1"))))

  model.friends
  |> should.equal([])
}
