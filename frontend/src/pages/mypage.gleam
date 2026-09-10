// マイページ
import components/input
import components/btn
import gleam/dynamic/decode
import gleam/int
import gleam/option.{type Option, None, Some}
import lustre/event.{on, on_click, on_input}
import lustre/attribute
import lustre/element
import lustre/effect
import gleam/io
import gleam/list
import lustre/element/html.{button, div, h1, img, p, text}

import types/session.{type Session}
import types/user.{type UserInfo}
import wrap/api as api
import wrap/user as user_wrap

pub type InputType {
  UserName
}

pub type Model {
  Model(
    session: Session,
    current_user_name: String,
    profile: Option(user_wrap.MyProfile),
    icon_load_failed: Bool,
    messages: List(String)
  )
}

pub type Msg {
  ToHome
  ToProfile
  ToFriend
  InputUpdated(target: InputType, str: String)
  SubmitClicked
  MyProfileLoaded(Result(user_wrap.MyProfile, user_wrap.MyProfileErr))
  IconLoadFailed
}

pub fn init(session: Session) -> #(Model, effect.Effect(Msg)) {
  #(
    Model(
      session: session,
      current_user_name: "",
      profile: None,
      icon_load_failed: False,
      messages: []),
    user_wrap.get_my_profile(MyProfileLoaded)
  )
}

pub fn update(model: Model, msg: Msg) -> #(Model, effect.Effect(Msg)) {
  case msg {
    ToHome -> {
      #(model, effect.none())
    }

    ToProfile -> {
      #(model, effect.none())
    }

    ToFriend -> {
      #(model, effect.none())
    }

    // 文字が入力されたら、Modelの current_input をリアルタイムに書き換える
    InputUpdated(target, text) -> {
      let new_model = case target {
        UserName -> {
          Model(..model, current_user_name: text)
        }
      }
      #(new_model, effect.none())
    }

    // 送信ボタンが押されたら、入力内容を履歴に追加し、入力欄を空にする
    SubmitClicked -> {
      #(model, effect.none())
    }

    MyProfileLoaded(Ok(profile)) -> {
      #(
        Model(
          ..model,
          current_user_name: profile.name,
          profile: Some(profile),
          icon_load_failed: False,
          messages: [],
        ),
        effect.none(),
      )
    }

    MyProfileLoaded(Error(user_wrap.MyProfileApiErr(api.ApiError(message)))) -> {
      #(Model(..model, messages: [message]), effect.none())
    }

    IconLoadFailed -> #(Model(..model, icon_load_failed: True), effect.none())
  }
}

pub fn view (model: Model) -> element.Element(Msg) {
  let profile_view = case model.profile {
    Some(profile) -> [
      div([attribute.class("flex items-center gap-4 border-b border-gray-100 pb-5")], [
        icon_view(profile.icon_url, model.icon_load_failed),
        div([], [
          h1([attribute.class("text-2xl font-bold text-gray-900")], [text(profile.name)]),
          p([attribute.class("mt-1 text-sm text-gray-500")], [text("アカウントプロフィール")]),
        ]),
      ]),
      div([attribute.class("mt-5 grid gap-3 sm:grid-cols-2")], [
        profile_row("名前", profile.name),
        profile_row("作業カテゴリ", string_or_fallback(profile.work_category, "未設定")),
        div([attribute.class("rounded-lg bg-gray-50 p-4 sm:col-span-2")], [
          div([attribute.class("text-xs font-medium text-gray-500")], [text("自己紹介")]),
          p([attribute.class("mt-1 break-words text-sm text-gray-800")], [
            text(string_or_fallback(profile.bio, "未設定")),
          ]),
        ]),
      ]),
    ]
    None -> [
      div([attribute.class("py-10 text-center text-sm text-gray-500")], [
        text("プロフィールを読み込んでいます..."),
      ]),
    ]
  }

  case model.session {
    session.Guest -> {
      div([attribute.class("flex min-h-screen items-center justify-center bg-gray-50 p-5")], [
        div([attribute.class("rounded-xl border border-gray-200 bg-white p-8 text-center shadow-sm")], [
          h1([attribute.class("mb-3 text-xl font-bold text-gray-900")], [text("マイページ")]),
          p([attribute.class("mb-6 text-gray-600")], [text("ログインしてください")]),
          btn.to_home_btn_component(ToHome),
        ]),
      ])
    }

    session.Authenticated(_, _) -> {
      div([attribute.class("min-h-screen bg-gray-50 p-4 font-sans sm:p-6")], [
        div([attribute.class("mx-auto max-w-2xl")], [
          div([attribute.class("mb-5 flex items-center justify-between")], [
            h1([attribute.class("text-2xl font-bold text-gray-900")], [text("マイページ")]),
            p([attribute.class("text-sm text-gray-500")], [text("アカウント情報")]),
          ]),
          div([attribute.class("rounded-xl border border-gray-200 bg-white p-5 shadow-sm sm:p-8")], [
            div([attribute.class("space-y-3")], list.map(model.messages, fn (message) {
              div([attribute.class("rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700")], [
                text(message),
              ])
            })),
            div([], profile_view),
            div([attribute.class("mt-6 grid gap-3 sm:grid-cols-3")], [
              btn.to_profile_btn_component(ToProfile),
              btn.to_friend_btn_component(ToFriend),
              btn.to_home_btn_component(ToHome),
            ]),
          ]),
        ]),
      ])
    }
  }
}

fn profile_row(label: String, value: String) -> element.Element(Msg) {
  div([attribute.class("rounded-lg bg-gray-50 p-4")], [
    div([attribute.class("text-xs font-medium text-gray-500")], [text(label)]),
    div([attribute.class("mt-1 break-words text-sm font-semibold text-gray-900")], [
      text(label <> ": " <> value),
    ]),
  ])
}

fn string_or_fallback(value: String, fallback: String) -> String {
  case value {
    "" -> fallback
    _ -> value
  }
}

fn icon_view(icon_url: String, load_failed: Bool) -> element.Element(Msg) {
  case icon_url {
    "" -> icon_fallback("アイコン未設定")
    _ -> case load_failed {
      True -> icon_fallback("アイコンを表示できません")
      False ->
        img([
          attribute.src(icon_url),
          attribute.alt("アイコン"),
          attribute.class("h-20 w-20 rounded-full border border-gray-200 object-cover"),
          on("error", decode.success(IconLoadFailed)),
        ])
    }
  }
}

fn icon_fallback(label: String) -> element.Element(Msg) {
  div([
    attribute.attribute("aria-label", label),
    attribute.class("flex h-20 w-20 items-center justify-center rounded-full bg-gray-200 text-xs text-gray-500"),
  ], [])
}
