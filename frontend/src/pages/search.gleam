// ユーザー検索結果
import components/btn
import components/ui
import gleam/list
import gleam/string
import lustre/event.{on_click}
import lustre/attribute
import lustre/element
import lustre/effect
import lustre/element/html.{button, div, text}

import types/user.{type UserInfo}
import types/session.{type Session} as session_t

import wrap/user as user_wrap
import wrap/api as api

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
  ToFriend
  ToUserInfo(UserInfo)
  SearchLoaded(Result(List(UserInfo), user_wrap.SearchErr))
}

pub fn init(session: Session, search_word: String) -> #(Model, effect.Effect(Msg)) {
  case string.trim(search_word) {
    "" -> #(
      Model(
        session: session,
        search_word: search_word,
        search_result: [],
        messages: ["検索文字を入力してください"],
      ),
      effect.none(),
    )
    _ -> {
      #(
        Model(
          session: session,
          search_word: search_word,
          search_result: [],
          messages: []),
        user_wrap.search_user(search_word, SearchLoaded),
      )
    }
  }
}

pub fn update(model: Model, msg: Msg) -> #(Model, effect.Effect(Msg)) {
  case msg {
    ToFriend -> {
      #(model, effect.none())
    }

    ToHome -> {
      #(model, effect.none())
    }

    ToUserInfo(user_info) -> {
      #(model, effect.none())
    }

    SearchLoaded(Ok(search_result)) -> {
      #(Model(..model, search_result: search_result, messages: []), effect.none())
    }

    SearchLoaded(Error(user_wrap.SearchApiErr(api.ApiError(message)))) -> {
      #(Model(..model, search_result: [], messages: [message]), effect.none())
    }
  }
}

pub fn view (model: Model) -> element.Element(Msg) {
  case model.session {
    session_t.Guest -> {
      div([
        attribute.class("min-h-screen bg-[#f7f5f0] p-5 text-slate-800")
      ],
      [
        text("ユーザー検索結果"),
        text("ログインしてください"),
        btn.to_home_btn_component(ToHome),
      ])
    }

    session_t.Authenticated(_, _) -> {
      div([
        attribute.class("min-h-screen bg-[#f7f5f0] px-4 py-5 sm:px-8")
      ],
      [
        button([
          on_click(ToFriend), attribute.class("mb-8 text-sm text-[#58745a] hover:underline")
        ], [text("← マイページに戻る")]),
        div([attribute.class("mx-auto max-w-3xl")], [
          div([attribute.class("mb-6 text-2xl font-semibold text-slate-900")], [text("ユーザー検索結果")]),
          div([attribute.class("divide-y divide-[#dedbd2] border-y border-[#dedbd2]")], list.map(model.search_result, fn(person) {
            div([attribute.class("flex items-center justify-between gap-4 py-4")], [
              div([attribute.class("min-w-0 break-words font-medium text-slate-800")], [text(person.name)]),
              button([on_click(ToUserInfo(person)), attribute.class("shrink-0 rounded-md border border-[#d8d1c5] px-3 py-2 text-sm text-[#58745a]")], [text("詳細を見る")]),
            ])
          })),
          div([attribute.class("py-4 text-sm text-[#6f6a61]")], case model.search_result { [] -> [text("該当するユーザーはいません")] _ -> [] }),
        ]),
        ui.error_messages(model.messages)
      ])

    }
  }
}
