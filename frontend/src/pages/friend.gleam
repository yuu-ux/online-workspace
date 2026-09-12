// フレンド編集画面
import components/btn
import components/ui
import gleam/int
import gleam/list
import gleam/dynamic/decode
import gleam/result
import lustre/event.{on, on_click, on_input}
import lustre/attribute
import lustre/element
import lustre/effect
import gleam/io
import lustre/element/html.{button, div, h1, h2, img, input, p, span, text}

import types/user.{FriendInfo, type FriendInfo, type UserInfo, type UserId} as user_t
import types/session.{type Session, Guest, Authenticated}

import wrap/api.{type ApiError, ApiError}
import wrap/user.{get_friends_with_icons}
import wrap/room.{connect_to_friend_presence, friend_presence_from_json}

pub type Model {
  Model(
    session: Session,
    friends: List(FriendInfo),
    icons: List(#(UserInfo, String)),
    search_word: String,
    messages: List(String)
  )
}

pub type Msg {
  ToHome
  ToMyPage
  ToUserInfo(UserInfo)
  SearchInputChanged(String)
  SearchSubmitted
  FriendsLoaded(Result(List(UserInfo), ApiError))
  ProfilesLoaded(Result(List(#(FriendInfo, String)), ApiError))
  FriendWsMessage(String)
  FriendPresenceChanged(UserId, Bool)
  IconFailed(UserId)
}

pub fn init(session: Session) -> #(Model, effect.Effect(Msg)) {
  case session {
    Guest -> {
      #(Model(
          session: session,
          friends: [],
          icons: [],
          search_word: "",
          messages: ["ログインしてください"]),
        effect.none(),
      )
    }
    Authenticated(..) -> {
      #(Model(
          session: session,
          friends: [],
          icons: [],
          search_word: "",
          messages: []),
        effect.batch([
          get_friends_with_icons(ProfilesLoaded),
          connect_to_friend_presence(FriendWsMessage),
        ]),
      )
    }
  }
}


pub fn update(model: Model, msg: Msg) -> #(Model, effect.Effect(Msg)) {
  case msg {
    ProfilesLoaded(Ok(profiles)) ->
      #(
        Model(
          ..model,
          friends: list.map(profiles, fn(item) { item.0 }),
          icons: list.map(profiles, fn(item) { #(item.0.user, item.1) }),
          messages: [],
        ),
        effect.none(),
      )
    ProfilesLoaded(Error(error)) -> update(model, FriendsLoaded(Error(error)))
    FriendWsMessage(message) ->
      case friend_presence_from_json(message) {
        Ok(presence) -> update(model, FriendPresenceChanged(presence.user_id, presence.online))
        Error(_) -> #(model, effect.none())
      }
    FriendPresenceChanged(user_id, online) ->
      #(
        Model(..model, friends: list.map(model.friends, fn(friend) {
          case friend.user.user_id == user_id {
            True -> FriendInfo(friend.user, online)
            False -> friend
          }
        })),
        effect.none(),
      )
    IconFailed(id) ->
      #(Model(..model, icons: list.map(model.icons, fn(item) {
        case item.0.user_id == id {
          True -> #(item.0, "")
          False -> item
        }
      })), effect.none())
    ToMyPage -> {
      #(model, effect.none())
    }

    ToHome -> {
      #(model, effect.none())
    }

    ToUserInfo(_user_info) -> {
      #(model, effect.none())
    }

    SearchInputChanged(value) -> {
      #(Model(..model, search_word: value), effect.none())
    }

    SearchSubmitted -> {
      #(model, effect.none())
    }

    FriendsLoaded(Ok(friends)) -> {
      #(
        Model(..model, friends: list.map(friends, fn(user) { FriendInfo(user, False) }), messages: []),
        effect.none(),
      )
    }

    FriendsLoaded(Error(ApiError(message))) -> {
      #(Model(..model, friends: [], messages: [message]), effect.none())
    }
  }
}

