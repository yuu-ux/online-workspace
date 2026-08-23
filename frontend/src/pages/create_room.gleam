import components/btn
import gleam/int
import lustre/event.{on_click, on_input}
import lustre/attribute.{class}
import lustre/element
import lustre/effect
import lustre/element/html.{div, text, p}

import types/room.{
  type CategoryType,
  type WorkStyleType,
  type VisibilityType,
  type RoomId,
  type RoomNameType,
  type DescriptionType,
  RoomNameType,
  DescriptionType,
  RoomId,
  Cat1,
  Cat2,
  Cat3,
  CasualChat,
  Quiet,
  Public,
  Invite,
  Friend,
}

import types/session.{type Session}

pub type InputType {
  RoomName
  Description
  Category
  WorkStyle
  Visibility
  MaxNumOfMember
}

pub type Model {
  Model(
    session: Session,
    current_roomname_input: String,
    current_description_input: String,
    current_category_input: String,
    current_workstyle_input: String,
    current_visibility_input: String,
    current_maxnumofmember_input: String,
    messages: List(String)
  )
}

pub type Msg {
  ToHome
  ToLogin
  ToRoom(RoomId)
  InputUpdated(target: InputType, str: String)
  SubmitClicked
}

pub type CreateRoomErr {
  DummyError
}

/// 新しいRoomIdを発行して返す
fn create_room_proc(
  roomname: RoomNameType,
  description: DescriptionType,
  category_type: Result(CategoryType, Nil),
  workstyle_type: Result(WorkStyleType, Nil),
  visibility: Result(VisibilityType, Nil),
  max_number_of_member: Result(Int, Nil)
) -> Result(RoomId, CreateRoomErr) {
  // TODO SERVER API
  Ok(RoomId(0)) // example room id
}

pub fn init(session: Session) -> #(Model, effect.Effect(Msg)) {
  #(
    Model(
      session: session,
      current_roomname_input: "",
      current_description_input: "",
      current_category_input: "",
      current_workstyle_input: "",
      current_visibility_input: "",
      current_maxnumofmember_input: "12",
      messages: []),
    effect.none()
  )
}

pub fn update(model: Model, msg: Msg) -> #(Model, effect.Effect(Msg)) {
  case msg {
    ToHome -> {
      #(model, effect.none())
    }

    ToLogin -> {
      #(model, effect.none())
    }

    ToRoom(_room_id) -> {
      #(model, effect.none())
    }

    // 文字が入力されたら、Modelの current_input をリアルタイムに書き換える
    InputUpdated(target, text) -> {
      let new_model = case target {
        RoomName -> {
          Model(..model, current_roomname_input: text)
        }

        Description -> { 
          Model(..model, current_description_input: text)
        }

        Category -> {
          Model(..model, current_category_input: text)
        }

        WorkStyle -> {
          Model(..model, current_workstyle_input: text)
        }

        Visibility -> {
          Model(..model, current_visibility_input: text)
        }

        MaxNumOfMember -> {
          Model(..model, current_maxnumofmember_input: text)
        }
      }
      #(new_model, effect.none())
    }

    // 送信ボタンが押されたら、入力内容を履歴に追加し、入力欄を空にする
    SubmitClicked -> {
      // io.println("current_email_input:" <> model.current_email_input)
      // io.println("current_password_input:" <> model.current_password_input)
      let room_name = RoomNameType(model.current_roomname_input)

      let description = DescriptionType(model.current_description_input)

      let category = case model.current_category_input {
        "category 1" -> { Ok(Cat1) }
        "category 2" -> { Ok(Cat2) }
        "category 3" -> { Ok(Cat3) }
        _ -> {
          Error(Nil)
        }
      }

      let work_style = case model.current_workstyle_input {
        "Casual Chat" -> {
          Ok(CasualChat)
        }
        "Quiet" -> {
          Ok(Quiet)
        }
        _ -> {
          Error(Nil)
        }
      }

      let visibility = case model.current_visibility_input {
        "Public" -> { Ok(Public) }
        "Invitation Only" -> { Ok(Invite) }
        "Friend Only" -> { Ok(Friend) }
        _ -> { Error(Nil) }
      }
      let max_number_of_member = int.parse(model.current_maxnumofmember_input)

      case create_room_proc(
        room_name,
        description,
        category,
        work_style,
        visibility,
        max_number_of_member
      ) {
        // TODO
        Ok(room_id) -> {
          // 成功したらToRoomを呼ぶ
          #(Model(..model, session: model.session), effect.from(fn (dispatch) { dispatch(ToRoom(room_id)) }))
        }
        Error(err_type) -> {
          let msg = case err_type {
            DummyError -> {
              ["DummyError"]
            }
          }
          #(Model(..model, messages: msg), effect.none())
        }
      }
    }
  }
}

