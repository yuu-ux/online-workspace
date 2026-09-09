import components/btn
import gleam/list
import gleam/string
import lustre/event.{on_click, on_input}
import lustre/attribute
import lustre/element
import lustre/effect

import lustre/element/html.{button, div, text, input}

import types/session.{type Session}
import types/room.{type RoomId} as room_t
import types/user.{type UserInfo} as user_t
import wrap/api.{type ApiError, ApiError}

import wrap/room.{
  type Chat,
  chat_from_json,
  close_ws,
  connect_to_server,
  create_message,
  get_messages,
  get_room_members,
  join_room,
}

import components/userlist.{user_list_component}

pub type InputType {
  ChatMsg
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
  ToUserInfo(UserInfo)
  InputUpdated(target: InputType, str: String)
  SubmitClicked
  JoinedRoom(Result(Nil, ApiError))
  MembersLoaded(Result(List(UserInfo), ApiError))
  MessagesLoaded(Result(List(Chat), ApiError))
  MessagePosted(Result(Nil, ApiError))
  WsMessageReceived(String)
}

pub fn init(session: Session, room_id: RoomId) -> #(Model, effect.Effect(Msg)) {
  case session {
    session.Guest -> {
      #(
        Model(
          session: session,
          room_id: room_id,
          member_list: [],
          chat_list: [],
          current_message_input: "",
          messages: ["ログインしてください"],
        ),
        effect.none(),
      )
    }
    session.Authenticated(..) -> {
      #(
        Model(
          session: session,
          room_id: room_id,
          member_list: [],
          chat_list: [],
          current_message_input: "",
          messages: [],
        ),
        join_room(room_id, JoinedRoom),
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

    ToUserInfo(_user_info) -> {
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

    JoinedRoom(_) -> {
      #(
        model,
        get_room_members(model.room_id, MembersLoaded),
      )
    }

    MembersLoaded(Ok(members)) -> {
      #(
        Model(..model, member_list: members, messages: []),
        get_messages(model.room_id, MessagesLoaded),
      )
    }

    MembersLoaded(Error(ApiError(message))) -> {
      #(Model(..model, messages: [message]), effect.none())
    }

    MessagesLoaded(Ok(messages)) -> {
      #(
        Model(..model, chat_list: messages),
        connect_to_server(model.room_id, WsMessageReceived),
      )
    }

    MessagesLoaded(Error(ApiError(message))) -> {
      #(Model(..model, messages: [message]), effect.none())
    }

    SubmitClicked -> {
      case string.trim(model.current_message_input) {
        "" -> #(model, effect.none())
        _ -> #(
          Model(..model, messages: []),
          create_message(model.room_id, model.current_message_input, MessagePosted),
        )
      }
    }

    MessagePosted(Ok(_)) -> {
      #(Model(..model, current_message_input: ""), effect.none())
    }

    MessagePosted(Error(ApiError(message))) -> {
      #(Model(..model, messages: [message]), effect.none())
    }

    WsMessageReceived(m) -> {
      case chat_from_json(m) {
        Ok(chat) if chat.room_id == model.room_id -> {
          #(Model(..model, chat_list: [chat, ..model.chat_list]), effect.none())
        }
        Ok(_) -> #(model, effect.none())
        Error(_) -> #(model, effect.none())
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

    session.Authenticated(_, _) -> {
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
