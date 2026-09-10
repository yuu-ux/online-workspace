import lustre/event.{on_click}
import types/room.{type RoomId}
import types/user.{type UserInfo}
import lustre/element
import lustre/effect
import types/session.{type Session}
import lustre/attribute.{class}

import components/userinfo
import lustre/element/html.{button, div, text}

pub type Model {
  Model(
    user_info_component: userinfo.Model,
    backroomid: RoomId
  )
}

pub type Msg {
  ToRoom(RoomId)
  ToFriend
  UserInfo(userinfo.Msg)
}

pub fn init(session: Session, target_user_info: UserInfo, backroomid: RoomId) -> #(Model, effect.Effect(Msg)) {
  let #(model, effect) = userinfo.init(session, target_user_info)

  #(Model(user_info_component: model, backroomid: backroomid), effect |> effect.map(UserInfo))
}

pub fn update(model: Model, msg: Msg) -> #(Model, effect.Effect(Msg)) {
  case msg {
    ToRoom(room_id) -> {
      #(model, effect.none())
    }
    ToFriend -> {
      #(model, effect.none())
    }

    UserInfo(user_info_msg) -> {
      case user_info_msg {
        userinfo.MoveToFriend ->
          #(model, effect.from(fn(dispatch) { dispatch(ToFriend) }))
        _ -> {
          let #(update_user_info_model, update_effect) = userinfo.update(model.user_info_component, user_info_msg)
          #(Model(..model, user_info_component: update_user_info_model), update_effect |> effect.map(UserInfo))
        }
      }
    }
  }
}

pub fn view(model: Model) -> element.Element(Msg) {
  let user_info_elem = userinfo.view(model.user_info_component) |> element.map(UserInfo)
  div([class("bg-gray-50")], [
    user_info_elem,
    div([class("mx-auto max-w-2xl px-4 pb-6 sm:px-8")], [
      button([
        class("w-full rounded-lg border border-gray-300 bg-white px-4 py-2 text-sm font-semibold text-gray-700 shadow-sm transition hover:bg-gray-50"),
        on_click(ToRoom(model.backroomid)),
      ], [text("ルームに戻る")]),
    ]),
  ])
}
