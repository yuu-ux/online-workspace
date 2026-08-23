import gleam/list
import lustre/attribute.{class, placeholder, type_, value}
import lustre/element.{type Element, text}
import lustre/element/html.{div, h2, input, label, p, select, textarea, option}
import lustre/event.{on_input}

// --- セクション単位のコンポーネント ---

/// テキストエリア（複数行入力）
pub fn text_area(
  label_text: String,
  placeholder_text: String,
  current_value: String,
  on_change: fn(String) -> a,
) -> Element(a) {
  div([], [
    label([class("block text-sm font-medium text-gray-700 mb-1")], [text(label_text)]),
    textarea(
      [
        placeholder(placeholder_text),
        value(current_value),
        on_input(on_change),
        // text_input と同じ美しい枠線とリング、高さを固定(rows-4相当)
        class("w-full px-4 py-2 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors resize-none h-32")
      ],
      current_value
    )
  ])
}

/// セレクトボックス（ドロップダウン）
pub fn select_box(
  label_text: String,
  current_value: String,
  options_list: List(#(String, String)), // #(value, 表示名) のリスト
  on_change: fn(String) -> a,
) -> Element(a) {
  div([], [
    label([class("block text-sm font-medium text-gray-700 mb-1")], [text(label_text)]),
    select(
      [
        value(current_value),
        on_input(on_change),
        // ブラウザデフォルトの矢印を使いつつ、枠線などを整える
        class("w-full px-4 py-2 border border-gray-300 rounded-lg shadow-sm bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors")
      ],
      // 選択肢のリストから option タグを生成
      list.map(options_list, fn(opt) {
        let #(opt_val, opt_label) = opt
        option([value(opt_val)], opt_label)
      })
    )
  ])
}

/// 数値入力欄
pub fn number_input(
  label_text: String,
  current_value: String,
  on_change: fn(String) -> a,
) -> Element(a) {
  div([], [
    label([class("block text-sm font-medium text-gray-700 mb-1")], [text(label_text)]),
    input([
      type_("number"),
      value(current_value),
      on_input(on_change),
      class("w-full px-4 py-2 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors")
    ])
  ])
}

/// 画面全体を覆い、中央に白いカードを配置する汎用レイアウト
/// 
/// 引数:
/// - card_children: カードの中に配置する要素のリスト
/// - footer_element: カードの下に配置するフッター要素
pub fn centered_card_layout(
  card_children: List(Element(a)),
  footer_element: Element(a),
) -> Element(a) {
  div(
    // 画面全体（背景と中央寄せ）
    [class("min-h-screen bg-gray-50 flex flex-col justify-center items-center py-12 px-4 sm:px-6 lg:px-8")],
    [
      // 中央の白いカード
      div(
        [class("max-w-md w-full bg-white rounded-xl shadow-lg p-8 space-y-8")],
        card_children
      ),
      // フッターリンク
      footer_element
    ]
  )
}

/// 認証済み画面用など、カードの中身も中央寄せ＆少し余白が狭い(space-y-6)レイアウト
pub fn centered_text_card_layout(
  card_children: List(Element(a)),
  footer_element: Element(a),
) -> Element(a) {
  div(
    [class("min-h-screen bg-gray-50 flex flex-col justify-center items-center py-12 px-4 sm:px-6 lg:px-8")],
    [
      div(
        // text-center と space-y-6 がポイント
        [class("max-w-md w-full bg-white rounded-xl shadow-lg p-8 space-y-6 text-center")],
        card_children
      ),
      footer_element
    ]
  )
}

/// タイトルとサブタイトルのセクション
pub fn header_section(title: String, subtitle: String) -> Element(a) {
  div([class("text-center")], [
    h2([class("text-3xl font-extrabold text-gray-900")], [text(title)]),
    p([class("mt-2 text-sm text-gray-600")], [text(subtitle)])
  ])
}

/// エラーメッセージのリスト表示セクション
pub fn error_messages(messages: List(String)) -> Element(a) {
  div(
    [class("flex flex-col gap-2")],
    list.map(messages, fn(msg) {
      div([class("bg-red-50 text-red-600 text-sm p-3 rounded-md border border-red-200")], [text(msg)])
    })
  )
}

///「または」の区切り線
pub fn divider_with_text(label_text: String) -> Element(a) {
  div([class("relative my-6")], [
    div([class("absolute inset-0 flex items-center")], [
      div([class("w-full border-t border-gray-200")], [])
    ]),
    div([class("relative flex justify-center text-sm")], [
      p([class("px-2 bg-white text-gray-500")], [text(label_text)])
    ])
  ])
}

// --- 汎用的な UI 部品（デザインシステム） ---

/// ラベル付きの入力欄（再利用可能）
pub fn text_input(
  label_text: String,
  input_type: String,
  placeholder_text: String,
  current_value: String,
  on_change: fn(String) -> a,
) -> Element(a) {
  div([], [
    label([class("block text-sm font-medium text-gray-700 mb-1")], [text(label_text)]),
    input([
      type_(input_type),
      placeholder(placeholder_text),
      value(current_value),
      on_input(on_change),
      class("w-full px-4 py-2 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors")
    ])
  ])
}

