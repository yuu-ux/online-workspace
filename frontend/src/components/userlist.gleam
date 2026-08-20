import lustre/event.{on_click}
import types/user.{type UserInfo, type UserId} as user_t
import lustre/element
import gleam/list
import lustre/element/html.{button, div, text}

/// ユーザーを並べたリストを表示する
pub fn user_list_component(user_list: List(UserInfo), friend_detail_onclicked: fn(UserInfo) -> a) -> element.Element(a) {
  div(
    [],
    [
      div([], list.map(user_list, fn (member) {
        div([], [text(member.name),
        button([on_click(friend_detail_onclicked(member))], [text("フレンド詳細")])])
      }))
    ]
  )
}
