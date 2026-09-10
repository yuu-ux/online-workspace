// プロフィール編集画面
import gleam/list
import lustre/event.{on_click}
import lustre/attribute
import lustre/element
import lustre/effect
import lustre/element/html.{button, div, h1, label, p, text}

import types/session.{type Session}
import wrap/api.{type ApiError, ApiError}
import wrap/user as user_wrap
import components/input
import components/btn

pub type Model {
  Model(
    session: Session,
    name: String,
    icon_url: String,
    bio: String,
    work_category_id: Int,
    is_public: Bool,
    loading: Bool,
    messages: List(String)
  )
}

pub type Msg {
  ToHome
  ToMyPage
  InputName(String)
  InputIconUrl(String)
  InputBio(String)
  TogglePublic(Bool)
  SaveClicked
  ProfileLoaded(Result(user_wrap.MyProfile, user_wrap.MyProfileErr))
  ProfileSaved(Result(user_wrap.MyProfile, ApiError))
}

pub fn init(session: Session) -> #(Model, effect.Effect(Msg)) {
  #(
    Model(
      session: session, name: "", icon_url: "", bio: "", work_category_id: 0, is_public: True,
      loading: True, messages: []),
    user_wrap.get_my_profile(ProfileLoaded)
  )
}

pub fn update(model: Model, msg: Msg) -> #(Model, effect.Effect(Msg)) {
  case msg {
    ToMyPage -> {
      #(model, effect.none())
    }
    ToHome -> {
      #(model, effect.none())
    }

    InputName(value) -> #(Model(..model, name: value), effect.none())
    InputIconUrl(value) -> #(Model(..model, icon_url: value), effect.none())
    InputBio(value) -> #(Model(..model, bio: value), effect.none())
    TogglePublic(value) -> #(Model(..model, is_public: value), effect.none())

    SaveClicked ->
      #(
        Model(..model, loading: True, messages: []),
        user_wrap.update_my_profile(
          model.name,
          model.icon_url,
          model.bio,
          model.work_category_id,
          model.is_public,
          ProfileSaved,
        ),
      )

    ProfileLoaded(Ok(profile)) ->
      #(
        Model(
          ..model,
          name: profile.name,
          icon_url: profile.icon_url,
          bio: profile.bio,
          work_category_id: profile.work_category_id,
          is_public: profile.is_public,
          loading: False,
          messages: [],
        ),
        effect.none(),
      )

    ProfileLoaded(Error(user_wrap.MyProfileApiErr(ApiError(message)))) ->
      #(Model(..model, loading: False, messages: [message]), effect.none())

    ProfileSaved(Ok(profile)) ->
      #(
        Model(
          ..model,
          name: profile.name,
          icon_url: profile.icon_url,
          bio: profile.bio,
          work_category_id: profile.work_category_id,
          is_public: profile.is_public,
          loading: False,
          messages: ["プロフィールを保存しました"],
        ),
        effect.none(),
      )

    ProfileSaved(Error(ApiError(message))) ->
      #(Model(..model, loading: False, messages: [message]), effect.none())
  }
}

pub fn view (model: Model) -> element.Element(Msg) {
  case model.session {
    session.Guest -> {
      div([attribute.class("flex min-h-screen items-center justify-center bg-gray-50 p-5")], [
        div([attribute.class("rounded-xl border border-gray-200 bg-white p-8 text-center shadow-sm")], [
          h1([attribute.class("mb-3 text-xl font-bold text-gray-900")], [text("プロフィール編集")]),
          p([attribute.class("mb-6 text-gray-600")], [text("ログインしてください")]),
          btn.to_home_btn_component(ToHome),
        ]),
      ])
    }

    session.Authenticated(_, _) -> {
      div([attribute.class("min-h-screen bg-gray-50 p-4 font-sans sm:p-6")], [
        div([attribute.class("mx-auto max-w-2xl")], [
          div([attribute.class("mb-5")], [
            h1([attribute.class("text-2xl font-bold text-gray-900")], [text("プロフィール編集")]),
            p([attribute.class("mt-1 text-sm text-gray-500")], [
              text("プロフィール情報と公開設定を変更できます"),
            ]),
          ]),
          div([attribute.class("rounded-xl border border-gray-200 bg-white p-5 shadow-sm sm:p-8")], [
            div([attribute.class("space-y-5")], [
              field("名前", input.normal_input(InputName, model.name)),
              field("アイコンURL", input.normal_input(InputIconUrl, model.icon_url)),
              field("自己紹介", input.normal_input(InputBio, model.bio)),
              div([attribute.class("flex items-center justify-between rounded-lg border border-gray-200 p-4")], [
                div([], [
                  div([attribute.class("font-semibold text-gray-900")], [text("公開設定")]),
                  p([attribute.class("mt-1 text-xs text-gray-500")], [
                    text("プロフィールを他のユーザーに公開します"),
                  ]),
                ]),
                button([
                  attribute.class(case model.is_public {
                    True -> "rounded-full bg-green-100 px-4 py-2 text-sm font-semibold text-green-700 transition hover:bg-green-200"
                    False -> "rounded-full bg-gray-100 px-4 py-2 text-sm font-semibold text-gray-600 transition hover:bg-gray-200"
                  }),
                  on_click(TogglePublic(toggle(model.is_public))),
                ], [
                  text(case model.is_public { True -> "公開" False -> "非公開" }),
                ]),
              ]),
            ]),
            div([attribute.class("mt-6 space-y-3")], list.map(model.messages, fn(message) {
              div([attribute.class("rounded-lg border border-blue-200 bg-blue-50 p-3 text-sm text-blue-700")], [
                text(message),
              ])
            })),
            div([attribute.class("mt-6 flex flex-col gap-3 sm:flex-row-reverse")], [
              button([
                attribute.class("rounded-lg bg-blue-600 px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60"),
                on_click(SaveClicked),
              ], [text(case model.loading { True -> "保存中..." False -> "プロフィールを保存" })]),
              button([
                attribute.class("rounded-lg border border-gray-300 bg-white px-5 py-2.5 text-sm font-semibold text-gray-700 transition hover:bg-gray-50"),
                on_click(ToMyPage),
              ], [text("マイページに戻る")]),
            ]),
          ]),
        ]),
      ])
    }
  }
}

fn field(label_text: String, control: element.Element(Msg)) -> element.Element(Msg) {
  div([attribute.class("space-y-1.5")], [
    label([attribute.class("block text-sm font-semibold text-gray-700")], [text(label_text)]),
    control,
  ])
}

fn toggle(value: Bool) -> Bool {
  case value {
    True -> False
    False -> True
  }
}


