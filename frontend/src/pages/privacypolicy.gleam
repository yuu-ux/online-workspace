import components/btn
import lustre/event.{on_click}
import lustre/attribute
import lustre/element
import lustre/effect
import gleam/io
import lustre/element/html.{button, div, h1, h3, hr, span, style, text}

import types/session.{type Session}

pub type Model {
  Model
}

pub type Msg {
  ToHome
}

pub fn init(session: Session) -> #(Model, effect.Effect(Msg)) {
  #(
    Model,
    effect.none()
  )
}

pub fn update(model: Model, msg: Msg) -> #(Model, effect.Effect(Msg)) {
  case msg {
    ToHome -> {
      #(model, effect.none())
    }
  }
}

pub fn view (model: Model) -> element.Element(Msg) {
   div([
     attribute.attribute("style", "padding: 20px; font-family: sans-serif;")
   ],
   [
     text("Privacy Policy"),
     btn.to_home_btn_component(ToHome),
     // button([on_click(ToMyPage)], [text("mypage")])
   ])
}

