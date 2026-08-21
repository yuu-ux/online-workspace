import lustre/element
import lustre/element/html.{button, div, text, option, select, input}
import lustre/event.{on_click, on_input}
import lustre/attribute.{class}

fn button_design() -> attribute.Attribute(a) {
  class("bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded")
}

fn normal_btn(msg: a, str: String) -> element.Element(a) {
  button(
    [
      button_design(),
      on_click(msg)
    ],
    [text(str)]
  )
}

/// メインのアクションボタン（青色）
pub fn primary_button(label: String, on_click_msg: a) -> element.Element(a) {
  button(
    [
      on_click(on_click_msg),
      class("w-full py-2.5 px-4 bg-blue-600 hover:bg-blue-700 text-white font-bold rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors")
    ],
    [text(label)]
  )
}

/// サブのアクションボタン（白背景）
pub fn secondary_button(label: String, on_click_msg: a) -> element.Element(a) {
  button(
    [
      on_click(on_click_msg),
      class("w-full py-2.5 px-4 bg-white border border-gray-300 hover:bg-gray-50 text-gray-700 font-semibold rounded-lg shadow-sm transition-colors")
    ],
    [text(label)]
  )
}

/// フッター用のテキストリンク風ボタン
pub fn link_button(label: String, on_click_msg: a) -> element.Element(a) {
  button(
    [on_click(on_click_msg), class("hover:text-gray-900 transition-colors")], 
    [text(label)]
  )
}


pub fn to_home_btn_component(msg: a) -> element.Element(a) {
  normal_btn(msg, "ホームへ")
}

pub fn to_room_btn_component(msg: a) -> element.Element(a) {
  normal_btn(msg, "ルームへ")
}

/// フレンド管理ボタン
pub fn to_friend_btn_component(msg: a) -> element.Element(a) {
  normal_btn(msg, "フレンド管理")
}

/// プロファイル編集ボタン
pub fn to_profile_btn_component(msg: a) -> element.Element(a) {
  normal_btn(msg, "プロファイル編集")
}

/// 作業履歴ボタン
pub fn to_history_btn_component(msg: a) -> element.Element(a) {
  normal_btn(msg, "作業履歴")
}

/// 検索ボタン
pub fn search_btn_component(msg: a) -> element.Element(a) {
  normal_btn(msg, "検索")
}

/// ログインボタン
pub fn login_btn_component(msg: a) -> element.Element(a) { 
  normal_btn(msg, "ログイン")
}

/// ログアウトボタン
pub fn logout_btn_component(msg: a) -> element.Element(a) {
  normal_btn(msg, "ログアウト")
}

/// ルーム作成ボタン
pub fn create_room_btn_component(msg: a) -> element.Element(a) {
  normal_btn(msg, "ルームを作成")
}

/// マイページ
pub fn mypage_btn_component(msg: a) -> element.Element(a) {
  normal_btn(msg, "マイページ")
}

/// 新規登録ボタン
pub fn register_btn_component(msg: a) -> element.Element(a) {
  normal_btn(msg, "新規ユーザー登録")
}

/// プライバシーポリシーボタン
pub fn to_privacypolicy_btn_component(msg: a) -> element.Element(a) {
  link_button("プライバシーポリシー", msg)
}

/// 利用規約ボタン
pub fn to_tos_btn_component(msg: a) -> element.Element(a) {
  link_button("利用規約", msg)
}
