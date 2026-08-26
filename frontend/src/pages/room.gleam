import gleam/dynamic/decode
import components/btn
import lustre/event.{on_click, on_input}
import lustre/attribute
import lustre/element
import lustre/effect
import gleam/io
import gleam/list
import gleam/json

import lustre/element/html.{button, div, text, input}

import types/session.{type Session}
import types/room.{type RoomId, RoomId}

import types/user.{type UserInfo, type UserId} as user_t
import wrap/user.{GetUserInfoListAuthErr, get_all_user_info_list}
import components/userlist.{user_list_component}

pub type InputType {
  ChatMsg
}

pub type Chat {
  Chat(
    // from: String, // 誰から
    // timestamp: String,
    // comment: String, 

    msg_type: String,
    room_id: RoomId, 
    user: String,
    message: String,
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
  ToUserInfo(UserInfo)
  InputUpdated(target: InputType, str: String)
  SubmitClicked
  WsMessageReceived(String)
}

pub type RoomErr {
  DummyError
}

// ---------------------------------------------------------
// 1. FFI (JavaScriptの関数をインポート)
// ---------------------------------------------------------

/// 接続する
@external(javascript, "./../ffi/ws.js", "connect_ws")
fn do_connect(url: String, dispatch: fn(String) -> Nil) -> Nil

/// 送る
@external(javascript, "./../ffi/ws.js", "send_ws")
fn do_send(message: String) -> Nil

@external(javascript, "./../ffi/ws.js", "close_ws")
fn close_ws() -> Nil

/// チャット画面に入ったタイミングで接続を開始する
fn connect_to_server() -> effect.Effect(Msg) {
  effect.from(fn(dispatch) {
    let js_callback = fn(received_text: String) {
      dispatch(WsMessageReceived(received_text))
    }

    // JS側の接続関数を呼び出す
    do_connect("/ws-test", js_callback)
  })
}

fn room_send_msg_proc(
  user_name: String,
  room_id: RoomId,
  send_msg: String
) -> Result(Nil, RoomErr) {
  // TODO SERVER API
  Ok(
    do_send(
      chat_to_json(
        Chat(
          msg_type: "msg",
          room_id: room_id,
          user: user_name,
          message: send_msg
        )
      )
    )
  )
}

// テストデータ用構造体

// https://gleam-json.hexdocs.pm/

fn chat_to_json(chat: Chat) -> String {
  json.object([
    #("type", json.string(chat.msg_type)),
    #("room_id", json.int(case chat.room_id { RoomId(i) -> i })),
    #("user", json.string(chat.user)),
    #("message", json.string(chat.message)),
  ])
  |> json.to_string
}

fn chat_from_json(json_string: String) -> Result(Chat, json.DecodeError) {
  let chat_decoder = {
    use msg_type <- decode.field("type", decode.string)
    use room_id <- decode.field("room_id", decode.int)
    use user <- decode.field("user", decode.string)
    use message <- decode.field("message", decode.string)
    decode.success(Chat(
      msg_type: msg_type,
      room_id: RoomId(room_id),
      user: user,
      message: message
    ))
  }
  json.parse(from: json_string, using: chat_decoder)
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
          connect_to_server()
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
      close_ws()
      #(model, effect.none())
    }

    ToUserInfo(user_id) -> {
      close_ws()
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
      case room_send_msg_proc(
        "me",
        model.room_id,
        model.current_message_input) {
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

    WsMessageReceived(m) -> {
      io.println("msg from server"<> m)

      case chat_from_json(m) {
        Ok(chat) -> {
          #(Model(..model, chat_list: [chat, ..model.chat_list]), effect.none())
        }
        Error(e) -> {
          #(Model(..model, messages: ["レスポンス解析エラー"]), effect.none())
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
        btn.to_home_btn_component(ToHome)
      ])
    }

    session.Authenticated(jwt, user_id) -> {
      div([
        attribute.attribute("style", "padding: 20px; font-family: sans-serif;")
      ],
      [
        text("Room"),
        // 参加者一覧
        user_list_component(model.member_list, ToUserInfo),
        // chat欄
        div([], [
          div([],
            list.map(
              list.reverse(model.chat_list), fn (chat) { div([], [text("from" <> chat.user), text(chat.message)])} 
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
        btn.to_home_btn_component(ToHome),
        // エラー表示用
        div([], list.map(model.messages, fn (x) {div([], [text(x)])}))
      ])

    }
  }
}

