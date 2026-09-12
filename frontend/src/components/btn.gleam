import lustre/element
import lustre/element/html.{button, div, text, option, select, input}
import lustre/event.{on_click, on_input}
import lustre/attribute.{class}

fn button_design() -> attribute.Attribute(a) {
  class("whitespace-nowrap rounded-md bg-[#58745a] px-4 py-2 font-bold text-white transition-colors hover:bg-[#46614a]")
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

/// メインのアクションボタン
pub fn primary_button(label: String, on_click_msg: a) -> element.Element(a) {
  button(
    [
      on_click(on_click_msg),
      class("w-full rounded-md bg-[#58745a] px-4 py-2.5 font-bold text-white transition-colors hover:bg-[#46614a] focus:outline-none focus:ring-2 focus:ring-[#58745a] focus:ring-offset-2")
    ],
    [text(label)]
  )
}

pub fn navigation_button_classes() -> String {
  "shrink-0 rounded-md border border-[#ded8ce] bg-[#fcfaf5] font-medium text-[#454b43] transition-colors hover:bg-[#f2eee5] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#58745a]"
}

/// サブのアクションボタン
pub fn secondary_button(label: String, on_click_msg: a) -> element.Element(a) {
  button(
    [
      on_click(on_click_msg),
      class("w-full rounded-md border border-[#d8d1c5] bg-white px-4 py-2.5 font-semibold text-slate-700 transition-colors hover:bg-[#f3f0e9]")
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
  button([on_click(msg), class("shrink-0 whitespace-nowrap text-sm font-medium text-[#58745a] hover:underline")], [text("← ホームへ")])
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
