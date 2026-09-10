import gleam/int
import gleam/list
import gleam/option.{type Option, Some}
import gleam/string
import lustre/attribute.{aria_disabled, class, disabled}
import lustre/element.{text}
import lustre/element/html.{button, div, span, h3}
import lustre/event.{on_click}

import types/room.{
  type RoomInfo,
  type CategoryType,
  type WorkStyleType,
  type RoomId,
  RoomNameType,
  Cat1,
  Cat2,
  Cat3,
  CasualChat,
  Quiet,
}

// ui/button など自作のコンポーネントがあれば、それを使ってもOKです

// --- View本体 ---

pub fn room_list_view(room_info_list: List(RoomInfo), to_room: fn(RoomId) -> a) -> element.Element(a) {
  div(
    // 画面全体に余白を取り、レスポンシブなグリッドレイアウトを設定
    [class("p-6")], 
    [
      div(
        // 画面幅に応じて、1列 -> 2列 -> 3列 とカードが並ぶようにする
        [class("grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6")],
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
    [class("bg-white border border-gray-200 rounded-xl shadow-sm hover:shadow-md transition-shadow duration-200 p-5 flex flex-col")],
    [
      // --- 上部：ルーム名とカテゴリバッジ ---
      div([class("flex justify-between items-start mb-4")], [
        h3([class("text-lg font-bold text-gray-900 truncate pr-4")], [
          text(room_name_str)
        ]),
        category_badge(room_info.category)
      ]),

      // --- 中部：詳細情報（ワークスタイルや人数） ---
      div([class("flex-1 space-y-3 mb-6")], [
        info_row("作業スタイル", work_style_to_string(room_info.work_style)),
        info_row(
          "参加人数",
          int.to_string(room_info.current_members)
            <> " / "
            <> int.to_string(room_info.max_number_of_member)
            <> " 人",
        ),
        info_row("作成日時", format_created_at(room_info.created_at)),
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
          class("w-full py-2.5 px-4 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-lg shadow-sm transition-colors duration-200"),
          on_click(to_room(room_info.room_id)),
        ],
        [text("このルームに入室する")],
      )
    False ->
      button(
        [
          class("w-full py-2.5 px-4 bg-gray-300 text-gray-600 font-semibold rounded-lg cursor-not-allowed"),
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

fn format_created_at(value: String) -> String {
  case string.split(value, "T") {
    [date, time, ..] -> {
      case string.split(time, ":") {
        [hour, minute, ..] -> date <> " " <> hour <> ":" <> minute
        _ -> value
      }
    }
    _ -> value
  }
}

// --- ヘルパー関数（さらに小さな部品） ---

/// 情報の行を綺麗に並べるヘルパー
fn info_row(label: String, value: String) -> element.Element(a) {
  div([class("flex items-center text-sm")], [
    span([class("text-gray-500 w-24")], [text(label)]),
    span([class("font-medium text-gray-800")], [text(value)])
  ])
}

/// カテゴリをカラフルな「バッジ」として表示するコンポーネント
fn category_badge(cat: CategoryType) -> element.Element(a) {
  // カテゴリによってバッジの色を変える
  let #(bg_color, text_color, label) = case cat {
    Cat1 -> #("bg-blue-100", "text-blue-800", "カテゴリ1")
    Cat2 -> #("bg-green-100", "text-green-800", "カテゴリ2")
    Cat3 -> #("bg-purple-100", "text-purple-800", "カテゴリ3")
  }
  
  span(
    [class("px-2.5 py-0.5 rounded-full text-xs font-semibold whitespace-nowrap " <> bg_color <> " " <> text_color)],
    [text(label)]
  )
}

/// ワークスタイルを文字列に変換（冗長なcaseをここにまとめる）
fn work_style_to_string(style: WorkStyleType) -> String {
  case style {
    CasualChat -> "雑談OK (Casual)"
    Quiet      -> "もくもく (Quiet)"
  }
}
