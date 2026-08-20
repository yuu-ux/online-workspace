// ユーザー情報
import lustre/event.{on_check}
import lustre/element
import lustre/attribute
import types/user.{type UserInfo} as user_t
import types/session.{type Session, Authenticated, Guest} as session_t
import lustre/element/html.{div, text, input}
import lustre/effect
import wrap/user.{is_friend, is_blocked}

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
    Authenticated(token, user_info) -> {
      #(
        Model(
          session: session,
          user_info: target_user_info,
          is_friend: is_friend(user_info.user_id, target_user_info.user_id),
          is_blocked: is_blocked(user_info.user_id, target_user_info.user_id),
          messages: []),
        effect.none()
      )
    }
  }
}

pub fn update(model: Model, msg: Msg) -> #(Model, effect.Effect(Msg)) {
  case msg {
    BlockOnChecked(s) -> {

    }
    FriendOnChecked(s) -> {

    }
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

    session_t.Authenticated(jwt, user_id) -> {
      div([
        attribute.attribute("style", "padding: 20px; font-family: sans-serif;")
      ],
      [
        text("他ユーザープロフィール詳細"),
        div(
          [],
          [
            div([], [
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
          ]
        )
      ])
    }
  }
}

