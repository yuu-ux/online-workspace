import gleam/list
import gleam/dynamic/decode
import gleam/option.{type Option, None, Some}
import lustre/attribute
import lustre/effect
import lustre/element
import lustre/event.{on, on_check}
import lustre/element/html.{div, h1, img, input, p, span, text}
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
    icon_load_failed: Bool,
    is_friend: Bool,
    messages: List(String),
  )
}

pub type Msg {
  ProfileLoaded(Result(user_wrap.UserProfile, user_wrap.GetUserProfileErr))
  MyProfileLoaded(Result(user_wrap.MyProfile, user_wrap.MyProfileErr))
  IconLoadFailed
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
          icon_load_failed: False,
          is_friend: False,
          messages: ["ログインしてください"],
        ),
        effect.none(),
      )

    session_t.Authenticated(_, _) -> {
      let profile_effect = case session {
        session_t.Authenticated(_, current_user)
          if current_user.user_id == target_user_info.user_id ->
          user_wrap.get_my_profile(MyProfileLoaded)
        session_t.Authenticated(_, _) ->
          user_wrap.get_user_profile(target_user_info.user_id, ProfileLoaded)
        session_t.Guest ->
          effect.none()
      }
      #(
        Model(
          session: session,
          user_info: target_user_info,
          profile: None,
          loading: True,
          icon_load_failed: False,
          is_friend: initial_is_friend,
          messages: [],
        ),
        profile_effect,
      )
    }
  }
}

pub fn update(model: Model, msg: Msg) -> #(Model, effect.Effect(Msg)) {
  case msg {
    ProfileLoaded(Ok(profile)) ->
      #(
        Model(
          ..model,
          profile: Some(profile),
          is_friend: profile.friendship == "FRIEND",
          loading: False,
          icon_load_failed: False,
          messages: [],
        ),
        effect.none(),
      )

    ProfileLoaded(Error(user_wrap.GetUserProfileApiErr(api.ApiError(message)))) ->
      #(Model(..model, profile: None, loading: False, messages: [message]), effect.none())

    MyProfileLoaded(Ok(profile)) ->
      #(
        Model(
          ..model,
          profile: Some(user_wrap.UserProfile(
            name: profile.name,
            icon_url: profile.icon_url,
            is_public: profile.is_public,
            bio: profile.bio,
            work_category: profile.work_category,
            friendship: "NONE",
          )),
          loading: False,
          icon_load_failed: False,
          messages: [],
        ),
        effect.none(),
      )

    MyProfileLoaded(Error(user_wrap.MyProfileApiErr(api.ApiError(message)))) ->
      #(Model(..model, profile: None, loading: False, messages: [message]), effect.none())

    IconLoadFailed ->
      #(Model(..model, icon_load_failed: True), effect.none())

    FriendOnChecked(is_friend) -> {
      let previous_state = model.is_friend
      let to_msg = fn(result) { FriendUpdated(previous_state, result) }
      let friendship = case is_friend {
        True -> "FRIEND"
        False -> "NONE"
      }
      let friend_effect = case is_friend {
        True -> user_wrap.add_friend(model.user_info.user_id, to_msg)
        False -> user_wrap.remove_friend(model.user_info.user_id, to_msg)
      }
      #(
        Model(
          ..model,
          profile: set_profile_friendship(model.profile, friendship),
          is_friend: is_friend,
          messages: [],
        ),
        friend_effect,
      )
    }

    FriendUpdated(_, Ok(_)) ->
      #(Model(..model, messages: []), effect.none())

    FriendUpdated(previous_state, Error(api.ApiError(message))) ->
      #(
        Model(
          ..model,
          profile: set_profile_friendship(
            model.profile,
            case previous_state {
              True -> "FRIEND"
              False -> "NONE"
            },
          ),
          is_friend: previous_state,
          messages: [message],
        ),
        effect.none(),
      )

    MoveToFriend -> #(model, effect.none())
  }
}

