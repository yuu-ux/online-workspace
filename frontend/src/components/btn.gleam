import lustre/element
import lustre/element/html.{button, div, text, option, select, input}
import lustre/event.{on_click, on_input}
import lustre/attribute.{class}

fn button_design() -> attribute.Attribute(a) {
  class("bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded")
}

fn primary_btn(msg: a, str: String) -> element.Element(a) {
  button(
    [
      button_design(),
      on_click(msg)
    ],
    [text(str)]
  )
}

pub fn to_home_btn_component(msg: a) -> element.Element(a) {
  primary_btn(msg, "ホームへ")
}

pub fn to_room_btn_component(msg: a) -> element.Element(a) {
  primary_btn(msg, "ルームへ")
}

