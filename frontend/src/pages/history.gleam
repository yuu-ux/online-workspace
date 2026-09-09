// 作業履歴
import components/btn
import gleam/int
import gleam/list
import lustre/event.{on_click}
import lustre/attribute
import lustre/element
import lustre/effect
import lustre/element/html.{button, div, text}

import types/session.{type Session} as session_t
import wrap/api.{ApiError, type ApiError}
import wrap/workhistory.{type WorkHistory, list_work_histories}

pub type Model {
  Model(
    session: Session,
    items: List(WorkHistory),
    messages: List(String)
  )
}

pub type Msg {
  ToHome
  ToMyPage
  HistoriesLoaded(Result(List(WorkHistory), ApiError))
}

pub fn init(session: Session) -> #(Model, effect.Effect(Msg)) {
  case session {
    session_t.Guest ->
      #(
        Model(session: session, items: [], messages: ["ログインしてください"]),
        effect.none(),
      )
    session_t.Authenticated(..) ->
      #(
        Model(session: session, items: [], messages: []),
        list_work_histories(HistoriesLoaded),
      )
  }
}

pub fn update(model: Model, msg: Msg) -> #(Model, effect.Effect(Msg)) {
  case msg {
    ToMyPage -> {
      #(model, effect.none())
    }
    ToHome -> {
      #(model, effect.none())
    }
    HistoriesLoaded(Ok(items)) -> {
      #(Model(..model, items: items, messages: []), effect.none())
    }
    HistoriesLoaded(Error(ApiError(message))) -> {
      #(Model(..model, messages: [message]), effect.none())
    }
  }
}

pub fn view (model: Model) -> element.Element(Msg) {
  case model.session {
    session_t.Guest -> {
      div([
        attribute.attribute("style", "padding: 20px; font-family: sans-serif;")
      ],
      [
        text("作業履歴"),
        text("ログインしてください"),
        btn.to_home_btn_component(ToHome),
      ])
    }

    session_t.Authenticated(_, _) -> {
      div([
        attribute.attribute("style", "padding: 20px; font-family: sans-serif;")
      ],
      [
        text("作業履歴"),
        div([], list.map(model.items, history_view)),
        button([
          on_click(ToMyPage)
        ], [text("マイページに戻る")]),
        div([], list.map(model.messages, fn (x) {div([], [text(x)])}))
      ])

    }
  }
}

fn history_view(item: WorkHistory) -> element.Element(Msg) {
  div([], [
    text("ルーム: " <> item.room_name),
    text(" / 開始: " <> item.joined_at),
    text(" / 終了: " <> item.left_at),
    text(" / 作業分: " <> int.to_string(item.duration_minutes)),
  ])
}


