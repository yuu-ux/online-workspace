// フレンド編集画面
import components/btn
import gleam/int
import gleam/list
import lustre/event.{on_click}
import lustre/attribute
import lustre/element
import lustre/effect
import gleam/io
import lustre/element/html.{button, div, h1, h2, p, text}

import types/user.{type UserInfo} as user_t
import types/session.{type Session, Guest, Authenticated}

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
    Guest -> {
      #(Model(
          session: session,
          friends: [],
          messages: ["ログインしてください"]),
        effect.none(),
      )
    }
    Authenticated(..) -> {
      #(Model(
          session: session,
          friends: [],
          messages: []),
        get_friends(FriendsLoaded),
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
      div([attribute.class("flex min-h-screen items-center justify-center bg-gray-50 p-5")], [
        div([attribute.class("rounded-xl border border-gray-200 bg-white p-8 text-center shadow-sm")], [
          h1([attribute.class("mb-3 text-xl font-bold text-gray-900")], [text("フレンド管理")]),
          p([attribute.class("mb-6 text-gray-600")], [text("ログインしてください")]),
          btn.to_home_btn_component(ToHome),
        ]),
      ])
    }

    session.Authenticated(_, _) -> {
      div([attribute.class("min-h-screen bg-gray-50 p-4 font-sans sm:p-6")], [
        div([attribute.class("mx-auto max-w-3xl")], [
          div([attribute.class("mb-5 flex flex-wrap items-end justify-between gap-3")], [
            div([], [
              h1([attribute.class("text-2xl font-bold text-gray-900")], [text("フレンド管理")]),
              p([attribute.class("mt-1 text-sm text-gray-500")], [
                text("登録済みのフレンドを確認できます（最大50人）"),
              ]),
            ]),
            span_count(model.friends),
          ]),
          div([attribute.class("mb-4 space-y-2")], list.map(model.messages, fn(message) {
            div([attribute.class("rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700")], [
              text(message),
            ])
          })),
          friend_list(model.friends),
          div([attribute.class("mt-5 flex flex-col gap-3 sm:flex-row")], [
            button([
              attribute.class("flex-1 rounded-lg border border-gray-300 bg-white px-4 py-2.5 text-sm font-semibold text-gray-700 transition hover:bg-gray-50"),
              on_click(ToMyPage),
            ], [text("マイページに戻る")]),
            button([
              attribute.class("flex-1 rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-blue-700"),
              on_click(ToHome),
            ], [text("ホームへ")]),
          ]),
        ]),
      ])
    }
  }
}

fn span_count(friends: List(UserInfo)) -> element.Element(Msg) {
  div([attribute.class("rounded-full bg-blue-50 px-3 py-1 text-sm font-semibold text-blue-700")], [
    text(int.to_string(list.length(friends)) <> " / 50人"),
  ])
}

fn friend_list(friends: List(UserInfo)) -> element.Element(Msg) {
  case friends {
    [] ->
      div([attribute.class("rounded-xl border border-dashed border-gray-300 bg-white p-10 text-center")], [
        h2([attribute.class("text-lg font-semibold text-gray-900")], [text("フレンドがいません")]),
        p([attribute.class("mt-2 text-sm text-gray-500")], [
          text("ユーザーを検索してフレンドに追加すると、ここに表示されます。"),
        ]),
      ])
    _ ->
      div([attribute.class("rounded-xl border border-gray-200 bg-white p-5 shadow-sm")], [
        div([attribute.class("space-y-3")], list.map(friends, fn(friend) {
          div([attribute.class("flex items-center justify-between rounded-lg bg-gray-50 p-3")], [
            div([attribute.class("font-medium text-gray-900")], [text(friend.name)]),
            button([
              attribute.class("rounded-md border border-gray-300 bg-white px-3 py-1.5 text-xs font-medium text-gray-700 transition hover:bg-gray-100"),
              on_click(ToUserInfo(friend)),
            ], [text("詳細を見る")]),
          ])
        })),
      ])
  }
}
