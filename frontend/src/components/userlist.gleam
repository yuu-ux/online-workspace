import lustre/event.{on_click}
import gleam/int
import types/user.{type UserInfo, type UserId} as user_t
import lustre/element
import gleam/list
import lustre/element/html.{button, div, text}
import lustre/attribute.{class}

/// ユーザーを並べたリストを表示する
pub fn user_list_component(user_list: List(UserInfo), friend_detail_onclicked: fn(UserInfo) -> a) -> element.Element(a) {
  div(
    [class("rounded-xl border border-gray-200 bg-white p-5 shadow-sm")],
    [
      div([class("mb-4 flex items-center justify-between")], [
        text("参加メンバー"),
        div([class("rounded-full bg-blue-50 px-3 py-1 text-sm font-semibold text-blue-700")], [
          text(int.to_string(list.length(user_list)) <> "人"),
        ]),
      ]),
      div([class("space-y-3")], list.map(user_list, fn(member) {
        div([class("flex items-center justify-between rounded-lg bg-gray-50 p-3")], [
          text(member.name),
          button(
            [
              class("rounded-md border border-gray-300 bg-white px-3 py-1.5 text-xs font-medium text-gray-700 transition hover:bg-gray-100"),
              on_click(friend_detail_onclicked(member)),
            ],
            [text("フレンド詳細")],
          ),
        ])
      })),
    ]
  )
}
