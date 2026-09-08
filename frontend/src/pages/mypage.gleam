// マイページ
import components/input
import components/btn
import lustre/event.{on_click, on_input}
import lustre/attribute
import lustre/element
import lustre/effect
import gleam/list
import lustre/element/html.{div, text}

import types/session.{type Session}
import wrap/api.{type ApiError, ApiError}
import wrap/user.{type MyProfile, get_my_profile}

pub type InputType {
  UserName
}

pub type Model {
  Model(
    session: Session,
    display_name: String,
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
  MyProfileLoaded(Result(MyProfile, ApiError))
}

pub fn init(session: Session) -> #(Model, effect.Effect(Msg)) {
  case session {
    session.Guest ->
      #(
        Model(
          session: session,
          display_name: "",
          current_user_name: "",
          messages: [],
        ),
        effect.none(),
      )
    session.Authenticated(..) ->
      #(
        Model(
          session: session,
          display_name: "",
          current_user_name: "",
          messages: [],
        ),
        get_my_profile(MyProfileLoaded),
      )
  }
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
      #(model, effect.none())
    }

    MyProfileLoaded(Ok(profile)) -> {
      #(
        Model(..model, display_name: profile.name),
        effect.none(),
      )
    }
    MyProfileLoaded(Error(ApiError(message))) -> {
      #(Model(..model, messages: [message]), effect.none())
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

    session.Authenticated(_, _) -> {
      div([
        attribute.attribute("style", "padding: 20px; font-family: sans-serif;")
      ],
      [
        text("MyPage"),
        div([], [text("ようこそ " <> model.display_name)]),

        btn.to_home_btn_component(ToHome),
        btn.to_friend_btn_component(ToFriend),
        btn.to_profile_btn_component(ToProfile),
        btn.to_history_btn_component(ToHistory),
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
