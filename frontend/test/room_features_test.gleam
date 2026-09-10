import gleeunit
import gleeunit/should
import gleam/json
import gleam/option.{Some}
import gleam/string
import lustre/element
import components/rooms
import pages/room
import types/room as room_t
import types/session.{Authenticated, Token}
import types/user.{UserInfo, UserId}
import wrap/api.{ApiError}
import wrap/room as room_api

pub fn main() {
  gleeunit.main()
}

pub fn room_join_error_is_rendered_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let #(model, _) = room.init(session, room_t.RoomId(1))
  let #(updated_model, _) = room.update(
    model,
    room.JoinedRoom(Error(ApiError("このルームには入室できません。"))),
  )

  room.view(updated_model)
  |> element.to_string
  |> string.contains("このルームには入室できません。")
  |> should.equal(True)
}

pub fn room_leave_error_is_rendered_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let #(model, _) = room.init(session, room_t.RoomId(1))
  let #(updated_model, _) = room.update(
    model,
    room.RoomLeft(Error(ApiError("退室に失敗しました。"))),
  )

  room.view(updated_model)
  |> element.to_string
  |> string.contains("退室に失敗しました。")
  |> should.equal(True)
}

pub fn room_list_decodes_joinability_and_metadata_test() {
  let decoded = json.parse(
    "{\"id\":1,\"name\":\"集中ルーム\",\"category\":{\"id\":1},\"workStyle\":\"FOCUS\",\"maxMembers\":4,\"currentMembers\":2,\"status\":\"OPEN\",\"createdBy\":{\"id\":10,\"name\":\"Alice\",\"iconUrl\":null},\"joinable\":true,\"joinRestriction\":null,\"createdAt\":\"2026-09-10T00:00:00Z\"}",
    room_api.room_info_decoder(),
  )

  case decoded {
    Ok(info) -> {
      info.current_members |> should.equal(2)
      info.max_number_of_member |> should.equal(4)
      info.joinable |> should.equal(True)
      info.created_at |> should.equal("2026-09-10T00:00:00Z")
    }
    Error(_) -> should.fail()
  }
}

pub fn full_room_is_rendered_as_unavailable_test() {
  let info = room_t.RoomInfo(
    roomname: room_t.RoomNameType("満員ルーム"),
    visibility: room_t.Public,
    category: room_t.Cat1,
    work_style: room_t.Quiet,
    max_number_of_member: 2,
    room_id: room_t.RoomId(1),
    current_members: 2,
    status: "OPEN",
    joinable: False,
    join_restriction: Some("FULL"),
    created_at: "2026-09-10T00:00:00Z",
  )

  rooms.room_list_view([info], fn(_) { Nil })
  |> element.to_string
  |> string.contains("満員のため入室できません")
  |> should.equal(True)
}

pub fn room_detail_is_rendered_after_loading_test() {
  let decoded = json.parse(
    "{\"id\":1,\"name\":\"集中ルーム\",\"description\":\"静かに作業します\",\"category\":{\"id\":1},\"workStyle\":\"FOCUS\",\"maxMembers\":4,\"currentMembers\":2,\"status\":\"OPEN\",\"createdBy\":{\"id\":10,\"name\":\"Alice\",\"iconUrl\":null},\"joinable\":true,\"joinRestriction\":null,\"member\":true,\"createdAt\":\"2026-09-10T00:00:00Z\",\"updatedAt\":\"2026-09-10T00:00:00Z\"}",
    room_api.room_detail_decoder(),
  )
  let assert Ok(detail) = decoded
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let #(model, _) = room.init(session, room_t.RoomId(1))
  let #(updated_model, _) = room.update(model, room.RoomLoaded(Ok(detail)))

  room.view(updated_model)
  |> element.to_string
  |> string.contains("静かに作業します")
  |> should.equal(True)
}
