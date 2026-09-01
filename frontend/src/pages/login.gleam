import components/btn
import components/ui
import lustre/event.{on_click, on_input}
import lustre/attribute.{class, placeholder, type_, value}
import lustre/element
import lustre/effect
import gleam/list
import lustre/element/html.{button, div, text, input, h2, p, label}

import types/session.{type Session, Guest, Authenticated}
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

    ToPrivacyPolicy -> {
      #(model, effect.none())
    }

    ToTOS -> {
      #(model, effect.none())
    }
  }
}

// ---------------------------------------------------------
// login画面のView
// ---------------------------------------------------------
pub fn view(model: Model) -> element.Element(Msg) {
  case model.session {
    
    // -------------------------------------
    // 未ログイン（ゲスト）: ログインフォームを表示
    // -------------------------------------
    Guest -> {
      ui.centered_card_layout(
        // 第1引数: カードの中身（リスト）
        [
          ui.header_section("ログイン", "アカウント情報を入力してください"),
          ui.error_messages(model.messages),
          form_section(model),
          ui.divider_with_text("または"),
          btn.secondary_button("新規アカウントを作成", ToRegister)
        ],
        // 第2引数: フッター
        footer_links()
      )
    }

    // -------------------------------------
    // ログイン済みの場合
    // -------------------------------------
    Authenticated(_jwt, _user_id) -> {
      ui.centered_text_card_layout(
        [
          ui.header_section("ログイン済み", "既にログインしています"),
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
  div([class("space-y-5")], [
    ui.text_input(
      "メールアドレス",
      "email",
      "you@example.com",
      model.current_email_input,
      fn(val) { InputUpdated(Email, val) }
    ),
    ui.text_input(
      "パスワード",
      "password",
      "••••••••",
      model.current_password_input,
      fn(val) { InputUpdated(Password, val) }
    ),
    btn.primary_button("ログイン", SubmitClicked)
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
