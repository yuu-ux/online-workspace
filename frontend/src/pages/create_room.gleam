import components/btn
import gleam/int
import lustre/event.{on_click, on_input}
import lustre/attribute
import lustre/element
import lustre/effect
import gleam/io
import gleam/list
import lustre/element/html.{button, div, text, input, textarea, select, option}
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

pub fn view (model: Model) -> element.Element(Msg) {
  case model.session {
    session.Guest -> {
      div([
        attribute.attribute("style", "padding: 20px; font-family: sans-serif;")
      ],
      [
        text("Create Room"),
        text("ログインしてください"),
        btn.to_home_btn_component(ToHome)
      ])
    }

    session.Authenticated(jwt, user_id) -> {
      div([
        attribute.attribute("style", "padding: 20px; font-family: sans-serif;")
      ],
      [
        text("Create Room"),
        div([], [
          input([
            on_input(InputUpdated(RoomName, _)),
            attribute.value(model.current_roomname_input)
          ]),

          textarea(
            [
              on_input(InputUpdated(Description, _)),
              attribute.value(model.current_description_input)
            ],
            model.current_description_input
          ),

          select(
            [
              on_input(InputUpdated(Category, _)),
              attribute.value(model.current_category_input)
            ], 
            [
              option([attribute.value("category 1")], "category 1"),
              option([attribute.value("category 2")], "category 2"),
              option([attribute.value("category 3")], "category 3"),
            ]
          ),

          select(
            [
              on_input(InputUpdated(WorkStyle, _)),
              attribute.value(model.current_workstyle_input)
            ], 
            [
              option([attribute.value("Casual Chat")], "Casual Chat"),
              option([attribute.value("Quiet")], "Quiet"),
            ]
          ),

          select(
            [
              on_input(InputUpdated(Visibility, _)),
              attribute.value(model.current_category_input)
            ], 
            [
              option([attribute.value("Public")], "Public"),
              option([attribute.value("Invitation Only")], "Invitation Only"),
              option([attribute.value("Friend Only")], "Friend Only"),
            ]
          ),

          input([
            attribute.type_("number"),
            on_input(InputUpdated(MaxNumOfMember, _)),
            attribute.value(model.current_maxnumofmember_input)
          ]),

          // ボタンが押されたら SubmitClicked イベントを発射
          button([
            on_click(SubmitClicked)
          ], [text("create room")])
        ]),
        btn.to_home_btn_component(ToHome),
        div([], list.map(model.messages, fn (x) {div([], [text(x)])}))
      ])

    }
  }
}

