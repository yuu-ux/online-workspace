// ユーザー検索結果
import components/btn
import gleam/list
import lustre/event.{on_click}
import lustre/attribute
import lustre/element
import lustre/effect
import lustre/element/html.{button, div, text}

import types/user.{type UserInfo} as user_t
import types/session.{type Session, Guest, Authenticated}

import wrap/user.{search_user}
import wrap/api.{type ApiError, ApiError}

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
  SearchLoaded(Result(List(UserInfo), ApiError))
}

pub fn init(session: Session, search_word: String) -> #(Model, effect.Effect(Msg)) {
  case session {
    Guest ->
      #(
        Model(
          session: session,
          search_word: search_word,
          search_result: [],
          messages: ["ログインしてください"]),
        effect.none(),
      )
    Authenticated(..) ->
      #(
        Model(
          session: session,
          search_word: search_word,
          search_result: [],
          messages: []),
        search_user(search_word, SearchLoaded),
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

    SearchLoaded(Ok(search_result)) -> {
      #(Model(..model, search_result: search_result, messages: []), effect.none())
    }
    SearchLoaded(Error(ApiError(message))) -> {
      #(Model(..model, search_result: [], messages: [message]), effect.none())
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

    session.Authenticated(_, _) -> {
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

