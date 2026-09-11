import gleam/int
import gleam/list
import gleam/option.{type Option, Some}
import lustre/attribute.{aria_disabled, class, disabled}
import lustre/element.{text}
import lustre/element/html.{button, div, span, h3}
import lustre/event.{on_click}

import types/room.{
  type RoomInfo,
  type WorkStyleType,
  type RoomId,
  RoomNameType,
  CasualChat,
  Quiet,
}

// ui/button など自作のコンポーネントがあれば、それを使ってもOKです

// --- View本体 ---

pub fn room_list_view(room_info_list: List(RoomInfo), to_room: fn(RoomId) -> a) -> element.Element(a) {
  div(
    // 画面全体に余白を取り、レスポンシブなグリッドレイアウトを設定
    [class("p-0")],
    [
      div(
        // 画面幅に応じて、1列 -> 2列 -> 3列 とカードが並ぶようにする
        [class("divide-y divide-[#dedbd2] border-y border-[#dedbd2]")],
        list.map(room_info_list, room_card(_, to_room))
      )
    ]
  )
}

// --- カードコンポーネント（1つのルームを描画） ---

fn room_card(room_info: RoomInfo, to_room: fn(RoomId) -> a) -> element.Element(a) {
  // RoomNameType(a) から文字列を取り出す処理を変数に入れておく
  let room_name_str = case room_info.roomname {
    RoomNameType(a) -> a
  }

  div(
    // カード全体のスタイル：白背景、角丸、薄い影、ホバーで少し浮き上がるアニメーション
    [class("grid grid-cols-[minmax(0,1fr)_auto] items-center gap-x-5 gap-y-3 py-5 sm:grid-cols-[minmax(0,1fr)_auto_auto] sm:gap-x-5")],
    [
      // --- 上部：ルーム名と作業スタイル ---
      div([class("min-w-0 space-y-2")], [
        h3([class("break-words text-lg font-semibold text-slate-900")], [
          text(room_name_str)
        ]),
        div([class("flex flex-wrap items-center gap-3 text-sm text-[#6f6a61]")], [
          text(work_style_to_string(room_info.work_style)),
          span([class("text-xs text-[#847e74]")], [text("作成 " <> format_created_at_jst(room_info.created_at))]),
        ]),
      ]),

      // --- 中部：詳細情報（ワークスタイルや人数） ---
      div([class("row-start-2 text-sm sm:row-auto")], [
        info_row(
          "参加人数",
          int.to_string(room_info.current_members)
            <> " / "
            <> int.to_string(room_info.max_number_of_member)
            <> " 人",
        ),
      ]),

      // --- 下部：入室ボタン ---
      join_button(room_info, to_room),
    ]
  )
}

fn join_button(room_info: RoomInfo, to_room: fn(RoomId) -> a) -> element.Element(a) {
  case room_info.joinable {
    True ->
      button(
        [
          class("col-start-2 row-start-1 rounded-md bg-[#58745a] px-5 py-2 text-sm font-semibold text-white hover:bg-[#46614a] sm:col-start-3"),
          on_click(to_room(room_info.room_id)),
        ],
        [text("入室")],
      )
    False ->
      button(
        [
          class("col-start-2 row-start-1 max-w-32 cursor-not-allowed rounded-md bg-[#e5e0d7] px-4 py-2 text-sm text-[#8d887f] sm:col-start-3"),
          disabled(True),
          aria_disabled(True),
        ],
        [text(restriction_label(room_info.join_restriction))],
      )
  }
}

fn restriction_label(restriction: Option(String)) -> String {
  case restriction {
    Some("FULL") -> "満員のため入室できません"
    Some("CLOSED") -> "終了したルームです"
    _ -> "入室できません"
  }
}

@external(javascript, "./../ffi/date.js", "format_created_at_jst")
fn format_created_at_jst(value: String) -> String

// --- ヘルパー関数（さらに小さな部品） ---

/// 情報の行を綺麗に並べるヘルパー
fn info_row(label: String, value: String) -> element.Element(a) {
  div([class("flex items-center text-sm")], [
    span([class("mr-2 text-xs text-[#847e74]")], [text(label)]),
    span([class("font-medium text-slate-800")], [text(value)])
  ])
}

/// ワークスタイルを文字列に変換（冗長なcaseをここにまとめる）
fn work_style_to_string(style: WorkStyleType) -> String {
  case style {
    CasualChat -> "雑談OK"
    Quiet      -> "もくもく"
  }
}
