import gleam/list
import lustre/attribute.{attribute, class, placeholder, type_, value}
import lustre/element.{type Element, text}
import lustre/element/html.{div, h2, input, label, p, select, textarea, option}
import lustre/event.{on_input}

pub fn input_classes() -> String {
  "h-11 min-w-0 rounded-md border-2 border-[#c8c1b5] bg-transparent px-3 py-2 text-sm text-slate-800 placeholder:text-[#9a958c] transition-colors focus:border-[#58745a] focus:outline-none"
}

// --- セクション単位のコンポーネント ---

/// テキストエリア（複数行入力）
pub fn text_area(
  label_text: String,
  placeholder_text: String,
  current_value: String,
  on_change: fn(String) -> a,
) -> Element(a) {
  div([], [
    label([class("mb-1 block text-sm font-medium text-slate-700")], [text(label_text)]),
    textarea(
      [
        placeholder(placeholder_text),
        value(current_value),
        on_input(on_change),
        // text_input と同じ美しい枠線とリング、高さを固定(rows-4相当)
        class("h-32 w-full resize-none rounded-md border-2 border-[#c8c1b5] bg-transparent px-3 py-2 text-sm text-slate-800 placeholder:text-[#9a958c] transition-colors focus:border-[#58745a] focus:outline-none")
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
    label([class("mb-1 block text-sm font-medium text-slate-700")], [text(label_text)]),
    select(
      [
        value(current_value),
        on_input(on_change),
        // ブラウザデフォルトの矢印を使いつつ、枠線などを整える
        class("w-full " <> input_classes())
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
    label([class("mb-1 block text-sm font-medium text-slate-700")], [text(label_text)]),
    input([
      type_("number"),
      attribute("min", "2"),
      attribute("max", "12"),
      value(current_value),
      on_input(on_change),
      class("w-full " <> input_classes())
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
    [class("flex min-h-screen flex-col bg-[#f7f5f0] px-4 py-5 sm:px-8")],
    [
      div([class("self-start")], [footer_element]),
      // 中央の白いカード
      div(
        [class("mx-auto mt-10 w-full max-w-md space-y-6 pb-10")],
        card_children
      )
    ]
  )
}

/// 認証済み画面用など、カードの中身も中央寄せ＆少し余白が狭い(space-y-6)レイアウト
pub fn centered_text_card_layout(
  card_children: List(Element(a)),
  footer_element: Element(a),
) -> Element(a) {
  div(
    [class("flex min-h-screen flex-col bg-[#f7f5f0] px-4 py-5 sm:px-8")],
    [
      div([class("self-start")], [footer_element]),
      div(
        [class("mx-auto mt-10 w-full max-w-md space-y-6 pb-10")],
        card_children
      )
    ]
  )
}

/// タイトルとサブタイトルのセクション
pub fn header_section(title: String, subtitle: String) -> Element(a) {
  div([], [
    h2([class("text-2xl font-semibold text-slate-900")], [text(title)]),
    p([class("mt-2 text-sm text-[#6f6a61]")], [text(subtitle)])
  ])
}

/// エラーメッセージのリスト表示セクション
pub fn error_messages(messages: List(String)) -> Element(a) {
  div(
    [class("flex flex-col gap-2")],
    list.map(messages, fn(msg) {
      div([class("rounded-md border border-[#e8c9c1] bg-[#fff3f0] p-3 text-sm text-[#9a4a3c]")], [text(msg)])
    })
  )
}

pub fn success_messages(messages: List(String)) -> Element(a) {
  div(
    [class("flex flex-col gap-2")],
    list.map(messages, fn(msg) {
      div([class("rounded-md border border-[#c9dcc7] bg-[#f0f6ee] p-3 text-sm text-[#36553b]")], [text(msg)])
    })
  )
}

///「または」の区切り線
pub fn divider_with_text(label_text: String) -> Element(a) {
  div([class("relative my-6")], [
    div([class("absolute inset-0 flex items-center")], [
      div([class("w-full border-t border-[#e5ded2]")], [])
    ]),
    div([class("relative flex justify-center text-sm")], [
      p([class("bg-[#fffdf9] px-2 text-[#847e74]")], [text(label_text)])
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
    label([class("mb-1 block text-sm font-medium text-slate-700")], [text(label_text)]),
    input([
      type_(input_type),
      placeholder(placeholder_text),
      value(current_value),
      on_input(on_change),
      class("w-full " <> input_classes())
    ])
  ])
}
