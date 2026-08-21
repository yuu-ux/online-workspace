// ユーザー検索結果
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

import wrap/user.{search_user}

import components/userlist.{user_list_component}

pub type Model {
  Model(
    session: Session,
    search_word: String,
    search_result: List(UserInfo),
    messages: List(String)
  )
}

pub type Msg {
  ToHome
  ToMyPage
  ToUserInfo(UserInfo)
}

pub fn init(session: Session, search_word: String) -> #(Model, effect.Effect(Msg)) {
  case search_user(search_word) {
    Ok(search_result) -> {
      #(
        Model(
          session: session,
          search_word: search_word,
          search_result: search_result,
          messages: []),
        effect.none()
      )
    }
    Error(err_type) -> {
      #(
        Model(
          session: session,
          search_word: search_word,
          search_result: [],
          messages: ["検索に失敗しました"]),
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
        text("ユーザー検索結果"),
        text("ログインしてください"),
        btn.to_home_btn_component(ToHome),
      ])
    }

    session.Authenticated(jwt, user_id) -> {
      div([
        attribute.attribute("style", "padding: 20px; font-family: sans-serif;")
      ],
      [
        text("ユーザー検索結果"),

        user_list_component(model.search_result, ToUserInfo),
        button([
          on_click(ToMyPage)
        ], [text("マイページに戻る")]),
        div([], list.map(model.messages, fn (x) {div([], [text(x)])}))
      ])

    }
  }
}

