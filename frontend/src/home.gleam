import lustre/event.{on_click}
import lustre/attribute
import lustre/element
import lustre/effect
import gleam/io
import lustre/element/html.{button, div, h1, h3, hr, span, style, text}

import session/session.{type Session}

pub type Model {
  Model(
    session: Session
  )
}

pub type Msg {
  ToLogin
  ToLogout
  ToMyPage
}

pub fn init(session: Session) -> #(Model, effect.Effect(Msg)) {
  #(
    Model(session: session),
    effect.none()
  )
}

pub fn update(model: Model, msg: Msg) -> #(Model, effect.Effect(Msg)) {
  case msg {
    ToLogin -> {}
    ToLogout -> {}
    ToMyPage -> {}
  }
}

pub fn view (model: Model) -> element.Element(Msg) {
  case model.session {
    session.Guest -> {
      div([
        attribute.attribute("style", "padding: 20px; font-family: sans-serif;")
      ],
      [
        text("Home (guest)"),
        button([on_click(ToLogin)], [text("login")]),
        // button([on_click(ToMyPage)], [text("mypage")])
      ])
    }
    session.Authenticated(jwt, user_id) -> {
      div([
        attribute.attribute("style", "padding: 20px; font-family: sans-serif;")
      ],
      [
        text("Home (logined)"),
        button([on_click(ToLogout)], [text("logout")]),
        button([on_click(ToMyPage)], [text("mypage")])
      ])
    }
  }
}