import components/authui as ui

// ---------------------------------------------------------
// Create Room画面のView
// ---------------------------------------------------------
pub fn view(model: Model) -> element.Element(Msg) {
  case model.session {
    
    // -------------------------------------
    // 未ログイン（ゲスト）: 作成できない旨を表示
    // -------------------------------------
    session.Guest -> {
      ui.centered_text_card_layout(
        [
          ui.header_section("エラー", "ルームの作成にはログインが必要です"),
          p([class("text-gray-600")], [text("アカウントにログインしてから再度お試しください。")]),
          btn.primary_button("ログイン画面へ", ToLogin) // ※もしToLoginイベントがあれば
        ],
        btn.to_home_btn_component(ToHome)
      )
    }

    // -------------------------------------
    // ログイン済みの場合: ルーム作成フォームを表示
    // -------------------------------------
    session.Authenticated(jwt, user_id) -> {
      ui.centered_card_layout(
        [
          ui.header_section("ルーム作成", "新しいワークスペースを立ち上げましょう"),
          ui.error_messages(model.messages),
          form_section(model)
        ],
        btn.to_home_btn_component(ToHome)
      )
    }
  }
}

// ---------------------------------------------------------
// フォームセクション
// ---------------------------------------------------------
fn form_section(model: Model) -> element.Element(Msg) {
  div([class("space-y-5")], [
    
    // 1. ルーム名
    ui.text_input(
      "ルーム名",
      "text",
      "例: 開発チーム雑談部屋",
      model.current_roomname_input,
      fn(val) { InputUpdated(RoomName, val) }
    ),

    // 2. 説明文（テキストエリア）
    ui.text_area(
      "ルームの説明",
      "このルームの目的やルールを書いてください...",
      model.current_description_input,
      fn(val) { InputUpdated(Description, val) }
    ),

    // 3. カテゴリ（セレクトボックス）
    ui.select_box(
      "カテゴリ",
      model.current_category_input,
      [
        #("category 1", "カテゴリ 1"),
        #("category 2", "カテゴリ 2"),
        #("category 3", "カテゴリ 3")
      ],
      fn(val) { InputUpdated(Category, val) }
    ),

    // 4. ワークスタイル（セレクトボックス）
    ui.select_box(
      "作業スタイル",
      model.current_workstyle_input,
      [
        #("Casual Chat", "🗣️ 雑談OK (Casual Chat)"),
        #("Quiet", "🤫 もくもく (Quiet)")
      ],
      fn(val) { InputUpdated(WorkStyle, val) }
    ),

    // 5. 公開範囲（セレクトボックス）
    ui.select_box(
      "公開設定",
      model.current_visibility_input, // 元コードでは category_input になっていましたが修正と推測
      [
        #("Public", "🌎 全体に公開 (Public)"),
        #("Invitation Only", "✉️ 招待制 (Invitation Only)"),
        #("Friend Only", "🤝 フレンドのみ (Friend Only)")
      ],
      fn(val) { InputUpdated(Visibility, val) }
    ),

    // 6. 最大人数（数値入力）
    ui.number_input(
      "最大参加人数",
      model.current_maxnumofmember_input,
      fn(val) { InputUpdated(MaxNumOfMember, val) }
    ),

    // 作成ボタン
    div([class("pt-4")], [
      btn.primary_button("ルームを作成する", SubmitClicked)
    ])
  ])
}
