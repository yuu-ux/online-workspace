import components/btn
import gleam/int
import gleam/list
import lustre/event.{on_click}
import lustre/attribute
import lustre/element
import lustre/effect
import gleam/io
import lustre/element/html.{button, div, h1, h3, hr, span, style, text}

import types/user.{type UserId}
import types/session.{type Session,type Token, Guest, Authenticated}
import types/room.{
  type CategoryType,
  type WorkStyleType,
  type VisibilityType,
  type RoomId,
  type RoomNameType,
  type DescriptionType,
  type RoomInfo,
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
  RoomInfo
}

import components/rooms.{room_list_view}

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
    Authenticated (jwt, user_info) -> {
      #(
        Model(session: session, rooms: get_rooms(jwt, user_info.user_id)),
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
        btn.login_btn_component(ToLogin),
      ])
    }

    session.Authenticated(jwt, user_id) -> {
      div([
        attribute.attribute("style", "padding: 20px; font-family: sans-serif;")
      ],
      [
        text("Home (logined)"),
        room_list_view(model.rooms, ToRoom),

        btn.create_room_btn_component(ToCreateRoom),
        btn.logout_btn_component(ToLogout),
        btn.mypage_btn_component(ToMyPage),
      ])
    }
  }
}

