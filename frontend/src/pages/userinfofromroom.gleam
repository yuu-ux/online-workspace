import lustre/event.{on_click}
import types/room.{type RoomId}
import types/user.{type UserInfo}
import lustre/element
import lustre/effect
import types/session.{type Session}

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
  UserInfo(userinfo.Msg)
}

pub fn init(session: Session, target_user_info: UserInfo, backroomid: RoomId) -> #(Model, effect.Effect(Msg)) {
  let #(model, effect) = userinfo.init(session, target_user_info)

  #(Model(user_info_component: model, backroomid: backroomid), effect.none())
}

pub fn update(model: Model, msg: Msg) -> #(Model, effect.Effect(Msg)) {
  case msg {
    ToRoom(room_id) -> {
      #(model, effect.none())
    }

    UserInfo(user_info_msg) -> {
      let #(update_user_info_model, update_effect) = userinfo.update(model.user_info_component, user_info_msg)
      #(Model(..model, user_info_component: update_user_info_model), update_effect |> effect.map(UserInfo))
    }
  }
}

pub fn view(model: Model) -> element.Element(Msg) {
  let user_info_elem = userinfo.view(model.user_info_component) |> element.map(UserInfo)
  div([], [
    user_info_elem,
    button([on_click(ToRoom(model.backroomid))], [text("ルームに戻る")])
  ])
}