pub fn view(model: Model) -> element.Element(Msg) {
  let profile_view = case model.profile {
    Some(profile) -> [
      div([attribute.class("flex items-center gap-4 border-b border-gray-100 pb-5")], [
        icon_view(profile.icon_url, model.icon_load_failed),
        div([], [
          h1([attribute.class("text-2xl font-bold text-gray-900")], [text(profile.name)]),
          p([attribute.class("mt-1 text-sm text-gray-500")], [
            text("ユーザープロフィール"),
          ]),
        ]),
      ]),
      div([attribute.class("mt-5 grid gap-3 sm:grid-cols-2")], [
        profile_row("公開設定", bool_to_label(profile.is_public)),
        profile_row("フレンド状態", string_or_fallback(profile.friendship, "非公開")),
        div([attribute.class("rounded-lg bg-gray-50 p-4 sm:col-span-2")], [
          div([attribute.class("text-xs font-medium text-gray-500")], [text("自己紹介")]),
          p([attribute.class("mt-1 break-words text-sm text-gray-800")], [
            text(string_or_fallback(profile.bio, "非公開")),
          ]),
        ]),
        profile_row("作業カテゴリ", string_or_fallback(profile.work_category, "非公開")),
      ]),
    ]
    None -> []
  }

  let friend_control = case model.session {
    session_t.Authenticated(_, current_user)
      if current_user.user_id == model.user_info.user_id -> []

    session_t.Authenticated(_, _) -> [
      div([attribute.class("mt-6 flex items-center justify-between rounded-lg border border-blue-100 bg-blue-50 p-4")], [
        div([], [
          div([attribute.class("font-semibold text-gray-900")], [text("フレンド")]),
          p([attribute.class("mt-1 text-xs text-gray-600")], [
            text("フレンド登録状態を変更できます"),
          ]),
        ]),
        input([
          attribute.type_("checkbox"),
          attribute.checked(model.is_friend),
          attribute.class("h-5 w-5 accent-blue-600"),
          on_check(FriendOnChecked),
        ]),
      ]),
    ]
    session_t.Guest -> []
  }

  div(
    [attribute.class("min-h-screen bg-gray-50 p-4 font-sans sm:p-6")],
    [
      div([attribute.class("mx-auto max-w-2xl rounded-xl border border-gray-200 bg-white p-5 shadow-sm sm:p-8")], [
        div([attribute.class("mb-5 flex items-center justify-between")], [
          h1([attribute.class("text-lg font-semibold text-gray-900")], [text("フレンド詳細")]),
          span([attribute.class("rounded-full bg-gray-100 px-3 py-1 text-xs text-gray-600")], [
            text("プロフィール"),
          ]),
        ]),
        div([attribute.class("space-y-3")], list.map(model.messages, fn(message) {
          div([attribute.class("rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700")], [
            text(message),
          ])
        })),
        div([attribute.class("py-4 text-center text-sm text-gray-500")], case model.loading {
          True -> [text("プロフィールを読み込んでいます...")]
          False -> []
        }),
        div([], profile_view),
        div([], friend_control),
      ]),
    ],
  )
}

fn profile_row(label: String, value: String) -> element.Element(Msg) {
  div([attribute.class("rounded-lg bg-gray-50 p-4")], [
    div([attribute.class("text-xs font-medium text-gray-500")], [text(label)]),
    div([attribute.class("mt-1 break-words text-sm font-semibold text-gray-900")], [text(value)]),
  ])
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

fn set_profile_friendship(
  profile: Option(user_wrap.UserProfile),
  friendship: String,
) -> Option(user_wrap.UserProfile) {
  case profile {
    Some(profile) -> Some(user_wrap.UserProfile(..profile, friendship: friendship))
    None -> None
  }
}

fn icon_view(icon_url: String, load_failed: Bool) -> element.Element(Msg) {
  case icon_url {
    "" -> icon_fallback("アイコン未設定")
    _ -> case load_failed {
      True -> icon_fallback("アイコンを表示できません")
      False ->
        img([
          attribute.src(icon_url),
          attribute.alt("アイコン"),
          attribute.class("h-20 w-20 rounded-full border border-gray-200 object-cover"),
          on("error", decode.success(IconLoadFailed)),
        ])
    }
  }
}

fn icon_fallback(label: String) -> element.Element(Msg) {
  div([
    attribute.attribute("aria-label", label),
    attribute.class("flex h-20 w-20 items-center justify-center rounded-full bg-gray-200 text-xs text-gray-500"),
  ], [])
}
