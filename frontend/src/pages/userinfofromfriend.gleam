import lustre/event.{on_click}
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
  )
}

pub type Msg {
  ToFriend
  UserInfo(userinfo.Msg)
}

pub fn init(session: Session, target_user_info: UserInfo) -> #(Model, effect.Effect(Msg)) {
  let #(model, user_effect) = userinfo.init_friend(session, target_user_info)

  #(Model(user_info_component: model), user_effect |> effect.map(UserInfo))
}

pub fn update(model: Model, msg: Msg) -> #(Model, effect.Effect(Msg)) {
  case msg {
    ToFriend -> {
      #(model, effect.none())
    }

    UserInfo(user_info_msg) -> {
      case user_info_msg {
        userinfo.MoveToFriend ->
          #(model, effect.from(fn(dispatch) { dispatch(ToFriend) }))
        _ -> {
          let #(update_user_info_model, update_effect) = userinfo.update(model.user_info_component, user_info_msg)
          #(Model(user_info_component: update_user_info_model), update_effect |> effect.map(UserInfo))
        }
      }
    }
  }
}

pub fn view(model: Model) -> element.Element(Msg) {
  let user_info_elem = userinfo.view(model.user_info_component) |> element.map(UserInfo)
  div([class("min-h-screen bg-[#f7f5f0]")], [
    div([class("px-4 py-4 sm:px-8")], [
      button([
        class("text-sm font-medium text-[#58745a] hover:underline"),
        on_click(ToFriend),
      ], [text("← マイページに戻る")]),
    ]),
    user_info_elem,
  ])
}
