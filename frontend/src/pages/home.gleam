import components/btn
import components/ui
import gleam/int
import gleam/list
import lustre/event.{on_click}
import lustre/attribute.{class}
import lustre/element
import lustre/effect
import gleam/io
import lustre/element/html.{button, div, h1, h2, hr, span, style, text, nav, p, header, main, br}

import types/user.{type UserId}
import types/session.{type Session,type Token, Guest, Authenticated}
import types/room.{
  type CategoryType,
  type WorkStyleType,
  type VisibilityType,
  type RoomId,
  type RoomNameType,
  type DescriptionType,
  type RoomInfo,
  RoomNameType,
  DescriptionType,
  RoomId,
  Cat1,
  Cat2,
  Cat3,
  CasualChat,
  Quiet,
  Public,
  Invite,
  Friend,
  RoomInfo
} as room_t

import wrap/room.{
  type RoomMemberCount,
  connect_to_room_list,
  get_rooms,
  room_member_count_from_json,
  room_created_from_json,
}
import wrap/session as session_api
import wrap/api.{type ApiError, ApiError}
import components/rooms.{room_list_view}

pub type Model {
  Model(
    session: Session,
    rooms: List(RoomInfo),
    messages: List(String),
  )
}

pub type Msg {
  ToCreateRoom
  ToLogin
  ToLogout
  ToMyPage
  ToRoom(RoomId)
  RoomNotJoinable
  LeftRoom
  RoomsLoaded(Result(List(RoomInfo), ApiError))
  LogoutCompleted(Result(Nil, ApiError))
  WsMessageReceived(String)
}


pub fn init(session: Session) -> #(Model, effect.Effect(Msg)) {
  case session {
    Guest -> {
      #(
        Model(session: session, rooms: [], messages: []),
        effect.none()
      )
    }
    Authenticated(..) -> {
      #(
        Model(session: session, rooms: [], messages: []),
        get_rooms(RoomsLoaded),
      )
    }
  }
}

pub fn update(model: Model, msg: Msg) -> #(Model, effect.Effect(Msg)) {
  case msg {
    ToLogout -> #(model, session_api.logout_proc(LogoutCompleted))
    RoomNotJoinable ->
      #(Model(..model, messages: ["このルームは満員のため入室できません。"]), effect.none())
    LeftRoom -> #(Model(..model, messages: ["退室しました。"]), effect.none())
    RoomsLoaded(Ok(rooms)) ->
      #(
        Model(..model, rooms: rooms),
        connect_to_room_list(
          list.map(rooms, fn(room) { room.room_id }),
          WsMessageReceived,
        ),
      )
    RoomsLoaded(Error(ApiError(message))) ->
      #(Model(..model, messages: [message]), effect.none())
    LogoutCompleted(Ok(_)) ->
      #(Model(session: Guest, rooms: [], messages: []), effect.none())
    LogoutCompleted(Error(ApiError(message))) ->
      #(Model(..model, messages: [message]), effect.none())
    WsMessageReceived(message) -> {
      case room_member_count_from_json(message) {
        Ok(member_count) ->
          #(
            Model(
              ..model,
              rooms: list.map(model.rooms, update_room_member_count(_, member_count)),
            ),
            effect.none(),
          )
        Error(_) ->
          case room_created_from_json(message) {
            Ok(room) -> {
              let updated_rooms = add_or_replace_room(model.rooms, room)
              #(
                Model(..model, rooms: updated_rooms),
                connect_to_room_list(
                  list.map(updated_rooms, fn(current_room) { current_room.room_id }),
                  WsMessageReceived,
                ),
              )
            }
            Error(_) -> #(model, effect.none())
          }
      }
    }
    _ -> #(model, effect.none())
  }
}

// ---------------------------------------------------------
// Home画面のView
// ---------------------------------------------------------
pub fn view(model: Model) -> element.Element(Msg) {
  // アプリ全体のベースレイアウト：薄いグレー背景、全画面高さ
  div([class("flex min-h-screen flex-col bg-[#f7f5f0] text-slate-900")], [
    
    // --- 共通ヘッダー（ナビゲーションバー） ---
    top_navbar(model.session),

    // --- メインコンテンツ ---
    main(
      [class("mx-auto w-full max-w-7xl flex-1 px-4 py-8 sm:px-6 lg:px-8")],
      [
        case model.session {
          
          // -------------------------------------
          // ゲスト（未ログイン）向けの表示
          // -------------------------------------
          session.Guest -> guest_content()

          // -------------------------------------
          // ログイン済み向けの表示（ダッシュボード）
          // -------------------------------------
          session.Authenticated(_jwt, _user_id) -> authenticated_content(model)
        }
      ]
    )
  ])
}

