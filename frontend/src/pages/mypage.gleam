// マイページ
import components/input
import components/btn
import components/ui
import pages/friend
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
    friends: friend.Model,
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
  FriendMsg(friend.Msg)
}

pub fn init(session: Session) -> #(Model, effect.Effect(Msg)) {
  let #(friends, friends_effect) = friend.init(session)
  #(
    Model(
      session: session,
      current_user_name: "",
      profile: None,
      icon_load_failed: False,
      friends: friends,
      messages: []),
    effect.batch([user_wrap.get_my_profile(MyProfileLoaded), effect.map(friends_effect, FriendMsg)])
  )
}

pub fn update(model: Model, msg: Msg) -> #(Model, effect.Effect(Msg)) {
  case msg {
    FriendMsg(message) -> {
      let #(friends, next_effect) = friend.update(model.friends, message)
      #(Model(..model, friends: friends), effect.map(next_effect, FriendMsg))
    }
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
      div([attribute.class("flex flex-wrap items-center gap-5 pb-6")], [
        icon_view(profile.icon_url, model.icon_load_failed),
        div([attribute.class("min-w-0 flex-1")], [
          h1([attribute.class("text-3xl font-semibold tracking-tight text-gray-900")], [text(profile.name)]),
          p([attribute.class("mt-2 break-words whitespace-pre-wrap text-sm leading-7 text-[#4c4841]")], [
            text(string_or_fallback(profile.bio, "自己紹介は未設定です")),
          ]),
        ]),
        button([attribute.class(btn.navigation_button_classes() <> " px-3 py-2 text-xs"), on_click(ToProfile)], [text("プロフィール編集")]),
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
      div([attribute.class("flex min-h-screen items-center justify-center bg-[#f7f5f0] p-5")], [
        div([attribute.class("p-8 text-center")], [
          h1([attribute.class("mb-3 text-xl font-bold text-gray-900")], [text("マイページ")]),
          p([attribute.class("mb-6 text-gray-600")], [text("ログインしてください")]),
          btn.to_home_btn_component(ToHome),
        ]),
      ])
    }

    session.Authenticated(_, _) -> {
      div([attribute.class("min-h-screen bg-[#f7f5f0] p-4 font-sans sm:p-6")], [
        div([attribute.class("mb-6")], [
          button([attribute.class("text-sm font-medium text-[#58745a] hover:underline"), on_click(ToHome)], [text("← ホームへ")]),
        ]),
        div([attribute.class("mx-auto max-w-2xl")], [
          div([attribute.class("mb-5 flex items-center justify-between")], [
            h1([attribute.class("text-2xl font-semibold text-gray-900")], [text("マイページ")]),
          ]),
          div([attribute.class("border-t border-[#dedbd2] py-6")], [
            ui.error_messages(model.messages),
            div([], profile_view),
            div([attribute.class("mt-8 border-t border-[#dedbd2] pt-6")], [
              friend.section_view(model.friends) |> element.map(FriendMsg),
            ]),
          ]),
        ]),
      ])
    }
  }
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
          attribute.class("h-24 w-24 shrink-0 rounded-full border border-gray-200 object-cover"),
          on("error", decode.success(IconLoadFailed)),
        ])
    }
  }
}

fn icon_fallback(label: String) -> element.Element(Msg) {
  div([
    attribute.attribute("aria-label", label),
    attribute.class("flex h-24 w-24 shrink-0 items-center justify-center rounded-full bg-gray-200 text-xs text-gray-500"),
  ], [])
}
