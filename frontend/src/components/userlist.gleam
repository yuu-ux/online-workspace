import lustre/event.{on_click}
import gleam/int
import types/user.{type UserInfo, type UserId} as user_t
import lustre/element
import gleam/list
import gleam/string
import lustre/element/html.{button, div, span, text}
import lustre/attribute.{class}
import gleam/dynamic/decode
import wrap/room.{type RoomMember}
import types/user

pub fn room_member_list(members: List(RoomMember), on_detail: fn(UserInfo) -> a, on_error: fn(user.UserId) -> a) -> element.Element(a) {
  div([class("min-h-0 overflow-y-auto border-l border-[#dedbd2] bg-[#efede6] px-2 py-5")], [
    div([class("mb-4 text-center text-xs leading-5 text-[#6f6a61]")], [text("参加者 " <> int.to_string(list.length(members)) <> "人")]),
    div([class("flex flex-col items-center gap-3")], list.map(members, fn(member) {
      button([class("h-10 w-10 shrink-0 overflow-hidden rounded-full bg-gray-200 focus:ring-2 focus:ring-[#58745a]"), attribute.title(member.name), attribute.attribute("aria-label", member.name), on_click(on_detail(user.UserInfo(member.name, member.user_id)))],
        case member.icon_url {
          "" -> []
          url -> [html.img([attribute.src(url), attribute.alt(""), class("h-full w-full object-cover"), event.on("error", decode.success(on_error(member.user_id)))])]
        }
      )
    })),
  ])
}

/// ユーザーを並べたリストを表示する
pub fn user_list_component(user_list: List(UserInfo), friend_detail_onclicked: fn(UserInfo) -> a) -> element.Element(a) {
  div(
    [class("min-h-0 overflow-y-auto border-l border-[#dedbd2] bg-[#efede6] px-2 py-5")],
    [
      div([class("mb-4 flex flex-col items-center gap-1 text-xs text-[#6f6a61]")], [
        text("参加者"),
        div([class("text-xs text-[#6f6a61]")], [
          text(int.to_string(list.length(user_list)) <> "人"),
        ]),
      ]),
      div([class("flex flex-col items-center gap-3")], list.map(user_list, fn(member) {
        member_avatar(member, friend_detail_onclicked(member))
      })),
    ]
  )
}

fn member_avatar(member: UserInfo, on_member_clicked: a) -> element.Element(a) {
  button(
    [
      class("group relative flex h-10 w-10 items-center justify-center rounded-full bg-[#58745a] text-sm font-bold text-white transition hover:bg-[#46614a] focus:outline-none focus:ring-2 focus:ring-[#58745a] focus:ring-offset-2"),
      on_click(on_member_clicked),
    ],
    [
      div([], [text(avatar_label(member.name))]),
      span([class("sr-only")], [text(member.name)]),
      span([class("pointer-events-none absolute right-full top-1/2 z-10 mr-2 hidden -translate-y-1/2 whitespace-nowrap rounded-md bg-slate-900 px-2 py-1 text-xs font-medium text-white shadow-sm group-hover:block")], [
        text(member.name),
      ]),
    ],
  )
}

fn avatar_label(name: String) -> String {
  case string.first(name) {
    Ok(first) -> first
    Error(_) -> "?"
  }
}
