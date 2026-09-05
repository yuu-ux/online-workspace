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

import wrap/room.{get_rooms}
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
  RoomsLoaded(Result(List(RoomInfo), ApiError))
  LogoutCompleted(Result(Nil, ApiError))
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
    RoomsLoaded(Ok(rooms)) ->
      #(Model(..model, rooms: rooms, messages: []), effect.none())
    RoomsLoaded(Error(ApiError(message))) ->
      #(Model(..model, messages: [message]), effect.none())
    LogoutCompleted(Ok(_)) ->
      #(Model(session: Guest, rooms: [], messages: []), effect.none())
    LogoutCompleted(Error(ApiError(message))) ->
      #(Model(..model, messages: [message]), effect.none())
    _ -> #(model, effect.none())
  }
}

// ---------------------------------------------------------
// Home画面のView
// ---------------------------------------------------------
pub fn view(model: Model) -> element.Element(Msg) {
  // アプリ全体のベースレイアウト：薄いグレー背景、全画面高さ
  div([class("min-h-screen bg-gray-50 flex flex-col")], [
    
    // --- 共通ヘッダー（ナビゲーションバー） ---
    top_navbar(model.session),

    // --- メインコンテンツ ---
    main(
      [class("flex-1 w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8")],
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
    [class("bg-white border-b border-gray-200 sticky top-0 z-10")],
    [
      div(
        [class("max-w-7xl mx-auto px-4 sm:px-6 lg:px-8")],
        [
          div(
            [class("flex justify-between items-center h-16")],
            [
              // 左側：アプリのロゴ/タイトル
              div([class("flex-shrink-0 flex items-center gap-2")], [
                // アイコンを置くとカッコいいですが、今は文字だけ
                span([class("text-2xl")], [text("⚡️")]), 
                h1([class("text-xl font-bold text-gray-900 tracking-tight")], [text("Online Workspace")])
              ]),

              // 右側：ログイン状態に応じたアクションボタン
              nav([class("flex items-center gap-4")], [
                case current_session {
                  session.Guest -> {
                    // 未ログイン時はログインボタンのみ
                    btn.primary_button("ログイン", ToLogin)
                  }
                  session.Authenticated(..) -> {
                    // ログイン時はマイページとログアウト
                    div([class("flex items-center gap-3")], [
                      btn.secondary_button("マイページ", ToMyPage),
                      // ※ログアウトボタンは赤い danger_button 等を作ると綺麗です
                      btn.logout_btn_component(ToLogout)
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
    [class("flex flex-col items-center justify-center text-center py-20 space-y-8")],
    [
      h2([class("text-4xl font-extrabold text-gray-900 sm:text-5xl")], [
        text("新しい働き方を、"),
        br([]), // ※必要に応じて br 要素を定義してください
        text("新しいワークスペースで。")
      ]),
      p([class("max-w-2xl text-xl text-gray-500")], [
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
  div([class("space-y-8")], [
    
    // ダッシュボードの上部（タイトルと「部屋を作成」ボタン）
    div(
      [class("flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4")],
      [
        div([], [
          h2([class("text-2xl font-bold text-gray-900")], [text("ルーム一覧")]),
          p([class("text-sm text-gray-500 mt-1")], [text("参加したいルームを選んで入室するか、新しいルームを作成してください。")])
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
    ui.error_messages(model.messages),
    room_list_view(model.rooms, ToRoom)
  ])
}
