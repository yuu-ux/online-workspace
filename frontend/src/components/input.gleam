import lustre/element
import lustre/element/html.{button, div, text, option, select, input}
import lustre/event.{on_click, on_input}
import lustre/attribute.{class}

fn input_design() -> attribute.Attribute(a) {
  class(
    "px-3 py-2 bg-white border border-gray-300 rounded-md shadow-sm " <>
    "placeholder-gray-400 " <>
    "focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500 focus:ring-opacity-50 " <>
    "transition-all duration-200 ease-in-out sm:text-sm",
  )
}

/// 入力欄(input)
pub fn normal_input(msg: fn(String) -> a, value: String) -> element.Element(a) {
  input([
    input_design(),
    on_input(msg),
    attribute.value(value)
  ])
}
