// プロフィール編集画面
import gleam/list
import lustre/event.{on_click}
import lustre/attribute
import lustre/element
import lustre/effect
import lustre/element/html.{button, div, h1, input, label, p, text}

import types/session.{type Session}
import wrap/api.{type ApiError, ApiError}
import wrap/user as user_wrap
import components/input as input_components
import components/btn
import components/ui

pub type Model {
  Model(
    session: Session,
    name: String,
    icon_url: String,
    bio: String,
    work_category_id: Int,
    is_public: Bool,
    loading: Bool,
    avatar_upload_pending: Bool,
    messages: List(Message)
  )
}

pub type Message {
  ErrorMessage(String)
  SuccessMessage(String)
}

pub type Msg {
  ToHome
  ToMyPage
  InputName(String)
  InputBio(String)
  TogglePublic(Bool)
  SaveClicked
  ProfileLoaded(Result(user_wrap.MyProfile, user_wrap.MyProfileErr))
  ProfileSaved(Result(user_wrap.MyProfile, ApiError))
  AvatarUploaded(Result(String, ApiError))
}

pub fn init(session: Session) -> #(Model, effect.Effect(Msg)) {
  #(
    Model(
      session: session, name: "", icon_url: "", bio: "", work_category_id: 0, is_public: True,
      loading: True, avatar_upload_pending: False, messages: []),
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
    InputBio(value) -> #(Model(..model, bio: value), effect.none())
    TogglePublic(value) -> #(Model(..model, is_public: value), effect.none())

    SaveClicked ->
      #(
        Model(..model, loading: True, avatar_upload_pending: user_wrap.has_selected_avatar(), messages: []),
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
          avatar_upload_pending: False,
          messages: [],
        ),
        effect.none(),
      )

    ProfileLoaded(Error(user_wrap.MyProfileApiErr(ApiError(message)))) ->
      #(Model(..model, loading: False, messages: [ErrorMessage(message)]), effect.none())

    ProfileSaved(Ok(profile)) ->
      case model.avatar_upload_pending {
        True ->
          #(
            Model(
              ..model,
              name: profile.name,
              icon_url: profile.icon_url,
              bio: profile.bio,
              work_category_id: profile.work_category_id,
              is_public: profile.is_public,
              loading: True,
              avatar_upload_pending: False,
              messages: [],
            ),
            user_wrap.upload_my_avatar(AvatarUploaded),
          )
        False ->
          #(
            Model(
              ..model,
              name: profile.name,
              icon_url: profile.icon_url,
              bio: profile.bio,
              work_category_id: profile.work_category_id,
              is_public: profile.is_public,
              loading: False,
              messages: [SuccessMessage("プロフィールを保存しました")],
            ),
            effect.none(),
          )
      }

    ProfileSaved(Error(ApiError(message))) ->
      #(Model(..model, loading: False, avatar_upload_pending: False, messages: [ErrorMessage(message)]), effect.none())

    AvatarUploaded(Ok(icon_url)) ->
      #(Model(..model, icon_url: icon_url, loading: False, messages: [SuccessMessage("プロフィールを保存しました")]), effect.none())

    AvatarUploaded(Error(ApiError(message))) ->
      #(Model(..model, loading: False, messages: [ErrorMessage(message)]), effect.none())
  }
}

pub fn view (model: Model) -> element.Element(Msg) {
  case model.session {
    session.Guest -> {
      div([attribute.class("flex min-h-screen items-center justify-center bg-[#f7f5f0] p-5")], [
        div([attribute.class("p-8 text-center")], [
          h1([attribute.class("mb-3 text-xl font-bold text-gray-900")], [text("プロフィール編集")]),
          p([attribute.class("mb-6 text-gray-600")], [text("ログインしてください")]),
          btn.to_home_btn_component(ToHome),
        ]),
      ])
    }

    session.Authenticated(_, _) -> {
      div([attribute.class("min-h-screen bg-[#f7f5f0] p-4 font-sans sm:p-6")], [
        div([attribute.class("mb-6")], [button([attribute.class("text-sm font-medium text-[#58745a] hover:underline"), on_click(ToMyPage)], [text("← マイページに戻る")])]),
        div([attribute.class("mx-auto max-w-2xl")], [
          div([attribute.class("mb-5")], [
            h1([attribute.class("text-2xl font-semibold text-gray-900")], [text("プロフィール編集")]),
            p([attribute.class("mt-1 text-sm text-gray-500")], [
              text("プロフィール情報と公開設定を変更できます"),
            ]),
          ]),
          div([attribute.class("border-t border-[#dedbd2] py-6")], [
            div([attribute.class("space-y-5")], [
              field("名前", input_components.normal_input(InputName, model.name)),
              field("アイコン", input([
                attribute.attribute("id", "avatar-file"),
                attribute.attribute("type", "file"),
                attribute.attribute("accept", "image/png,image/jpeg"),
                attribute.class("w-full " <> ui.input_classes() <> " file:mr-3 file:rounded-md file:border-0 file:bg-[#edf3eb] file:px-3 file:py-1.5 file:text-sm file:font-medium file:text-[#36553b]"),
              ])),
              p([attribute.class("-mt-3 text-xs text-gray-500")], [text("PNGまたはJPEG、2MB以下。保存時にアップロードします。")]),
              field("自己紹介", input_components.normal_input(InputBio, model.bio)),
              div([attribute.class("flex items-center justify-between border-y border-[#dedbd2] py-4")], [
                div([], [
                  div([attribute.class("font-semibold text-gray-900")], [text("公開設定")]),
                  p([attribute.class("mt-1 text-xs text-gray-500")], [
                    text("プロフィールを他のユーザーに公開します"),
                  ]),
                ]),
                button([
                  attribute.class(case model.is_public {
                    True -> "rounded-md bg-[#edf3eb] px-4 py-2 text-sm font-semibold text-[#36553b] transition hover:bg-[#dce8da]"
                    False -> "rounded-md bg-gray-100 px-4 py-2 text-sm font-semibold text-gray-600 transition hover:bg-gray-200"
                  }),
                  on_click(TogglePublic(toggle(model.is_public))),
                ], [
                  text(case model.is_public { True -> "公開" False -> "非公開" }),
                ]),
              ]),
            ]),
            div([attribute.class("mt-6")], list.map(model.messages, fn(message) {
              case message {
                ErrorMessage(error) -> ui.error_messages([error])
                SuccessMessage(success) -> ui.success_messages([success])
              }
            })),
            div([attribute.class("mt-6 flex flex-col gap-3 sm:flex-row-reverse")], [
              button([
                attribute.class("rounded-md bg-[#58745a] px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-[#46614a] disabled:cursor-not-allowed disabled:opacity-60"),
                on_click(SaveClicked),
              ], [text(case model.loading { True -> "保存中..." False -> "プロフィールを保存" })]),
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
