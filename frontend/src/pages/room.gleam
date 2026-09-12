import components/btn
import components/ui
import components/userlist.{room_member_list}
import gleam/int
import gleam/dynamic/decode
import gleam/list
import gleam/option.{type Option, None, Some}
import gleam/string
import lustre/attribute.{class}
import lustre/effect
import lustre/element
import lustre/element/html.{button, div, h1, img, input, p, span, text}
import lustre/event.{on, on_click, on_input}

import types/room as room_t
import types/session.{type Session}
import types/user.{UserInfo, type UserInfo, type UserId}
import wrap/api.{type ApiError, ApiError}

import wrap/room.{
  type Chat,
  Chat,
  type RoomMember,
  RoomMember,
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
  room_member_count_from_json,
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
    member_list: List(RoomMember),
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
  MembersLoaded(Result(List(RoomMember), ApiError))
  MemberIconFailed(UserId)
  ChatIconFailed(String)
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

    MemberIconFailed(id) -> {
      #(Model(..model, member_list: list.map(model.member_list, fn(member) {
        case member.user_id == id {
          True -> RoomMember(..member, icon_url: "")
          False -> member
        }
      })), effect.none())
    }

    ChatIconFailed(url) -> {
      #(Model(..model, chat_list: list.map(model.chat_list, fn(chat) {
        case chat.icon_url == url {
          True -> Chat(..chat, icon_url: "")
          False -> chat
        }
      })), effect.none())
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
      case room_member_count_from_json(message) {
        Ok(member_count) if member_count.room_id == model.room_id ->
          #(
            Model(
              ..model,
              room_detail:
                update_member_count(model.room_detail, member_count.current_members),
            ),
            effect.none(),
          )
        Ok(_) -> #(model, effect.none())
        Error(_) ->
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
                      RoomMember(presence.user, presence.user_id, presence.icon_url),
                      ..without_user
                    ]
                    False -> without_user
                  }
                  #(
                    Model(..model, member_list: members),
                    effect.none(),
                  )
                }
                _ -> #(model, effect.none())
              }
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
        [class("flex min-h-screen items-center justify-center bg-[#f7f4ee] p-5")],
        [
          div([class("rounded-lg border border-[#e5ded2] bg-[#fffdf9] p-8 text-center")], [
            h1([class("mb-3 text-xl font-bold text-slate-900")], [text("ルーム")]),
            p([class("mb-6 text-[#6f6a61]")], [text("ログインしてください")]),
            btn.to_home_btn_component(ToHome),
          ]),
        ],
      )

    session.Authenticated(_, _) -> {
      let room_content = case model.joined {
        True -> [
          div([class("grid min-h-0 flex-1 grid-cols-[minmax(0,1fr)_4.5rem] sm:grid-cols-[minmax(0,1fr)_6rem]")], [
            div([class("flex min-h-0 min-w-0 flex-col")], [
              div(
                [class("min-h-0 flex-1 space-y-1 overflow-y-auto px-4 py-5 sm:px-8")],
                list.map(
                  list.reverse(model.chat_list),
                  fn(chat) {
                    div([class("flex items-start gap-2.5 py-2 transition-colors hover:bg-[#efede7]")], [
                      div([class("mt-0.5 h-10 w-10 shrink-0 overflow-hidden rounded-full bg-gray-200")], case chat.icon_url {
                        "" -> []
                        url -> [img([attribute.src(url), attribute.alt(""), class("h-full w-full object-cover"), on("error", decode.success(ChatIconFailed(url)))])]
                      }),
                      div([class("min-w-0 flex-1")], [
                      div([class("mb-1 flex items-baseline gap-3")], [
                        span([class("text-sm font-semibold text-slate-800")], [text(chat.user)]),
                        span([class("text-xs text-[#847e74]"), attribute.title(format_created_at_jst(chat.sent_at))], [text(format_message_time_jst(chat.sent_at))]),
                      ]),
                      div([class("break-words text-sm leading-relaxed text-slate-700")], [text(chat.message)]),
                      ]),
                    ])
                  },
                ),
              ),
              div([class("flex shrink-0 gap-2 border-t border-[#e2dfd7] px-4 py-4 sm:px-8")], [
                input([
                  class("flex-1 " <> ui.input_classes()),
                  on_input(InputUpdated(ChatMsg, _)),
                  attribute.value(model.current_message_input),
                ]),
                button(
                  [
                    class("rounded-md bg-[#58745a] px-4 py-2 text-sm font-semibold text-white transition hover:bg-[#46614a]"),
                    on_click(SubmitClicked),
                  ],
                  [text("送信")],
                ),
              ]),
            ]),
            room_member_list(model.member_list, ToUserInfo, MemberIconFailed),
          ]),
        ]
        False -> []
      }
      div(
        [class("flex h-dvh flex-col overflow-hidden bg-[#f7f5f0] pb-14 font-sans")],
        list.flatten([
          [
            div([class("shrink-0 px-4 pt-4 sm:px-8")], [
              button([class("text-sm font-medium text-[#58745a] hover:underline"), on_click(ToHome)], [text("← ホームへ")]),
            ]),
          ],
          [room_detail_view(model.room_detail)],
          room_content,
          [
            div([class("shrink-0 px-4 sm:px-8")], [
              div([class("space-y-2")], list.map(model.messages, fn(message) {
                div([class("py-2 text-xs text-[#6f6a61]")], [
                  text(message),
                ])
              })),
            ]),
          ],
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
      div([class("shrink-0 border-b border-[#dedbd2] px-4 pb-4 pt-4 sm:px-8")], [
        div([class("flex flex-col gap-3")], [
          div([class("min-w-0")], [
            div([class("flex items-center gap-2")], [
              h1([class("break-words text-xl font-semibold tracking-tight text-slate-900")], [text(room_name)]),
            ]),
            p([class("mt-1 max-h-16 overflow-y-auto break-words text-sm leading-relaxed text-[#6f6a61]")], [text(room_description(room.description))]),
          ]),
          div([class("flex flex-wrap items-center gap-x-5 gap-y-2 text-xs leading-relaxed")], [
            compact_detail("作業", work_style_to_string(room.work_style)),
            compact_detail("参加", int_to_string(room.current_members) <> " / " <> int_to_string(room.max_number_of_member) <> " 人"),
            compact_detail("作成者", creator_name),
            span([class("whitespace-nowrap text-[#847e74]")], [text(format_created_at_jst(room.created_at))]),
          ]),
        ]),
      ])
    }
  }
}

@external(javascript, "./../ffi/date.js", "format_created_at_jst")
fn format_created_at_jst(value: String) -> String

@external(javascript, "./../ffi/date.js", "format_message_time_jst")
fn format_message_time_jst(value: String) -> String

fn room_description(description: room_t.DescriptionType) -> String {
  case description {
    room_t.DescriptionType(value) -> value
  }
}

fn compact_detail(label: String, value: String) -> element.Element(Msg) {
  span([class("inline-flex flex-wrap gap-x-1.5")], [
    span([class("mr-1 text-[#847e74]")], [text(label)]),
    span([class("font-semibold text-slate-800")], [text(value)]),
  ])
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

fn update_member_count(
  detail: Option(room_t.RoomDetail),
  member_count: Int,
) -> Option(room_t.RoomDetail) {
  case detail {
    Some(room) -> Some(room_t.RoomDetail(..room, current_members: member_count))
    None -> None
  }
}
