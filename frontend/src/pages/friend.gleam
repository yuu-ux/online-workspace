// フレンド編集画面
import components/btn
import gleam/int
import gleam/list
import lustre/event.{on_click}
import lustre/attribute
import lustre/element
import lustre/effect
import gleam/io
import lustre/element/html.{button, div, h1, h3, hr, span, style, text}

import types/user.{type UserId, type UserInfo} as user_t
import types/session.{type Session,type Token, Guest, Authenticated}

import components/userlist.{user_list_component}

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
}

pub fn init(session: Session) -> #(Model, effect.Effect(Msg)) {
  case get_friends(session) {
    Ok(friends) -> {
      #(Model(
          session: session,
          friends: friends,
          messages: []),
        effect.none()
      )
    }
    Error(err_type) -> {
      #(Model(
          session: session,
          friends: [],
          messages: ["フレンドの取得に失敗しました"]),
        effect.none()
      )
    }
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

    session.Authenticated(jwt, user_id) -> {
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

