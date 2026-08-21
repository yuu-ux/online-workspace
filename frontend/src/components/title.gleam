// ページタイトル
import lustre/element
import lustre/element/html.{button, div, text, option, select, input}

/// ページ上部に表示するタイトル
pub fn title_component(title: String) -> element.Element(a) {
  div([], [
    text(title)
  ])
}
