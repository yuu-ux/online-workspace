// ユーザー情報
import lustre/event.{on_check}
import lustre/element
import lustre/attribute
import gleam/list
import types/user.{type UserInfo} as user_t
import types/session.{type Session, Authenticated, Guest} as session_t
import lustre/element/html.{div, text, input}
import lustre/effect
import wrap/api.{ApiError, type ApiError}
import wrap/user.{add_friend, get_user_profile, remove_friend, type UserProfile}

pub type Model {
  Model(
    session: Session,
    user_info: UserInfo,
    is_blocked: Bool,
    is_friend: Bool,
    messages: List(String)
  )
}

// Msg(a) a: 前のページ
pub type Msg {
  BlockOnChecked(Bool)
  FriendOnChecked(Bool)
  ProfileLoaded(Result(UserProfile, ApiError))
  FriendUpdated(Result(Nil, ApiError))
}

pub fn init(session: Session, target_user_info:UserInfo) -> #(Model, effect.Effect(Msg)) {
  case session {
    Guest -> {
      #(
        Model(
          session: session,
          user_info: target_user_info,
          is_friend: False,
          is_blocked: False,
          messages: []),
        effect.none()
      )
    }
    Authenticated(_, _) -> {
      #(
        Model(
          session: session,
          user_info: target_user_info,
          is_friend: False,
          is_blocked: False,
          messages: []),
        get_user_profile(target_user_info.user_id, ProfileLoaded)
      )
    }
  }
}

pub fn update(model: Model, msg: Msg) -> #(Model, effect.Effect(Msg)) {
  case msg {
    BlockOnChecked(s) -> {
      #(Model(..model, is_blocked: s), effect.none())
    }
    FriendOnChecked(s) -> {
      let friend_effect = case s {
        True -> add_friend(model.user_info.user_id, FriendUpdated)
        False -> remove_friend(model.user_info.user_id, FriendUpdated)
      }
      #(
        Model(..model, is_friend: s, messages: []),
        friend_effect,
      )
    }
    ProfileLoaded(Ok(profile)) -> {
      #(
        Model(..model, is_friend: profile.is_friend),
        effect.none(),
      )
    }
    ProfileLoaded(Error(ApiError(message))) ->
      #(Model(..model, messages: [message]), effect.none())
    FriendUpdated(Ok(_)) -> #(model, effect.none())
    FriendUpdated(Error(ApiError(message))) ->
      #(Model(..model, messages: [message]), effect.none())
  }
}

pub fn view (model: Model) -> element.Element(Msg) {
  case model.session {
    session_t.Guest -> {
      div(
        [],
        [
          text("他ユーザープロフィール詳細"),
          text("ログインしてください")
        ]
      )
    }

    session_t.Authenticated(_, _) -> {
      div([
        attribute.attribute("style", "padding: 20px; font-family: sans-serif;")
      ],
      [
        text("他ユーザープロフィール詳細"),
        div(
          [],
          [
            div([], [
              div([], [text(model.user_info.name)]),
              input(
                [
                  attribute.type_("checkbox"), 
                  attribute.checked(model.is_friend),
                  on_check(FriendOnChecked)
                ]
              ),
              input(
                [
                  attribute.type_("checkbox"), 
                  attribute.checked(model.is_blocked),
                  on_check(BlockOnChecked)
                ]
              ),
            ])
            ,
            div([], list.map(model.messages, fn(message) { div([], [text(message)]) }))
          ]
        )
      ])
    }
  }
}
