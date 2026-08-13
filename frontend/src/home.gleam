import gleam/int
import gleam/list
import lustre/event.{on_click}
import lustre/attribute
import lustre/element
import lustre/effect
import gleam/io
import lustre/element/html.{button, div, h1, h3, hr, span, style, text}

import types/session.{type Session,type Token, type UserId, Guest, Authenticated}
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

pub type Model {
  Model(
    session: Session,
    rooms: List(RoomInfo)
  )
}

pub type Msg {
  ToCreateRoom
  ToLogin
  ToLogout
  ToMyPage
  ToRoom(RoomId)
}

pub type RoomInfo {
  RoomInfo(
    roomname: RoomNameType,
    visibility: VisibilityType,
    category: CategoryType,
    work_style: WorkStyleType,
    max_number_of_member: Int,
    room_id: RoomId
  )
}

pub fn get_rooms(jwt: Token, user_id: UserId) -> List(RoomInfo) {
  [
    RoomInfo(
      roomname: RoomNameType("Room1"),
      visibility: Public,
      category: Cat1,
      work_style: Quiet,
      max_number_of_member: 10,
      room_id: RoomId(1)
    ),

    RoomInfo(
      roomname: RoomNameType("Room2"),
      visibility: Friend,
      category: Cat2,
      work_style: CasualChat,
      max_number_of_member: 12,
      room_id: RoomId(2)
    ),
  ]
}

pub fn init(session: Session) -> #(Model, effect.Effect(Msg)) {
  case session {
    Guest -> {
      #(
        Model(session: session, rooms: []),
        effect.none()
      )
    }
    Authenticated (jwt, user_id) -> {
      #(
        Model(session: session, rooms: get_rooms(jwt, user_id)),
        effect.none()
      )
    }
  }
}

pub fn update(model: Model, msg: Msg) -> #(Model, effect.Effect(Msg)) {
  // case msg {
  //   ToCreateRoom -> {}
  //   ToLogin -> {}
  //   ToLogout -> {}
  //   ToMyPage -> {}
  //   ToRoom(room_id) -> {}
  // }
  #(model, effect.none())
}

pub fn view (model: Model) -> element.Element(Msg) {
  case model.session {

    session.Guest -> {
      div([
        attribute.attribute("style", "padding: 20px; font-family: sans-serif;")
      ],
      [
        text("Home (guest)"),
        button([on_click(ToLogin)], [text("login")]),
        // button([on_click(ToMyPage)], [text("mypage")])
      ])
    }

    session.Authenticated(jwt, user_id) -> {
      div([
        attribute.attribute("style", "padding: 20px; font-family: sans-serif;")
      ],
      [
        text("Home (logined)"),
        div(
          [], 
          list.map(model.rooms, fn (room_info) {
              div(
                [],
                [
                  text("room name: " <> case room_info.roomname {RoomNameType(a) -> { a }}),
                  text("category: " <> case room_info.category {
                    Cat1 -> {
                      "cat1"
                    }
                    Cat2 -> {
                      "cat2"
                    }
                    Cat3 -> {
                      "cat3"
                    }
                  }), 
                  text("work_style: " <> case room_info.work_style {
                    CasualChat -> {
                      "CasualChat"
                    }
                    Quiet -> {
                      "Quiet"
                    }
                  }), 
                  text("max number of member: " <> int.to_string(room_info.max_number_of_member)),
                  button([on_click(ToRoom(room_info.room_id))], [text("入室")])
              ])
            })
        ),
        button([on_click(ToCreateRoom)], [text("create room")]),
        button([on_click(ToLogout)], [text("logout")]),
        button([on_click(ToMyPage)], [text("mypage")])
      ])
    }
  }
}

