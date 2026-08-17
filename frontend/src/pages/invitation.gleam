// 招待管理画面
import lustre/event.{on_click, on_input}
import lustre/attribute
import lustre/element
import lustre/effect
import gleam/io
import lustre/element/html.{button, div, text, option, select, input}

import types/session.{type Session}
import types/room.{type RoomId}

pub type InputType {
  UserName
}

pub type Model {
  Model(
    session: Session,
    room_id: RoomId,
    current_user_name: String,
    messages: List(String)
  )
}

pub type Msg {
  ToHome
  BackToRoom
  InputUpdated(target: InputType, str: String)
  SubmitClicked
  SearchClicked
}

/// 通報するユーザーを引数に設定する
pub fn init(session: Session, room_id: RoomId) -> #(Model, effect.Effect(Msg)) {
  #(
    Model(
      session: session,
      room_id: room_id,
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

    BackToRoom -> {
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
      // io.println("current_user_name:" <> model.current_user_name)
    }

    // 検索ボタンがクリックされた
    SearchClicked -> {

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
        text("招待管理"),
        text("ログインしてください"),
        button([on_click(ToHome)], [text("home")]),
      ])
    }

    session.Authenticated(jwt, user_id) -> {
      div([
        attribute.attribute("style", "padding: 20px; font-family: sans-serif;")
      ],
      [
        text("招待管理"),

        input([on_input(InputUpdated(UserName, _)), attribute.value(model.current_user_name)]),
        button([on_click(SearchClicked)], [text("検索")]),
        // 検索の結果近い名前のuserを表示
        button([on_click(SubmitClicked)], [text("招待URLを発行")]),
        button([on_click(BackToRoom)], [text("ルームに戻る")])
      ])
    }
  }
}

