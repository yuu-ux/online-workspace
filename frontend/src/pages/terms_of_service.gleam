import components/btn
import lustre/attribute
import lustre/effect
import lustre/element
import lustre/element/html.{div, text}
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
  div(
    [attribute.attribute("style", "padding: 20px; font-family: sans-serif;")],
    [text("Terms of Service"), btn.to_home_btn_component(ToHome)]
  )
}
