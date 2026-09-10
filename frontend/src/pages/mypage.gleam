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
import lustre/element/html.{button, div, img, text, input, textarea, select, option}

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
      div([], [text("名前: " <> profile.name)]),
      div([], [text("アイコン:")]),
      icon_view(profile.icon_url, model.icon_load_failed),
      div([], [text("自己紹介: " <> string_or_fallback(profile.bio, "未設定"))]),
      div([], [text("作業カテゴリ: " <> string_or_fallback(profile.work_category, "未設定"))]),
    ]
    None -> [div([], [text("プロフィールを読み込み中...")])]
  }

  case model.session {
    session.Guest -> {
      div([
        attribute.attribute("style", "padding: 20px; font-family: sans-serif;")
      ],
      [
        text("MyPage"),
        text("ログインしてください"),
        btn.to_home_btn_component(ToHome)
      ])
    }

    session.Authenticated(jwt, user_id) -> {
      div([
        attribute.attribute("style", "padding: 20px; font-family: sans-serif;")
      ],
      [
        text("MyPage"),
        div([], profile_view),

        btn.to_home_btn_component(ToHome),
        btn.to_friend_btn_component(ToFriend),
        btn.to_profile_btn_component(ToProfile),
        // input([
        //   on_input(InputUpdated(UserName, _)),
        //   attribute.value(model.current_user_name)
        // ]),
        input.normal_input(InputUpdated(UserName, _), model.current_user_name),

        div([], [
          // ボタンが押されたら SubmitClicked イベントを発射
          btn.search_btn_component(SubmitClicked),
        ]),
        div([], list.map(model.messages, fn (x) {div([], [text(x)])}))
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
          on("error", decode.success(IconLoadFailed)),
          attribute.attribute(
            "style",
            "width: 64px; height: 64px; object-fit: cover; border-radius: 50%;",
          ),
        ])
    }
  }
}

fn icon_fallback(label: String) -> element.Element(Msg) {
  div([
    attribute.attribute("aria-label", label),
    attribute.attribute(
      "style",
      "width: 64px; height: 64px; border-radius: 50%; background-color: #d1d5db;",
    ),
  ], [])
}
