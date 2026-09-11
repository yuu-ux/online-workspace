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
    [attribute.class("min-h-screen bg-[#f7f5f0] px-4 py-5 sm:px-8")],
    [btn.to_home_btn_component(ToHome), div([attribute.class("mx-auto mt-10 max-w-3xl border-b border-[#dedbd2] pb-5 text-2xl font-semibold text-slate-900")], [text("利用規約")])]
  )
}
