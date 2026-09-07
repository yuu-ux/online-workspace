// フレンド編集画面
import components/btn
import gleam/int
import gleam/list
import lustre/event.{on_click}
import lustre/attribute
import lustre/element
import lustre/effect
import lustre/element/html.{button, div, text}

import types/user.{type FriendInfo, type UserId, type UserInfo} as user_t
import types/session.{type Session, Guest, Authenticated}

import wrap/api.{type ApiError, ApiError}
import wrap/user.{get_friends, remove_friend}

pub type Model {
  Model(
    session: Session,
    friends: List(FriendInfo),
    messages: List(String)
  )
}

pub type Msg {
  ToHome
  ToMyPage
  ToUserInfo(UserInfo)
  FriendsLoaded(Result(List(FriendInfo), ApiError))
  RemoveFriend(UserId)
  RemoveCompleted(Result(Nil, ApiError))
}

pub fn init(session: Session) -> #(Model, effect.Effect(Msg)) {
  let initial_model = Model(session: session, friends: [], messages: [])
  case session {
    Guest -> #(initial_model, effect.none())
    Authenticated(..) -> #(initial_model, get_friends(FriendsLoaded))
  }
}


pub fn update(model: Model, msg: Msg) -> #(Model, effect.Effect(Msg)) {
  case msg {
    ToMyPage -> {
      #(model, effect.none())
    }

    ToHome -> {
      #(model, effect.none())
    }

    ToUserInfo(user_info) -> {
      #(model, effect.none())
    }

    FriendsLoaded(Ok(friends)) -> {
      #(Model(..model, friends: friends, messages: []), effect.none())
    }

    FriendsLoaded(Error(ApiError(message))) -> {
      #(Model(..model, messages: [message]), effect.none())
    }

    RemoveFriend(user_id) -> {
      #(Model(..model, messages: []), remove_friend(user_id, RemoveCompleted))
    }

    RemoveCompleted(Ok(_)) -> {
      #(model, get_friends(FriendsLoaded))
    }

    RemoveCompleted(Error(ApiError(message))) -> {
      #(Model(..model, messages: [message]), effect.none())
    }
  }
}

pub fn view (model: Model) -> element.Element(Msg) {
  case model.session {
    session.Guest -> {
      div([
        attribute.attribute("style", "padding: 20px; font-family: sans-serif;")
      ],
      [
        text("フレンド"),
        text("ログインしてください"),
        btn.to_home_btn_component(ToHome),
      ])
    }

    session.Authenticated(..) -> {
      div([
        attribute.attribute("style", "padding: 20px; font-family: sans-serif;")
      ],
      [
        text("フレンド"),
        friend_list(model.friends),
        button([
          on_click(ToMyPage)
        ], [text("マイページに戻る")]),
        div([], list.map(model.messages, fn (x) {div([], [text(x)])}))
      ])

    }
  }
}

fn friend_list(friends: List(FriendInfo)) -> element.Element(Msg) {
  div([], list.map(friends, fn(friend) {
    let user_t.FriendInfo(user, online) = friend
    div([], [
      text(user.name <> case online {
        True -> " (オンライン)"
        False -> " (オフライン)"
      }),
      button([on_click(ToUserInfo(user))], [text("フレンド詳細")]),
      button([on_click(RemoveFriend(user.user_id))], [text("フレンド解除")]),
    ])
  }))
}
