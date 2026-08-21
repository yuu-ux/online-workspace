// マイページ
import components/btn
import gleam/int
import lustre/event.{on_click, on_input}
import lustre/attribute
import lustre/element
import lustre/effect
import gleam/io
import gleam/list
import lustre/element/html.{button, div, text, input, textarea, select, option}

import types/session.{type Session}
import types/user.{type UserInfo}

pub type InputType {
  UserName
}

pub type Model {
  Model(
    session: Session,
    current_user_name: String,
    messages: List(String)
  )
}

pub type Msg {
  ToHome
  ToProfile
  ToFriend
  ToHistory
  InputUpdated(target: InputType, str: String)
  SubmitClicked
}

pub fn init(session: Session) -> #(Model, effect.Effect(Msg)) {
  #(
    Model(
      session: session,
      current_user_name: "",
      messages: []),
    effect.none()
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

    ToHistory -> {
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

        button([
          on_click(ToHome)
        ], [text("ホーム画面")]),
        button([
          on_click(ToFriend)
        ], [text("フレンド管理")]),
        button([
          on_click(ToProfile)
        ], [text("プロファイル編集")]),
        button([
          on_click(ToHistory)
        ], [text("作業履歴")]),
        input([
          on_input(InputUpdated(UserName, _)),
          attribute.value(model.current_user_name)
        ]),
        div([], [
          // ボタンが押されたら SubmitClicked イベントを発射
          button([
            on_click(SubmitClicked)
          ], [text("検索")])
        ]),
        div([], list.map(model.messages, fn (x) {div([], [text(x)])}))
      ])

    }
  }
}

