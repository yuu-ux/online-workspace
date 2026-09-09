// プロフィール編集画面
import components/btn
import components/ui
import gleam/list
import lustre/attribute
import lustre/effect
import lustre/element
import lustre/event.{on_click}
import lustre/element/html.{button, div, text}

import types/session.{type Session}
import wrap/api.{ApiError, type ApiError}
import wrap/user.{MyProfile, get_my_profile, type MyProfile, update_my_profile}

pub type Model {
  Model(
    session: Session,
    id: Int,
    name: String,
    email: String,
    bio: String,
    is_public: Bool,
    messages: List(String),
  )
}

pub type Msg {
  ToHome
  ToMyPage
  NameUpdated(String)
  BioUpdated(String)
  VisibilityUpdated(String)
  SubmitClicked
  ProfileLoaded(Result(MyProfile, ApiError))
  ProfileSaved(Result(MyProfile, ApiError))
}

pub fn init(session: Session) -> #(Model, effect.Effect(Msg)) {
  let empty_model = Model(
    session: session,
    id: 0,
    name: "",
    email: "",
    bio: "",
    is_public: True,
    messages: [],
  )

  case session {
    session.Guest -> #(empty_model, effect.none())
    session.Authenticated(..) -> #(empty_model, get_my_profile(ProfileLoaded))
  }
}

pub fn update(model: Model, msg: Msg) -> #(Model, effect.Effect(Msg)) {
  case msg {
    ToMyPage -> #(model, effect.none())
    ToHome -> #(model, effect.none())

    NameUpdated(value) -> #(Model(..model, name: value), effect.none())
    BioUpdated(value) -> #(Model(..model, bio: value), effect.none())
    VisibilityUpdated(value) -> #(Model(..model, is_public: value == "true"), effect.none())

    SubmitClicked ->
      #(
        Model(..model, messages: []),
        update_my_profile(
          MyProfile(
            id: model.id,
            name: model.name,
            email: model.email,
            bio: model.bio,
            is_public: model.is_public,
          ),
          ProfileSaved,
        ),
      )

    ProfileLoaded(Ok(profile)) ->
      #(
        Model(
          ..model,
          id: profile.id,
          name: profile.name,
          email: profile.email,
          bio: profile.bio,
          is_public: profile.is_public,
          messages: [],
        ),
        effect.none(),
      )
    ProfileLoaded(Error(ApiError(message))) ->
      #(Model(..model, messages: [message]), effect.none())

    ProfileSaved(Ok(profile)) ->
      #(
        Model(
          ..model,
          id: profile.id,
          name: profile.name,
          email: profile.email,
          bio: profile.bio,
          is_public: profile.is_public,
          messages: ["保存しました"],
        ),
        effect.none(),
      )
    ProfileSaved(Error(ApiError(message))) ->
      #(Model(..model, messages: [message]), effect.none())
  }
}

pub fn view(model: Model) -> element.Element(Msg) {
  case model.session {
    session.Guest ->
      div(
        [attribute.attribute("style", "padding: 20px; font-family: sans-serif;")],
        [
          text("プロフィール編集"),
          text("ログインしてください"),
          btn.to_home_btn_component(ToHome),
        ],
      )

    session.Authenticated(..) ->
      div(
        [attribute.attribute("style", "padding: 20px; font-family: sans-serif;")],
        [
          text("プロフィール編集"),
          ui.text_input("表示名", "text", "名前", model.name, NameUpdated),
          ui.text_area("自己紹介", "自己紹介", model.bio, BioUpdated),
          ui.select_box(
            "公開設定",
            case model.is_public {
              True -> "true"
              False -> "false"
            },
            [#("true", "公開"), #("false", "非公開")],
            VisibilityUpdated,
          ),
          button([on_click(SubmitClicked)], [text("保存する")]),
          button([on_click(ToMyPage)], [text("マイページに戻る")]),
          div([], list.map(model.messages, fn(message) { div([], [text(message)]) })),
        ],
      )
  }
}


