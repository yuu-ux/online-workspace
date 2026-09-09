// フレンド編集画面
import components/btn
import gleam/list
import lustre/event.{on_click}
import lustre/attribute
import lustre/element
import lustre/effect
import lustre/element/html.{button, div, text}

import types/user.{type UserInfo} as user_t
import types/session.{type Session, Guest, Authenticated}

import components/userlist.{user_list_component}

import wrap/api.{type ApiError, ApiError}
import wrap/user.{get_friends}

pub type Model {
  Model(
    session: Session,
    friends: List(UserInfo),
    messages: List(String)
  )
}

pub type Msg {
  ToHome
  ToMyPage
  ToUserInfo(UserInfo)
  FriendsLoaded(Result(List(UserInfo), ApiError))
}

pub fn init(session: Session) -> #(Model, effect.Effect(Msg)) {
  case session {
    Guest ->
      #(Model(
          session: session,
          friends: [],
          messages: ["ログインしてください"]),
        effect.none()
      )
    Authenticated(..) ->
      #(Model(
          session: session,
          friends: [],
          messages: []),
        get_friends(FriendsLoaded)
      )
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

    ToUserInfo(_user_info) -> {
      #(model, effect.none())
    }

    FriendsLoaded(Ok(friends)) -> {
      #(Model(..model, friends: friends, messages: []), effect.none())
    }
    FriendsLoaded(Error(ApiError(message))) -> {
      #(Model(..model, friends: [], messages: [message]), effect.none())
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

    session.Authenticated(_, _) -> {
      div([
        attribute.attribute("style", "padding: 20px; font-family: sans-serif;")
      ],
      [
        text("フレンド"),
        user_list_component(model.friends, ToUserInfo),
        button([
          on_click(ToMyPage)
        ], [text("マイページに戻る")]),
        div([], list.map(model.messages, fn (x) {div([], [text(x)])}))
      ])

    }
  }
}

