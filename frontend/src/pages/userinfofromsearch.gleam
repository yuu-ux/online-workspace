import lustre/event.{on_click}
import gleam/list
import types/user.{type UserInfo}
import lustre/element
import lustre/effect
import types/session.{type Session}

import components/userinfo
import lustre/element/html.{button, div, text}
import wrap/api.{type ApiError}
import wrap/user as user_wrap

pub type Model {
  Model(
    user_info_component: userinfo.Model,
    search_word: String
  )
}

pub type Msg {
  ToSearch(String)
  ToFriend
  UserInfo(userinfo.Msg)
  FriendsLoaded(Result(List(UserInfo), ApiError))
}

pub fn init(session: Session, target_user_info: UserInfo, search_word: String) -> #(Model, effect.Effect(Msg)) {
  let #(model, user_effect) = userinfo.init(session, target_user_info)

  #(
    Model(user_info_component: model, search_word: search_word),
    effect.batch([
      user_effect |> effect.map(UserInfo),
      user_wrap.get_friends(FriendsLoaded),
    ]),
  )
}

pub fn update(model: Model, msg: Msg) -> #(Model, effect.Effect(Msg)) {
  case msg {
    ToSearch(_search_word) -> {
      #(model, effect.none())
    }

    FriendsLoaded(Ok(friends)) -> {
      let target_user_id = model.user_info_component.user_info.user_id
      let is_friend = list.any(friends, fn(friend) { friend.user_id == target_user_id })
      let updated_user_info = userinfo.set_friend_status(
        model.user_info_component,
        is_friend,
      )
      #(Model(..model, user_info_component: updated_user_info), effect.none())
    }

    FriendsLoaded(Error(_)) -> {
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
  div([], [
    user_info_elem,
    button([on_click(ToSearch(model.search_word))], [text("検索結果に戻る")])
  ])
}
