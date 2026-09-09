import gleam/list
import gleam/option.{type Option, None, Some}
import lustre/attribute
import lustre/effect
import lustre/element
import lustre/event.{on_check}
import lustre/element/html.{div, input, text}
import types/session.{type Session} as session_t
import types/user.{type UserInfo}
import wrap/api as api
import wrap/user as user_wrap

pub type Model {
  Model(
    session: Session,
    user_info: UserInfo,
    profile: Option(user_wrap.UserProfile),
    loading: Bool,
    is_friend: Bool,
    messages: List(String),
  )
}

pub type Msg {
  ProfileLoaded(Result(user_wrap.UserProfile, user_wrap.GetUserProfileErr))
  MoveToFriend
  FriendOnChecked(Bool)
  FriendUpdated(Bool, Result(Nil, api.ApiError))
}

pub fn init(session: Session, target_user_info: UserInfo) -> #(Model, effect.Effect(Msg)) {
  init_with_friend_status(session, target_user_info, False)
}

pub fn init_friend(session: Session, target_user_info: UserInfo) -> #(Model, effect.Effect(Msg)) {
  init_with_friend_status(session, target_user_info, True)
}

pub fn set_friend_status(model: Model, is_friend: Bool) -> Model {
  Model(..model, is_friend: is_friend)
}

fn init_with_friend_status(
  session: Session,
  target_user_info: UserInfo,
  initial_is_friend: Bool,
) -> #(Model, effect.Effect(Msg)) {
  case session {
    session_t.Guest ->
      #(
        Model(
          session: session,
          user_info: target_user_info,
          profile: None,
          loading: False,
          is_friend: False,
          messages: ["ログインしてください"],
        ),
        effect.none(),
      )

    session_t.Authenticated(_, _) ->
      #(
        Model(
          session: session,
          user_info: target_user_info,
          profile: None,
          loading: True,
          is_friend: initial_is_friend,
          messages: [],
        ),
        user_wrap.get_user_profile(target_user_info.user_id, ProfileLoaded),
      )
  }
}

pub fn update(model: Model, msg: Msg) -> #(Model, effect.Effect(Msg)) {
  case msg {
    ProfileLoaded(Ok(profile)) ->
      #(Model(..model, profile: Some(profile), loading: False, messages: []), effect.none())

    ProfileLoaded(Error(user_wrap.GetUserProfileApiErr(api.ApiError(message)))) ->
      #(Model(..model, profile: None, loading: False, messages: [message]), effect.none())

    FriendOnChecked(is_friend) -> {
      let previous_state = model.is_friend
      let to_msg = fn(result) { FriendUpdated(previous_state, result) }
      let friend_effect = case is_friend {
        True -> user_wrap.add_friend(model.user_info.user_id, to_msg)
        False -> user_wrap.remove_friend(model.user_info.user_id, to_msg)
      }
      #(Model(..model, is_friend: is_friend, messages: []), friend_effect)
    }

    FriendUpdated(_, Ok(_)) ->
      #(Model(..model, messages: []), effect.none())

    FriendUpdated(previous_state, Error(api.ApiError(message))) ->
      #(Model(..model, is_friend: previous_state, messages: [message]), effect.none())

    MoveToFriend -> #(model, effect.none())
  }
}

pub fn view(model: Model) -> element.Element(Msg) {
  let profile_view = case model.profile {
    Some(profile) -> [
      div([], [text("ユーザー名: " <> profile.name)]),
      div([], [text("アイコン: " <> string_or_fallback(profile.icon_url, "未設定"))]),
      div([], [text("公開設定: " <> bool_to_label(profile.is_public))]),
      div([], [text("自己紹介: " <> string_or_fallback(profile.bio, "非公開"))]),
      div([], [text("作業カテゴリ: " <> string_or_fallback(profile.work_category, "非公開"))]),
      div([], [text("フレンド状態: " <> string_or_fallback(profile.friendship, "非公開"))]),
    ]
    None -> []
  }

  let friend_control = case model.session {
    session_t.Authenticated(_, _) -> [
      div([], [
        text("フレンド"),
        input([
          attribute.type_("checkbox"),
          attribute.checked(model.is_friend),
          on_check(FriendOnChecked),
        ]),
      ]),
    ]
    session_t.Guest -> []
  }

  div(
    [attribute.attribute("style", "padding: 20px; font-family: sans-serif;")],
    [
      text("他ユーザープロフィール詳細"),
      div([], list.map(model.messages, fn(message) { div([], [text(message)]) })),
      div([], case model.loading {
        True -> [text("読み込み中...")]
        False -> []
      }),
      div([], profile_view),
      div([], friend_control),
    ],
  )
}

fn bool_to_label(value: Bool) -> String {
  case value {
    True -> "公開"
    False -> "非公開"
  }
}

fn string_or_fallback(value: String, fallback: String) -> String {
  case value {
    "" -> fallback
    _ -> value
  }
}
