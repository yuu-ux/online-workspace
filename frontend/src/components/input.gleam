import lustre/element
import components/ui
import lustre/element/html.{button, div, text, option, select, input}
import lustre/event.{on_click, on_input}
import lustre/attribute.{class}

fn input_design() -> attribute.Attribute(a) {
  class("w-full " <> ui.input_classes())
}

/// 入力欄(input)
pub fn normal_input(msg: fn(String) -> a, value: String) -> element.Element(a) {
  input([
    input_design(),
    on_input(msg),
    attribute.value(value)
  ])
}
