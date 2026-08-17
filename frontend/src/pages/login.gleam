import lustre/event.{on_click, on_input}
import lustre/attribute
import lustre/element
import lustre/effect
import gleam/io
import gleam/list
import lustre/element/html.{button, div, text, input}

import types/session.{type Session} as session_t
import wrap/session.{login_proc, DummyError}

pub type InputType {
  Email
  Password
}

pub type Model {
  Model(
    session: Session,
    current_email_input: String,
    current_password_input: String,
    messages: List(String)
  )
}

pub type Msg {
  ToHome
  ToRegister
  ToPrivacyPolicy
  ToTOS // terms of service(利用規約)

  InputUpdated(target: InputType, str: String)
  SubmitClicked
}

pub fn init(session: Session) -> #(Model, effect.Effect(Msg)) {
  #(
    Model(
      session: session,
      current_email_input: "",
      current_password_input: "",
      messages: []),
    effect.none()
  )
}

pub fn update(model: Model, msg: Msg) -> #(Model, effect.Effect(Msg)) {
  case msg {
    ToHome -> {
      #(model, effect.none())
    }

    ToRegister -> {
      #(model, effect.none())
    }

    // 文字が入力されたら、Modelの current_input をリアルタイムに書き換える
    InputUpdated(target, text) -> {
      let new_model = case target {
        Email -> { 
          Model(..model, current_email_input: text)
        }
        Password -> {
          Model(..model, current_password_input: text)
        }
      }
      #(new_model, effect.none())
    }

    // 送信ボタンが押されたら、入力内容を履歴に追加し、入力欄を空にする
    SubmitClicked -> {
      io.println("current_email_input:" <> model.current_email_input)
      io.println("current_password_input:" <> model.current_password_input)
      case login_proc(model.current_email_input, model.current_password_input) {
        Ok(session) -> {
          #(Model(..model, session: session), effect.from(fn (dispatch) { dispatch(ToHome) }))
        }
        Error(err_type) -> {
          let err_msg = case err_type {
            DummyError -> {
              ["DummyError"]
            }
          }
          let new_model = Model(
              ..model,
              messages: err_msg
            )
          #(new_model, effect.none())
        }
      }
    }

    _ -> {
      io.println("hello")
      #(Model(..model, session: session_t.Guest), effect.none())
    }
  }
}

pub fn view (model: Model) -> element.Element(Msg) {
  case model.session {
    session_t.Guest -> {
      div([
        attribute.attribute("style", "padding: 20px; font-family: sans-serif;")
      ],
      [
        text("Login"),
        div([], [
          input([
            on_input(InputUpdated(Email, _)),
            attribute.value(model.current_email_input)
          ]),
          input([
            on_input(InputUpdated(Password, _)),
            attribute.value(model.current_password_input)
          ]),

          // ボタンが押されたら SubmitClicked イベントを発射
          button([
            on_click(SubmitClicked)
          ], [text("login")])
        ]),
        button([on_click(ToRegister)], [text("register")]),
        button([on_click(ToPrivacyPolicy)], [text("プライバシーポリシー")]),
        button([on_click(ToTOS)], [text("利用規約")]),
        div([], list.map(model.messages, fn (x) {div([], [text(x)])}))
      ])
    }

    session_t.Authenticated(jwt, user_id) -> {
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

