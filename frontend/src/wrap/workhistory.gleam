import gleam/dynamic/decode
import lustre/effect
import wrap/api

pub type WorkHistory {
  WorkHistory(
    room_id: Int,
    room_name: String,
    joined_at: String,
    left_at: String,
    duration_minutes: Int,
  )
}

pub fn list_work_histories(
  to_msg: fn(Result(List(WorkHistory), api.ApiError)) -> msg,
) -> effect.Effect(msg) {
  let decoder = {
    use items <- decode.field("items", decode.list(work_history_decoder()))
    decode.success(items)
  }
  api.json_request(
    "GET",
    "/api/v1/workhistories?page=0&size=50",
    "",
    decoder,
    to_msg,
  )
}

fn work_history_decoder() -> decode.Decoder(WorkHistory) {
  use room_id <- decode.field("roomId", decode.int)
  use room_name <- decode.field("roomName", decode.string)
  use joined_at <- decode.field("joinedAt", decode.string)
  use left_at <- decode.optional_field("leftAt", "", decode.string)
  use duration_minutes <- decode.field("durationMinutes", decode.int)
  decode.success(
    WorkHistory(
      room_id: room_id,
      room_name: room_name,
      joined_at: joined_at,
      left_at: left_at,
      duration_minutes: duration_minutes,
    ),
  )
}
