import lustre/attribute
import lustre/effect
import lustre/element
import lustre/event.{on_click}
import lustre/element/html.{button, div, text}
import types/session.{type Session}
import types/user.{type UserInfo}

pub type Model {
  Model(session: Session, target: UserInfo)
}

pub type Msg {
  ToHome
}

pub fn init(session: Session, target: UserInfo) -> #(Model, effect.Effect(Msg)) {
  #(Model(session: session, target: target), effect.none())
}

pub fn update(model: Model, msg: Msg) -> #(Model, effect.Effect(Msg)) {
  case msg {
    ToHome -> #(model, effect.none())
  }
}

pub fn view(model: Model) -> element.Element(Msg) {
  div(
    [attribute.attribute("style", "padding: 20px; font-family: sans-serif;")],
    [
      text("通報機能"),
      div([], [text(model.target.name <> " さんへの通報画面です（実装準備中）")]),
      button([on_click(ToHome)], [text("ホームに戻る")]),
    ],
  )
}
