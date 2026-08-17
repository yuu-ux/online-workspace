import lustre/event.{on_click, on_input}
import lustre/attribute
import lustre/element
import lustre/effect
import gleam/io
import gleam/list
import lustre/element/html.{button, div, text, input}

import types/session.{type Session}
import types/room.{type RoomId}

import types/user.{type UserInfo, type UserId} as user_t
import wrap/user.{GetUserInfoListAuthErr, get_all_user_info_list}

pub type InputType {
  ChatMsg
}

pub type Chat {
  Chat(
    from: String, // 誰から
    timestamp: String,
    comment: String, 
  )
}

pub type Model {
  Model(
    session: Session,
    room_id: RoomId,
    member_list: List(UserInfo),
    chat_list: List(Chat), // 会話履歴表示用
    current_message_input: String,
    messages: List(String)
  )
}

pub type Msg {
  ToHome
  // ToLogin
  ToInvitation
  ToReport(UserInfo)
  InputUpdated(target: InputType, str: String)
  SubmitClicked
}

pub type RoomErr {
  DummyError
}

fn room_send_msg_proc(
  send_msg: String
) -> Result(Nil, RoomErr) {
  // TODO SERVER API
}

pub fn init(session: Session, room_id: RoomId) -> #(Model, effect.Effect(Msg)) {
  case get_all_user_info_list(session, room_id) {
    Ok(all_user_in_room) -> {
      #(
        Model(
          session: session,
          room_id: room_id,
          member_list: all_user_in_room,
          chat_list: [
            // Chat(from: "Tom", timestamp: "2026-8-12", comment: "hello"),
            // Chat(from: "Alice", timestamp: "2026-8-12", comment: "hello"),
          ],
          current_message_input: "",
          messages: []),
        effect.none()
      )
    }
    Error(e) -> {
      let err_msg = case e {
        GetUserInfoListAuthErr -> {
          "userの取得に失敗しました"
        }
      }
      #(
        Model(
          session: session,
          room_id: room_id,
          member_list: [],
          chat_list: [
            // Chat(from: "Tom", timestamp: "2026-8-12", comment: "hello"),
            // Chat(from: "Alice", timestamp: "2026-8-12", comment: "hello"),
          ],
          current_message_input: "",
          messages: [err_msg]),
        effect.none()
      )

    }
  }
}

pub fn update(model: Model, msg: Msg) -> #(Model, effect.Effect(Msg)) {
  case msg {
    ToHome -> {
      #(model, effect.none())
    }

    // ToLogin -> {
    //   #(model, effect.none())
    // }

    ToReport(user_id) -> {
      #(model, effect.none())
    }

    ToInvitation -> {
      #(model, effect.none())
    }

    // 文字が入力されたら、Modelの current_input をリアルタイムに書き換える
    InputUpdated(target, text) -> {
      let new_model = case target {
        ChatMsg -> {
          Model(..model, current_message_input: text)
        }
      }
      #(new_model, effect.none())
    }

    // 送信ボタンが押されたら、入力内容を履歴に追加し、入力欄を空にする
    SubmitClicked -> {
      case room_send_msg_proc(model.current_message_input) {
        Ok(_) -> {
          #(model, effect.none())
        }
        Error(err_type) -> {
          let err_msg = case err_type {
            DummyError -> {
              ["DummyError"]
            }
          }
          let new_model = Model(..model, messages: err_msg)
          #(new_model, effect.none())
        }
      }
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
        text("Room"),
        text("ログインしてください"),
        button([on_click(ToHome)], [text("home")]),
      ])
    }

    session.Authenticated(jwt, user_id) -> {
      div([
        attribute.attribute("style", "padding: 20px; font-family: sans-serif;")
      ],
      [
        text("Room"),
        // 参加者一覧
        div(
          [],
          [
            div([], list.map(model.member_list, fn (member) {
              div([], [text(member.name), button([on_click(ToReport(member))], [text("通報")])])
            }))
          ]
        ),
        button([on_click(ToInvitation)], [text("招待")]),
        // chat欄
        div([], [
          div([],
            list.map(
              model.chat_list, fn (chat) { div([], [text("from" <> chat.from), text(chat.comment)])} 
          )),
          input([
            on_input(InputUpdated(ChatMsg, _)),
            attribute.value(model.current_message_input)
          ]),

          // ボタンが押されたら SubmitClicked イベントを発射
          button([
            on_click(SubmitClicked)
          ], [text("send")])
        ]),
        button([on_click(ToHome)], [text("home")]),
        // エラー表示用
        div([], list.map(model.messages, fn (x) {div([], [text(x)])}))
      ])

    }
  }
}

