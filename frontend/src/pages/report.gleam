// 通報
import lustre/event.{on_click, on_input}
import lustre/attribute
import lustre/element
import lustre/effect
import gleam/io
import lustre/element/html.{button, div, text, option, select, textarea}

import types/session.{type Session}
import types/room.{type RoomId}

import types/user.{type UserInfo} as user_t
import wrap/user.{report_user, type ReportReason, ViolationOfTerms, Other}

pub type InputType {
  ReportReason
  ReportDetail
}

pub type Model {
  Model(
    session: Session,
    room_id: RoomId,
    target_user_info: UserInfo,
    current_details: String,
    current_reason: String,
    messages: List(String)
  )
}

pub type Msg {
  ToHome
  BackToRoom
  InputUpdated(target: InputType, str: String)
  SubmitClicked
}

/// 通報するユーザーを引数に設定する
pub fn init(session: Session, room_id: RoomId, target_user_info: UserInfo) -> #(Model, effect.Effect(Msg)) {
  #(
    Model(
      session: session,
      room_id: room_id,
      target_user_info: target_user_info,
      current_details: "",
      current_reason: "Violation of Terms",
      messages: []),
    effect.none()
  )
}

pub fn update(model: Model, msg: Msg) -> #(Model, effect.Effect(Msg)) {
  case msg {
    ToHome -> {
      #(model, effect.none())
    }

    BackToRoom -> {
      #(model, effect.none())
    }

    // 文字が入力されたら、Modelの current_input をリアルタイムに書き換える
    InputUpdated(target, text) -> {
      let new_model = case target {
        ReportReason -> {
          Model(..model, current_reason: text)
        }
        ReportDetail -> {
          Model(..model, current_details: text)
        }
      }
      #(new_model, effect.none())
    }

    // 送信ボタンが押されたら、入力内容を履歴に追加し、入力欄を空にする
    SubmitClicked -> {
      io.println("current_details:" <> model.current_details)
      let reason = case model.current_reason {
        "Violation of Terms" -> {
          ViolationOfTerms
        }
        "Other" -> {
          Other
        }
        _ -> {
          Other
        }
      }

      case report_user(
        model.session,
        model.target_user_info,
        reason,
        model.current_details) {
        Ok(_) -> {
          //
        }
        Error(err_type) -> {
          //
        }
      }
    }
  }
}

pub fn view (model: Model) -> element.Element(Msg) {
  case model.session {
    session.Guest -> {
      div([
        attribute.attribute("style", "padding: 20px; font-family: sans-serif;")
      ],
      [
        text("通報"),
        text("ログインしてください"),
        button([on_click(ToHome)], [text("home")]),
      ])
    }

    session.Authenticated(jwt, user_id) -> {
      div([
        attribute.attribute("style", "padding: 20px; font-family: sans-serif;")
      ],
      [
        text("通報"),
        text("ユーザー" <> model.target_user_info.name <> "を通報する"),
        select(
          [
            on_input(InputUpdated(ReportReason, _)),
            attribute.value(model.current_reason)
          ], 
          [
            option([attribute.value("Violation of Terms")], "Violation of Terms"),
            option([attribute.value("Other")], "その他"),
          ]
        ),

        textarea(
          [
            on_input(InputUpdated(ReportDetail, _)),
            attribute.value(model.current_details)
          ],
          model.current_details
        ),
        button([on_click(SubmitClicked)], [text("通報を送信")]),
        button([on_click(BackToRoom)], [text("ルームに戻る")])
      ])
    }
  }
}

