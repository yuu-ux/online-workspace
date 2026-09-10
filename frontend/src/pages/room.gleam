import components/btn
import components/userlist.{user_list_component}
import gleam/int
import gleam/list
import gleam/option.{type Option, None, Some}
import gleam/string
import lustre/attribute.{class}
import lustre/effect
import lustre/element
import lustre/element/html.{button, div, h1, input, p, span, text}
import lustre/event.{on_click, on_input}

import types/room as room_t
import types/session.{type Session}
import types/user.{UserInfo, type UserInfo}
import wrap/api.{type ApiError, ApiError}

import wrap/room.{
  type Chat,
  chat_from_json,
  close_ws,
  connect_to_server,
  create_message,
  get_messages,
  get_room,
  get_room_members,
  join_room,
  leave_room,
  presence_from_json,
}

pub type InputType {
  ChatMsg
}

pub type Model {
  Model(
    session: Session,
    room_id: room_t.RoomId,
    room_detail: Option(room_t.RoomDetail),
    joined: Bool,
    member_list: List(UserInfo),
    chat_list: List(Chat),
    current_message_input: String,
    messages: List(String),
  )
}

pub type Msg {
  ToHome
  ToUserInfo(UserInfo)
  InputUpdated(target: InputType, str: String)
  SubmitClicked
  RoomLoaded(Result(room_t.RoomDetail, ApiError))
  JoinedRoom(Result(Nil, ApiError))
  MembersLoaded(Result(List(UserInfo), ApiError))
  MessagesLoaded(Result(List(Chat), ApiError))
  MessagePosted(Result(Nil, ApiError))
  RoomLeft(Result(Nil, ApiError))
  LeaveCompleted
  WsMessageReceived(String)
}

pub fn init(session: Session, room_id: room_t.RoomId) -> #(Model, effect.Effect(Msg)) {
  let model = Model(
    session: session,
    room_id: room_id,
    room_detail: None,
    joined: False,
    member_list: [],
    chat_list: [],
    current_message_input: "",
    messages: [],
  )

  case session {
    session.Guest -> #(Model(..model, messages: ["ログインしてください"]), effect.none())
    session.Authenticated(..) -> #(model, get_room(room_id, RoomLoaded))
  }
}

pub fn update(model: Model, msg: Msg) -> #(Model, effect.Effect(Msg)) {
  case msg {
    ToHome -> {
      case model.joined {
        True -> #(model, leave_room(model.room_id, RoomLeft))
        False -> {
          close_ws()
          #(
            model,
            effect.from(fn(dispatch) { dispatch(LeaveCompleted) }),
          )
        }
      }
    }

    ToUserInfo(_user_info) -> {
      #(model, effect.none())
    }

    InputUpdated(target, input_text) -> {
      let new_model = case target {
        ChatMsg -> Model(..model, current_message_input: input_text)
      }
      #(new_model, effect.none())
    }

    RoomLoaded(Ok(detail)) -> {
      let updated_model = Model(
        ..model,
        room_detail: Some(detail),
        joined: detail.member,
      )
      case detail.member {
        True -> #(updated_model, get_room_members(model.room_id, MembersLoaded))
        False -> {
          case detail.joinable {
            True -> #(updated_model, join_room(model.room_id, JoinedRoom))
            False -> #(
              Model(
                ..updated_model,
                messages: [join_restriction_message(detail.join_restriction)],
              ),
              effect.none(),
            )
          }
        }
      }
    }

    RoomLoaded(Error(ApiError(message))) -> {
      #(Model(..model, messages: [message]), effect.none())
    }

    JoinedRoom(Ok(_)) -> {
      #(
        Model(..model, joined: True, messages: ["入室しました。"]),
        get_room(model.room_id, RoomLoaded),
      )
    }

    JoinedRoom(Error(ApiError(message))) -> {
      #(Model(..model, messages: [message]), effect.none())
    }

    MembersLoaded(Ok(members)) -> {
      #(
        Model(..model, member_list: members),
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

    RoomLeft(Ok(_)) -> {
      close_ws()
      #(model, effect.from(fn(dispatch) { dispatch(LeaveCompleted) }))
    }

    RoomLeft(Error(ApiError(message))) -> {
      #(Model(..model, messages: [message]), effect.none())
    }

    LeaveCompleted -> #(model, effect.none())

    WsMessageReceived(message) -> {
      case chat_from_json(message) {
        Ok(chat) if chat.room_id == model.room_id -> {
          #(Model(..model, chat_list: [chat, ..model.chat_list]), effect.none())
        }
        Ok(_) -> #(model, effect.none())
        Error(_) -> {
          case presence_from_json(message) {
            Ok(presence) if presence.room_id == model.room_id -> {
              let without_user =
                list.filter(model.member_list, fn(member) {
                  member.user_id != presence.user_id
                })
              let members = case presence.online {
                True -> [
                  UserInfo(name: presence.user, user_id: presence.user_id),
                  ..without_user
                ]
                False -> without_user
              }
              #(Model(..model, member_list: members), effect.none())
            }
            _ -> #(model, effect.none())
          }
        }
      }
    }
  }
}

