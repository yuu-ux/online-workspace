import lustre/event.{on_click, on_input}
import lustre/attribute
import lustre/element
import lustre/effect
import gleam/io
import gleam/list
import lustre/element/html.{button, div, text, input}

import types/session.{type Session}

pub type InputType {
  UserName
  Email
  Password
  PasswordConfirm
}

pub type Model {
  Model(
    session: Session,
    current_username_input: String,
    current_email_input: String,
    current_password_input: String,
    current_password_confirm_input: String,
    messages: List(String)
  )
}

pub type Msg {
  ToHome
  ToLogin
  ToPrivacyPolicy
  ToTOS // terms of service(利用規約)

  InputUpdated(target: InputType, str: String)
  SubmitClicked
}

pub type RegisterErr {
  PasswordMismatch
}

fn register_proc(username: String, email: String, password: String, password_confirm: String) -> Result(Nil, RegisterErr) {
  // TODO SERVER API
  case password == password_confirm {
    True -> {
      Ok(Nil)
    }
    False -> {
      Error(PasswordMismatch)
    }
  }
}

pub fn init(session: Session) -> #(Model, effect.Effect(Msg)) {
  #(
    Model(
      session: session,
      current_username_input: "",
      current_email_input: "",
      current_password_input: "",
      current_password_confirm_input: "",
      messages: []),
    effect.none()
  )
}

pub fn update(model: Model, msg: Msg) -> #(Model, effect.Effect(Msg)) {
  case msg {
    ToHome -> {
      #(model, effect.none())
    }

    ToLogin -> {
      #(model, effect.none())
    }

    // 文字が入力されたら、Modelの current_input をリアルタイムに書き換える
    InputUpdated(target, text) -> {
      let new_model = case target {
        UserName -> {
          Model(..model, current_username_input: text)
        }
        Email -> { 
          Model(..model, current_email_input: text)
        }
        Password -> {
          Model(..model, current_password_input: text)
        }
        PasswordConfirm -> {
          Model(..model, current_password_confirm_input: text)
        }
      }
      #(new_model, effect.none())
    }

    // 送信ボタンが押されたら、入力内容を履歴に追加し、入力欄を空にする
    SubmitClicked -> {
      // io.println("current_email_input:" <> model.current_email_input)
      // io.println("current_password_input:" <> model.current_password_input)
      case register_proc(
        model.current_username_input,
        model.current_email_input,
        model.current_password_input,
        model.current_password_confirm_input,
      ) {
        Ok(_) -> {
          #(model, effect.from(fn (dispatch) { dispatch (ToLogin )}))
        }
        Error(err_msg) -> {
          case err_msg {
            PasswordMismatch -> {
              #(Model(..model, messages: ["パスワードを確認してください"]), effect.none())
            }
          }
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
        text("Register"),
        div([], [
          input([
            on_input(InputUpdated(UserName, _)),
            attribute.value(model.current_username_input)
          ]),
          input([
            on_input(InputUpdated(Email, _)),
            attribute.value(model.current_email_input)
          ]),
          input([
            on_input(InputUpdated(Password, _)),
            attribute.value(model.current_password_input)
          ]),
          input([
            on_input(InputUpdated(PasswordConfirm, _)),
            attribute.value(model.current_password_confirm_input)
          ]),

          // ボタンが押されたら SubmitClicked イベントを発射
          button([
            on_click(SubmitClicked)
          ], [text("register")])
        ]),
        button([on_click(ToLogin)], [text("back to login")]),
        button([on_click(ToHome)], [text("home")]),
        button([on_click(ToPrivacyPolicy)], [text("プライバシーポリシー")]),
        button([on_click(ToTOS)], [text("利用規約")]),
        div([], list.map(model.messages, fn (x) {div([], [text(x)])}))
      ])
    }
    session.Authenticated(jwt, user_id) -> {
      div([
        attribute.attribute("style", "padding: 20px; font-family: sans-serif;")
      ],
      [
        text("Register"),
        text("既にログインしています"),
        button([on_click(ToHome)], [text("home")]),
        button([on_click(ToPrivacyPolicy)], [text("プライバシーポリシー")]),
        button([on_click(ToTOS)], [text("利用規約")]),
      ])
    }
  }
}

