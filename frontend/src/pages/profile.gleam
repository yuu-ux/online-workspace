// プロフィール編集画面
import gleam/list
import lustre/event.{on_click, on_input}
import lustre/attribute
import lustre/element
import lustre/effect
import lustre/element/html.{button, div, text}

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
      div([
        attribute.attribute("style", "padding: 20px; font-family: sans-serif;")
      ],
      [
        text("プロフィール編集"),
        text("ログインしてください"),
        btn.to_home_btn_component(ToHome),
      ])
    }

    session.Authenticated(_, _) -> {
      div([
        attribute.attribute("style", "padding: 20px; font-family: sans-serif;")
      ],
      [
        text("プロフィール編集"),
        div([], [text("名前")]),
        input.normal_input(InputName, model.name),
        div([], [text("アイコン情報")]),
        input.normal_input(InputIconUrl, model.icon_url),
        div([], [text("自己紹介")]),
        input.normal_input(InputBio, model.bio),
        div([], [
          text("公開設定: "),
          button([on_click(TogglePublic(toggle(model.is_public)))], [
            text(case model.is_public { True -> "公開" False -> "非公開" }),
          ]),
        ]),
        button(
          [on_click(SaveClicked)],
          [text("保存")],
        ),
        button([
          on_click(ToMyPage)
        ], [text("マイページに戻る")]),
        div([], list.map(model.messages, fn (x) {div([], [text(x)])}))
      ])

    }
  }
}

fn toggle(value: Bool) -> Bool {
  case value {
    True -> False
    False -> True
  }
}