fn join_restriction_message(restriction: Option(String)) -> String {
  case restriction {
    Some("FULL") -> "このルームは満員のため入室できません。"
    Some("CLOSED") -> "このルームは終了しているため入室できません。"
    _ -> "このルームには入室できません。"
  }
}

pub fn view(model: Model) -> element.Element(Msg) {
  case model.session {
    session.Guest ->
      div(
        [class("p-5")],
        [text("Room"), text("ログインしてください"), btn.to_home_btn_component(ToHome)],
      )

    session.Authenticated(_, _) -> {
      let room_content = case model.joined {
        True -> [
          user_list_component(model.member_list, ToUserInfo),
          div([], [
            div(
              [],
              list.map(
                list.reverse(model.chat_list),
                fn(chat) { div([], [text("from" <> chat.user), text(chat.message)]) },
              ),
            ),
            input([
              on_input(InputUpdated(ChatMsg, _)),
              attribute.value(model.current_message_input),
            ]),
            button([on_click(SubmitClicked)], [text("send")]),
          ]),
          btn.to_home_btn_component(ToHome),
        ]
        False -> [btn.to_home_btn_component(ToHome)]
      }
      div(
        [class("p-5 font-sans")],
        list.flatten([
          [room_detail_view(model.room_detail)],
          room_content,
          [div([class("text-red-600")], list.map(model.messages, fn(message) {
            div([], [text(message)])
          }))],
        ]),
      )
    }
  }
}

fn room_detail_view(detail: Option(room_t.RoomDetail)) -> element.Element(Msg) {
  case detail {
    None -> div([], [h1([], [text("Room")])])
    Some(room) -> {
      let room_name = case room.roomname {
        room_t.RoomNameType(name) -> name
      }
      let creator_name = case room.created_by {
        UserInfo(name: name, user_id: _) -> name
      }
      div([class("mb-5")], [
        h1([], [text(room_name)]),
        p([], [text(room_description(room.description))]),
        detail_row("カテゴリ", category_to_string(room.category)),
        detail_row("作業スタイル", work_style_to_string(room.work_style)),
        detail_row("参加人数", int_to_string(room.current_members) <> " / " <> int_to_string(room.max_number_of_member) <> " 人"),
        detail_row("ステータス", room.status),
        detail_row("作成者", creator_name),
        detail_row("作成日時", format_created_at_jst(room.created_at)),
      ])
    }
  }
}

@external(javascript, "./../ffi/date.js", "format_created_at_jst")
fn format_created_at_jst(value: String) -> String

fn room_description(description: room_t.DescriptionType) -> String {
  case description {
    room_t.DescriptionType(value) -> value
  }
}

fn detail_row(label: String, value: String) -> element.Element(Msg) {
  div([], [span([], [text(label <> ": ")]), span([], [text(value)])])
}

fn category_to_string(category: room_t.CategoryType) -> String {
  case category {
    room_t.Cat1 -> "カテゴリ1"
    room_t.Cat2 -> "カテゴリ2"
    room_t.Cat3 -> "カテゴリ3"
  }
}

fn work_style_to_string(work_style: room_t.WorkStyleType) -> String {
  case work_style {
    room_t.CasualChat -> "雑談OK"
    room_t.Quiet -> "もくもく"
  }
}

fn int_to_string(value: Int) -> String {
  int.to_string(value)
}
