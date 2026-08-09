import lustre/event.{on_click, on_input}
import lustre/attribute
import lustre/element
import lustre/effect
import lustre
import gleam/io
import gleam/list
import lustre/element/html.{button, div, h1, h3, hr, span, style, text, input}

import session/session.{type Session}

pub type Model {
  Model(
    session: Session,
    current_input: String,
    messages: List(String)
  )
}

pub type Msg {
  ToHome
  ToRegister
  ToPrivacyPolicy
  ToTOS // terms of service(利用規約)

  InputUpdated(String)
  SubmitClicked
}

pub fn init(session: Session) -> #(Model, effect.Effect(Msg)) {
  #(
    Model(session: session, current_input: "", messages: []),
    effect.none()
  )
}

pub fn update(model: Model, msg: Msg) -> #(Model, effect.Effect(Msg)) {
  case msg {
    ToHome -> {
      io.println("hello")
      #(Model(..model, session: session.Authenticated(jwt: "sample swt", user_id: "FOO")), effect.none())
    }

    // 文字が入力されたら、Modelの current_input をリアルタイムに書き換える
    InputUpdated(text) -> {
      let new_model = Model(..model, current_input: text)
      #(new_model, effect.none())
    }

    // 送信ボタンが押されたら、入力内容を履歴に追加し、入力欄を空にする
    SubmitClicked -> {
      case model.current_input {
        "" -> #(model, effect.none())
        text -> {
          let new_messages = list.append(model.messages, [text])
          
          let new_model = Model(
            ..model,
            current_input: "",
            messages: new_messages
          )
          #(new_model, effect.none())
        }
      }
    }

    _ -> {
      io.println("hello")
      #(Model(..model, session: session.Guest), effect.none())
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
        text("Login"),
        div([], [
          input([
            on_input(InputUpdated),
            attribute.value(model.current_input)
          ]),

          // ボタンが押されたら SubmitClicked イベントを発射
          button([
            on_click(SubmitClicked)
          ], [html.text("送信")])
        ]),
        button([on_click(ToHome)], [text("login")]),
        button([on_click(ToRegister)], [text("register")]),
        button([on_click(ToPrivacyPolicy)], [text("プライバシーポリシー")]),
        button([on_click(ToTOS)], [text("利用規約")]),
      ])
    }
    session.Authenticated(jwt, user_id) -> {
      div([
        attribute.attribute("style", "padding: 20px; font-family: sans-serif;")
      ],
      [
        text("Login"),
        text("既にログインしています"),
        button([on_click(ToHome)], [text("home")]),
        button([on_click(ToPrivacyPolicy)], [text("プライバシーポリシー")]),
        button([on_click(ToTOS)], [text("利用規約")]),
      ])
    }
  }
}


