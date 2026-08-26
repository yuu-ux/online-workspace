import components/btn
import lustre/event.{on_click, on_input}
import lustre/attribute.{class}
import lustre/element
import lustre/effect
import gleam/io
import gleam/list
import lustre/element/html.{button, div, text, input, p}

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

import components/ui // ※後で components/ui 等に変更予定とのこと

// ---------------------------------------------------------
// Register画面のView
// ---------------------------------------------------------
pub fn view(model: Model) -> element.Element(Msg) {
  case model.session {
    
    // -------------------------------------
    // 未ログイン（ゲスト）: 登録フォームを表示
    // -------------------------------------
    session.Guest -> {
      ui.centered_card_layout(
        [
          ui.header_section("アカウント登録", "新しいワークスペースに参加しましょう"),
          ui.error_messages(model.messages),
          form_section(model),
          ui.divider_with_text("または"),
          btn.secondary_button("既存のアカウントでログイン", ToLogin)
        ],
        footer_links()
      )
    }

    // -------------------------------------
    // ログイン済みの場合
    // -------------------------------------
    session.Authenticated(_jwt, _user_id) -> {
      ui.centered_text_card_layout(
        [
          ui.header_section("登録完了", "既にログインしています"),
          p([class("text-gray-600")], [text("ワークスペースへ移動して作業を始めましょう。")]),
          btn.primary_button("ホームへ進む", ToHome)
        ],
        footer_links()
      )
    }
  }
}

/// 入力フォーム本体のセクション
fn form_section(model: Model) -> element.Element(Msg) {
  div([class("space-y-4")], [ // 入力項目が多いので少し隙間を詰める(space-y-4)

    // ユーザー名
    ui.text_input(
      "ユーザー名",
      "text",
      "例: Gleam Taro",
      model.current_username_input,
      fn(val) { InputUpdated(UserName, val) }
    ),

    // メールアドレス
    ui.text_input(
      "メールアドレス",
      "email",
      "you@example.com",
      model.current_email_input,
      fn(val) { InputUpdated(Email, val) }
    ),

    // パスワード
    ui.text_input(
      "パスワード",
      "password",
      "8文字以上",
      model.current_password_input,
      fn(val) { InputUpdated(Password, val) }
    ),

    // パスワード（確認用）
    ui.text_input(
      "パスワード（確認用）",
      "password",
      "もう一度入力してください",
      model.current_password_confirm_input,
      fn(val) { InputUpdated(PasswordConfirm, val) }
    ),

    // 登録ボタン (要素間に少し余白をもたせるためのラッピング)
    div([class("pt-2")], [
      btn.primary_button("アカウントを作成する", SubmitClicked)
    ])
  ])
}

/// フッターリンク群
fn footer_links() -> element.Element(Msg) {
  div(
    [class("mt-8 flex flex-wrap justify-center gap-6 text-sm text-gray-500")],
    [
      btn.to_home_btn_component(ToHome),
      btn.to_privacypolicy_btn_component(ToPrivacyPolicy),
      btn.to_tos_btn_component(ToTOS),
    ]
  )
}