pub fn view (model: Model) -> element.Element(Msg) {
  case model.session {
    session.Guest -> {
      div([attribute.class("flex min-h-screen items-center justify-center bg-[#f7f5f0] p-5")], [
        div([attribute.class("p-8 text-center")], [
          h1([attribute.class("mb-3 text-xl font-bold text-gray-900")], [text("フレンド管理")]),
          p([attribute.class("mb-6 text-gray-600")], [text("ログインしてください")]),
          btn.to_home_btn_component(ToHome),
        ]),
      ])
    }

    session.Authenticated(_, _) -> {
      div([attribute.class("min-h-screen bg-[#f7f5f0] p-4 font-sans sm:p-6")], [
        div([attribute.class("mb-6 flex flex-wrap gap-5 text-sm text-[#58745a]")], [
          button([on_click(ToMyPage), attribute.class("hover:underline")], [text("← マイページに戻る")]),
        ]),
        div([attribute.class("mx-auto max-w-3xl")], [
          div([attribute.class("mb-5 flex flex-wrap items-end justify-between gap-3")], [
            div([], [
              h1([attribute.class("text-2xl font-semibold text-gray-900")], [text("フレンド管理")]),
              p([attribute.class("mt-1 text-sm text-gray-500")], [
                text("登録済みのフレンドを確認できます（最大50人）"),
              ]),
            ]),
            span_count(model.friends),
          ]),
          search_form(model.search_word),
          ui.error_messages(model.messages),
          friend_list(model.friends, model.icons),
        ]),
      ])
    }
  }
}

pub fn section_view(model: Model) -> element.Element(Msg) {
  div([], [
    div([attribute.class("mb-5 flex items-center justify-between")], [
      h2([attribute.class("text-lg font-semibold text-gray-900")], [text("フレンド")]),
      span_count(model.friends),
    ]),
    search_form(model.search_word),
    ui.error_messages(model.messages),
    friend_list(model.friends, model.icons),
  ])
}

fn search_form(search_word: String) -> element.Element(Msg) {
  div([attribute.class("mb-5 flex gap-2")], [
    input([
      attribute.class("flex-1 " <> ui.input_classes()),
      attribute.attribute("aria-label", "ユーザーを探す"),
      attribute.placeholder("ユーザーを探す"),
      attribute.value(search_word),
      on_input(SearchInputChanged),
    ]),
    button([
      attribute.class("shrink-0 rounded-md bg-[#58745a] px-4 py-2 text-sm font-semibold text-white transition hover:bg-[#46614a]"),
      on_click(SearchSubmitted),
    ], [text("検索")]),
  ])
}

fn span_count(friends: List(FriendInfo)) -> element.Element(Msg) {
  div([attribute.class("text-sm text-[#6f6a61]")], [
    text(int.to_string(list.length(friends)) <> " / 50人"),
  ])
}

fn friend_list(friends: List(FriendInfo), icons: List(#(UserInfo, String))) -> element.Element(Msg) {
  case friends {
    [] ->
      div([attribute.class("border-y border-[#dedbd2] py-12 text-center")], [
        h2([attribute.class("text-lg font-semibold text-gray-900")], [text("フレンドがいません")]),
        p([attribute.class("mt-2 text-sm text-gray-500")], [
          text("ユーザーを検索してフレンドに追加すると、ここに表示されます。"),
        ]),
      ])
    _ ->
      div([attribute.class("border-y border-[#dedbd2]")], [
        div([attribute.class("space-y-3")], list.map(friends, fn(friend) {
          div([attribute.class("flex items-center justify-between border-b border-[#dedbd2] py-4")], [
            div([attribute.class("flex min-w-0 items-center gap-3")], [
              div([attribute.class("h-10 w-10 shrink-0 overflow-hidden rounded-full bg-gray-200")], {
                let url = icons |> list.find(fn(item) { item.0.user_id == friend.user.user_id }) |> result.map(fn(item) { item.1 }) |> result.unwrap("")
                case url {
                  "" -> []
                  _ -> [img([attribute.src(url), attribute.alt(""), attribute.class("h-full w-full object-cover"), on("error", decode.success(IconFailed(friend.user.user_id)))])]
                }
              }),
              div([attribute.class("min-w-0")], [
                div([attribute.class("break-words font-medium text-gray-900")], [text(friend.user.name)]),
                span([attribute.class(status_classes(friend.online))], [text(status_label(friend.online))]),
              ]),
            ]),
            button([
              attribute.class(btn.navigation_button_classes() <> " px-3 py-1.5 text-xs"),
              on_click(ToUserInfo(friend.user)),
            ], [text("詳細を見る")]),
          ])
        })),
      ])
  }
}

fn status_classes(online: Bool) -> String {
  case online {
    True -> "text-xs text-[#58745a]"
    False -> "text-xs text-[#8a857c]"
  }
}

fn status_label(online: Bool) -> String {
  case online {
    True -> "オンライン"
    False -> "オフライン"
  }
}