// ---------------------------------------------------------
// ヘッダーナビゲーション（上部バー）
// ---------------------------------------------------------
fn top_navbar(current_session: session.Session) -> element.Element(Msg) {
  header(
    [class("sticky top-0 z-10 bg-[#f7f5f0]")],
    [
      div(
        [class("mx-auto max-w-7xl px-4 sm:px-6 lg:px-8")],
        [
          div(
            [class("flex h-16 items-center justify-between")],
            [
              // 左側：アプリのロゴ/タイトル
              div([class("flex shrink-0 items-center gap-3")], [
                span([class("flex h-8 w-8 items-center justify-center rounded-md bg-[#58745a] text-sm font-bold text-white")], [text("OW")]),
                h1([class("text-xl font-bold tracking-tight text-slate-900")], [text("Online Workspace")])
              ]),

              // 右側：ログイン状態に応じたアクションボタン
              nav([class("flex items-center gap-3")], [
                case current_session {
                  session.Guest -> {
                    // 未ログイン時はログインボタンのみ
                    btn.primary_button("ログイン", ToLogin)
                  }
                  session.Authenticated(..) -> {
                    // ログイン時はマイページとログアウト
                    div([class("flex items-center gap-2")], [
                      button([class(btn.navigation_button_classes() <> " px-4 py-2 text-sm"), on_click(ToMyPage)], [text("マイページ")]),
                      // ※ログアウトボタンは赤い danger_button 等を作ると綺麗です
                      button([class("shrink-0 whitespace-nowrap px-2 py-2 text-sm text-[#6f6a61] hover:text-slate-900"), on_click(ToLogout)], [text("ログアウト")])
                    ])
                  }
                }
              ])
            ]
          )
        ]
      )
    ]
  )
}

// ---------------------------------------------------------
// ゲスト用のメインコンテンツ（ヒーローセクション）
// ---------------------------------------------------------
fn guest_content() -> element.Element(Msg) {
  div(
    [class("flex flex-col items-center justify-center space-y-8 py-20 text-center")],
    [
      h2([class("text-4xl font-extrabold tracking-tight text-slate-900 sm:text-5xl")], [
        text("新しい働き方を、"),
        br([]), // ※必要に応じて br 要素を定義してください
        text("新しいワークスペースで。")
      ]),
      p([class("max-w-2xl text-xl text-[#6f6a61]")], [
        text("会話、集中、コラボレーション。目的に合わせたルームで、仲間と一緒に最高のパフォーマンスを発揮しましょう。")
      ]),
      div([class("w-48")], [
        btn.primary_button("今すぐ始める", ToLogin)
      ])
    ]
  )
}

// ---------------------------------------------------------
// ログイン済みのメインコンテンツ（ダッシュボード）
// ---------------------------------------------------------
fn authenticated_content(model: Model) -> element.Element(Msg) {
  div([class("mx-auto max-w-5xl space-y-5")], [
    
    // ダッシュボードの上部（タイトルと「部屋を作成」ボタン）
    div(
      [class("flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between")],
      [
        div([class("flex flex-wrap items-baseline gap-x-4 gap-y-1")], [
          h2([class("text-2xl font-semibold text-slate-900")], [text("ルーム一覧")]),
          span([class("text-xs text-[#6f6a61]")], case list.contains(model.messages, "退室しました。") {
            True -> [text("退室しました。")]
            False -> []
          }),
        ]),
        
        // 新規作成ボタン（目立たせる）
        div([class("w-full sm:w-auto")], [
          btn.primary_button("＋ 新しいルームを作成", ToCreateRoom)
        ])
      ]
    ),

    // ルーム一覧のグリッド表示
    // ※以前作成した room_list_view コンポーネントをここで呼び出します
    // ※ room_list_view 側で grid クラスを持たせている前提です
    case list.filter(model.messages, fn(message) { message != "退室しました。" }) {
      [] -> text("")
      errors -> ui.error_messages(errors)
    },
    room_list_view(model.rooms, ToRoom)
  ])
}

fn update_room_member_count(room: RoomInfo, member_count: RoomMemberCount) -> RoomInfo {
  case room.room_id == member_count.room_id {
    False -> room
    True -> {
      RoomInfo(
        ..room,
        current_members: member_count.current_members,
        joinable:
          room.status == "OPEN"
          && member_count.current_members < room.max_number_of_member,
      )
    }
  }
}

fn add_or_replace_room(rooms: List(RoomInfo), new_room: RoomInfo) -> List(RoomInfo) {
  case list.any(rooms, fn(room) { room.room_id == new_room.room_id }) {
    True ->
      list.map(rooms, fn(room) {
        case room.room_id == new_room.room_id {
          True -> new_room
          False -> room
        }
      })
    False -> [new_room, ..rooms]
  }
}
