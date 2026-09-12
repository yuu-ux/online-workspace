import components/btn
import lustre/attribute.{class}
import lustre/element
import lustre/element/html.{div}

pub type Msg {
  ToPrivacyPolicy
  ToTermsOfService
}

pub fn view() -> element.Element(Msg) {
  div(
    [class("fixed inset-x-0 bottom-0 z-20 border-t border-[#e5ded2] bg-[#fffdf9] px-4 py-3 text-center shadow-sm")],
    [
      div([class("flex flex-wrap items-center justify-center gap-4 text-sm text-[#6f6a61]")], [
        btn.to_privacypolicy_btn_component(ToPrivacyPolicy),
        btn.to_tos_btn_component(ToTermsOfService),
      ]),
    ],
  )
}
