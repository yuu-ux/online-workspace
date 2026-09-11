import components/btn
import gleam/list
import lustre/attribute.{class}
import lustre/effect
import lustre/element
import lustre/element/html.{div, h1, h2, text}

import types/session.{type Session}

pub type Model {
  Model
}

pub type Msg {
  ToHome
}

pub fn init(_session: Session) -> #(Model, effect.Effect(Msg)) {
  #(Model, effect.none())
}

pub fn update(model: Model, msg: Msg) -> #(Model, effect.Effect(Msg)) {
  case msg {
    ToHome -> #(model, effect.none())
  }
}

pub fn view(_model: Model) -> element.Element(Msg) {
  div([class("min-h-screen bg-gray-50 px-4 py-8 text-gray-800")], [
    div([class("mx-auto max-w-3xl")], [
      div([class("mb-6")], [
        h1([class("text-3xl font-bold text-gray-900")], [text("プライバシーポリシー")]),
        div([class("mt-2 text-sm text-gray-500")], [text("最終更新日: 2026年9月12日")]),
      ]),
      div([class("space-y-4")], [
        section("1. 基本方針", [
          "online-workspace 開発チーム（以下「運営者」といいます。）は、本サービスにおけるユーザーの個人情報を適切に取り扱います。",
          "本ポリシーは、運営者が取得する情報、その利用目的、管理方法およびユーザーの選択肢について説明するものです。",
        ]),
        section("2. 取得する情報", [
          "アカウントの登録・認証のため、メールアドレス、ユーザー名およびパスワードを取得します。パスワードは平文では保存せず、ハッシュ化して管理します。",
          "プロフィール機能のため、自己紹介、プロフィール画像のURL、プロフィール公開設定など、ユーザーが入力した情報を取得します。",
          "フレンド機能およびルーム機能のため、フレンド関係、ルームへの参加・退室、チャットメッセージおよびその送信時刻を取得・処理します。",
          "ログイン状態の維持、セキュリティ対策および障害調査のため、セッション情報および認証・認可・セキュリティ対策に関するログを取得します。",
        ]),
        section("3. 利用目的", [
          "取得した情報は、アカウントの作成・認証、ログイン状態の維持および本人確認のために利用します。",
          "プロフィール表示、ユーザー検索、フレンド管理、ルームへの入退室およびリアルタイムチャットを提供するために利用します。",
          "不正アクセスやなりすましを防止し、サービスの安全性を維持するために利用します。",
          "障害対応、問い合わせ対応およびセキュリティ対策のために利用します。",
        ]),
        section("4. プロフィール情報の公開", [
          "ユーザー名とプロフィール画像は、サービス内のユーザー検索、フレンド一覧およびルーム内で他のユーザーに表示される場合があります。",
          "プロフィールを非公開に設定した場合、他のユーザーには公開範囲を限定して表示します。ただし、フレンド状態など、サービスの機能提供に必要な情報が表示される場合があります。",
          "プロフィール画像に外部URLを設定した場合、画像の取得はユーザーのブラウザからそのURLの提供元に対して行われます。設定するURLは、信頼できる提供元のものを使用してください。",
        ]),
        section("5. 第三者提供・委託", [
          "運営者は、法令に基づく場合、本人の同意がある場合、または生命・身体・財産の保護のために必要な場合を除き、個人情報を第三者に提供しません。",
          "ただし、サービス運用に必要な範囲で、委託先に取り扱いを委託する場合があります。委託先を適切に選定し、必要な安全管理を求めます。",
        ]),
        section("6. 安全管理と保存期間", [
          "運営者は、アクセス制御、パスワードのハッシュ化、HTTPS通信その他の合理的な安全管理措置を講じます。",
          "個人情報は、利用目的の達成に必要な期間または法令上必要な期間保管します。アカウントの退会後はアカウントを利用できない状態にします。登録データは必要な範囲で保持し、自動削除・匿名化は行いません。",
        ]),
        section("7. ユーザーの権利", [
          "ユーザーは、法令に基づき、自己の個人情報の開示、訂正、利用停止、消去その他の請求を行うことができます。",
          "請求を希望する場合は、本人確認に必要な情報を添えて、運営者が別途案内する問い合わせ方法から連絡してください。法令上またはサービス運営上の理由により、請求に応じられない場合があります。",
        ]),
        section("8. ポリシーの変更", [
          "運営者は、法令の改正、サービス内容の変更その他の必要に応じて、本ポリシーを変更することがあります。変更内容は本ページに掲載します。重要な変更については、必要に応じてサービス上でお知らせします。",
          "変更後のポリシーは、本ページに掲載した時点または別途定めた効力発生日から適用します。",
        ]),
        section("9. 問い合わせ窓口", [
          "運営者: online-workspace 開発チーム / 問い合わせ先: kisaragi.12056 at gmail.com",
        ]),
      ]),
      div([class("mt-8")], [btn.to_home_btn_component(ToHome)]),
    ]),
  ])
}

fn section(title: String, paragraphs: List(String)) -> element.Element(Msg) {
  div([class("rounded-xl border border-gray-200 bg-white p-5 shadow-sm")], [
    h2([class("text-lg font-semibold text-gray-900")], [text(title)]),
    div(
      [class("mt-3 space-y-2 text-sm leading-6 text-gray-700")],
      list.map(paragraphs, fn(paragraph) { div([], [text(paragraph)]) }),
    ),
  ])
}
