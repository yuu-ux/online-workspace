import gleeunit
import gleeunit/should
import frontend
import gleam/json
import gleam/option.{None, Some}
import lustre/element
import gleam/string
import pages/friend
import pages/home
import pages/login
import pages/mypage
import pages/profile
import pages/room
import pages/create_room
import pages/search
import components/ui
import components/userinfo
import wrap/api.{ApiError}
import types/session.{Authenticated, Token}
import types/room as room_t
import types/user.{FriendInfo, UserInfo, UserId}
import wrap/user as user_wrap
import wrap/room as room_wrap

pub fn main() {
  gleeunit.main()
}

pub fn room_presence_updates_member_list_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let #(model, _) = room.init(session, room_t.RoomId(10))
  let #(with_member, _) = room.update(
    model,
    room.MembersLoaded(Ok([room_wrap.RoomMember("Alice", UserId("2"), "")])),
  )

  let #(joined_model, _) = room.update(
    with_member,
    room.WsMessageReceived(
      "{\"type\":\"presence\",\"room_id\":10,\"user_id\":3,\"user\":\"Bob\",\"online\":true}",
    ),
  )
  joined_model.member_list
  |> should.equal([room_wrap.RoomMember("Bob", UserId("3"), ""), room_wrap.RoomMember("Alice", UserId("2"), "")])

  let #(left_model, _) = room.update(
    joined_model,
    room.WsMessageReceived(
      "{\"type\":\"presence\",\"room_id\":10,\"user_id\":3,\"user\":\"Bob\",\"online\":false}",
    ),
  )
  left_model.member_list |> should.equal([room_wrap.RoomMember("Alice", UserId("2"), "")])
}

pub fn room_member_count_event_updates_member_count_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let #(model, _) = room.init(session, room_t.RoomId(10))
  let detail = room_t.RoomDetail(
    room_id: room_t.RoomId(10),
    roomname: room_t.RoomNameType("Room"),
    description: room_t.DescriptionType("Description"),
    category: room_t.Cat1,
    work_style: room_t.Quiet,
    max_number_of_member: 3,
    current_members: 1,
    status: "OPEN",
    created_by: UserInfo("Alice", UserId("2")),
    joinable: True,
    join_restriction: None,
    member: True,
    created_at: "2026-09-10T00:00:00Z",
    updated_at: "2026-09-10T00:00:00Z",
  )
  let model = room.Model(
    ..model,
    room_detail: Some(detail),
    member_list: [room_wrap.RoomMember("Alice", UserId("2"), "")],
  )

  let #(joined_model, _) = room.update(
    model,
    room.WsMessageReceived(
      "{\"type\":\"room:member_count_changed\",\"room_id\":10,\"current_members\":2}",
    ),
  )
  case joined_model.room_detail {
    Some(updated_detail) -> updated_detail.current_members |> should.equal(2)
    None -> should.fail()
  }

  let #(left_model, _) = room.update(
    joined_model,
    room.WsMessageReceived(
      "{\"type\":\"room:member_count_changed\",\"room_id\":10,\"current_members\":1}",
    ),
  )
  case left_model.room_detail {
    Some(updated_detail) -> updated_detail.current_members |> should.equal(1)
    None -> should.fail()
  }
}

pub fn room_duplicate_member_count_events_are_idempotent_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let #(model, _) = room.init(session, room_t.RoomId(10))
  let detail = room_t.RoomDetail(
    room_id: room_t.RoomId(10),
    roomname: room_t.RoomNameType("Room"),
    description: room_t.DescriptionType("Description"),
    category: room_t.Cat1,
    work_style: room_t.Quiet,
    max_number_of_member: 3,
    current_members: 1,
    status: "OPEN",
    created_by: UserInfo("Alice", UserId("2")),
    joinable: True,
    join_restriction: None,
    member: True,
    created_at: "2026-09-10T00:00:00Z",
    updated_at: "2026-09-10T00:00:00Z",
  )
  let model = room.Model(..model, room_detail: Some(detail))
  let event =
    room.WsMessageReceived(
      "{\"type\":\"room:member_count_changed\",\"room_id\":10,\"current_members\":2}",
    )

  let #(once_model, _) = room.update(model, event)
  let #(twice_model, _) = room.update(once_model, event)

  case twice_model.room_detail {
    Some(updated_detail) -> updated_detail.current_members |> should.equal(2)
    None -> should.fail()
  }
}

pub fn friend_loaded_message_updates_friend_page_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let #(friend_model, _) = friend.init(session)
  let model = frontend.Model(
    current_page: frontend.Friend(friend_model),
    session: session,
    notification: None,
  )

  let #(updated_model, _) = frontend.update(
    model,
    frontend.FriendMsg(friend.FriendsLoaded(Ok([
      UserInfo("test", UserId("1")),
    ]))),
  )

  case updated_model.current_page {
    frontend.Friend(updated_friend_model) ->
      updated_friend_model.friends |> should.equal([
        FriendInfo(UserInfo("test", UserId("1")), False),
      ])
    _ -> should.fail()
  }
}

pub fn home_member_count_event_updates_room_member_count_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let room_id = room_t.RoomId(10)
  let model = home.Model(
    session: session,
    rooms: [
      room_t.RoomInfo(
        roomname: room_t.RoomNameType("Room"),
        description: room_t.DescriptionType("Description"),
        visibility: room_t.Public,
        category: room_t.Cat1,
        work_style: room_t.Quiet,
        max_number_of_member: 3,
        room_id: room_id,
        current_members: 1,
        status: "OPEN",
        joinable: True,
        join_restriction: None,
        created_at: "2026-09-10T00:00:00Z",
      ),
    ],
    messages: [],
  )

  let #(updated_model, _) = home.update(
    model,
    home.WsMessageReceived(
      "{\"type\":\"room:member_count_changed\",\"room_id\":10,\"current_members\":2}",
    ),
  )
  case updated_model.rooms {
    [room, ..] -> room.current_members |> should.equal(2)
    [] -> should.fail()
  }
}

pub fn home_duplicate_member_count_events_are_idempotent_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let room_id = room_t.RoomId(10)
  let model = home.Model(
    session: session,
    rooms: [
      room_t.RoomInfo(
        roomname: room_t.RoomNameType("Room"),
        description: room_t.DescriptionType("Description"),
        visibility: room_t.Public,
        category: room_t.Cat1,
        work_style: room_t.Quiet,
        max_number_of_member: 3,
        room_id: room_id,
        current_members: 1,
        status: "OPEN",
        joinable: True,
        join_restriction: None,
        created_at: "2026-09-10T00:00:00Z",
      ),
    ],
    messages: [],
  )
  let event =
    home.WsMessageReceived(
      "{\"type\":\"room:member_count_changed\",\"room_id\":10,\"current_members\":2}",
    )

  let #(once_model, _) = home.update(model, event)
  let #(twice_model, _) = home.update(once_model, event)

  case twice_model.rooms {
    [room, ..] -> room.current_members |> should.equal(2)
    [] -> should.fail()
  }
}

pub fn home_room_created_message_adds_room_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let model = home.Model(session: session, rooms: [], messages: [])
  let #(updated_model, _) = home.update(
    model,
    home.WsMessageReceived(
      "{\"type\":\"room:created\",\"payload\":{\"id\":42,\"name\":\"New room\",\"description\":\"New room description\",\"category\":{\"id\":1},\"workStyle\":\"FOCUS\",\"maxMembers\":3,\"currentMembers\":1,\"status\":\"OPEN\",\"createdBy\":{\"id\":1,\"name\":\"me\",\"iconUrl\":null},\"joinable\":true,\"joinRestriction\":null,\"member\":true,\"createdAt\":\"2026-09-10T00:00:00Z\",\"updatedAt\":\"2026-09-10T00:00:00Z\"}}",
    ),
  )

  case updated_model.rooms {
    [room, ..] -> {
      room.room_id |> should.equal(room_t.RoomId(42))
      room.current_members |> should.equal(1)
    }
    [] -> should.fail()
  }
}

pub fn friend_error_message_is_rendered_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let target = UserInfo("test2", UserId("2"))
  let #(model, _) = userinfo.init(session, target)
  let #(updated_model, _) = userinfo.update(
    model,
    userinfo.FriendUpdated(False, Error(ApiError("既にフレンドです。"))),
  )

  userinfo.view(updated_model)
  |> element.to_string
  |> string.contains("既にフレンドです。")
  |> should.equal(True)
}

pub fn own_profile_does_not_render_friend_control_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let #(model, _) = userinfo.init(session, UserInfo("me", UserId("1")))

  userinfo.view(model)
  |> element.to_string
  |> string.contains("type=\"checkbox\"")
  |> should.equal(False)
}

pub fn own_profile_uses_my_profile_response_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let #(model, _) = userinfo.init(session, UserInfo("me", UserId("1")))
  let profile = user_wrap.MyProfile(
    name: "me",
    icon_url: "",
    is_public: True,
    bio: "my bio",
    work_category_id: 0,
    work_category: "未設定",
    email: "me@example.com",
  )
  let #(updated_model, _) = userinfo.update(
    model,
    userinfo.MyProfileLoaded(Ok(profile)),
  )

  userinfo.view(updated_model)
  |> element.to_string
  |> string.contains("my bio")
  |> should.equal(True)
}

pub fn loaded_friendship_status_checks_friend_control_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let target = UserInfo("friend", UserId("2"))
  let #(model, _) = userinfo.init(session, target)
  let profile = user_wrap.UserProfile(
    name: "friend",
    icon_url: "",
    is_public: True,
    bio: "",
    work_category: "",
    friendship: "FRIEND",
  )

  let #(updated_model, _) = userinfo.update(
    model,
    userinfo.ProfileLoaded(Ok(profile)),
  )

  updated_model.is_friend |> should.equal(True)
}

pub fn checking_friend_updates_friendship_label_immediately_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let target = UserInfo("friend", UserId("2"))
  let #(model, _) = userinfo.init(session, target)
  let profile = user_wrap.UserProfile(
    name: "friend",
    icon_url: "",
    is_public: True,
    bio: "",
    work_category: "",
    friendship: "NONE",
  )
  let #(loaded_model, _) = userinfo.update(
    model,
    userinfo.ProfileLoaded(Ok(profile)),
  )

  let #(updated_model, _) = userinfo.update(
    loaded_model,
    userinfo.FriendOnChecked(True),
  )

  userinfo.view(updated_model)
  |> element.to_string
  |> string.contains("フレンド状態")
  |> should.equal(True)

  let #(unchecked_model, _) = userinfo.update(
    updated_model,
    userinfo.FriendOnChecked(False),
  )

  userinfo.view(unchecked_model)
  |> element.to_string
  |> string.contains("フレンド状態")
  |> should.equal(True)
}

pub fn friend_page_displays_list_limit_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let model = friend.Model(
    session: session,
    friends: [], icons: [],
    search_word: "",
    messages: [],
  )

  friend.view(model)
  |> element.to_string
  |> string.contains("最大50人")
  |> should.equal(True)
}

pub fn user_profile_decoder_accepts_nullable_fields_test() {
  json.parse(
    "{\"name\":\"Alice\",\"iconUrl\":null,\"isPublic\":true,\"bio\":\"alice bio\",\"workCategory\":null,\"friendship\":\"NONE\"}",
    user_wrap.user_profile_decoder(),
  )
  |> should.be_ok()
}

pub fn mypage_loaded_profile_renders_profile_fields_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let #(model, _) = mypage.init(session)
  let profile = user_wrap.MyProfile(
    name: "Alice",
    icon_url: "https://example.com/icon.png",
    is_public: True,
    bio: "alice bio",
    work_category_id: 1,
    work_category: "集中",
    email: "alice@example.com",
  )
  let #(updated_model, _) = mypage.update(
    model,
    mypage.MyProfileLoaded(Ok(profile)),
  )
  let rendered = mypage.view(updated_model) |> element.to_string

  rendered |> string.contains("アカウント情報") |> should.equal(False)
  rendered |> string.contains("名前") |> should.equal(False)
  rendered |> string.contains("名前: Alice") |> should.equal(False)
  rendered |> string.contains("Alice") |> should.equal(True)
  rendered |> string.contains("src=\"https://example.com/icon.png\"") |> should.equal(True)
  rendered |> string.contains("alice bio") |> should.equal(True)
  rendered |> string.contains("作業カテゴリ") |> should.equal(False)
  rendered |> string.contains("集中") |> should.equal(False)
}

pub fn mypage_profile_header_and_actions_have_visual_hierarchy_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let #(model, _) = mypage.init(session)
  let profile = user_wrap.MyProfile(
    name: "Alice",
    icon_url: "https://example.com/icon.png",
    is_public: True,
    bio: "alice bio",
    work_category_id: 0,
    work_category: "",
    email: "alice@example.com",
  )
  let #(updated_model, _) = mypage.update(
    model,
    mypage.MyProfileLoaded(Ok(profile)),
  )
  let rendered = mypage.view(updated_model) |> element.to_string

  rendered |> string.contains("h-24 w-24") |> should.equal(True)
  rendered |> string.contains("text-3xl") |> should.equal(True)
  rendered
  |> string.contains("mt-8 border-t border-[#dedbd2] pt-6")
  |> should.equal(True)
}

pub fn friend_page_has_only_mypage_back_link_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let model = friend.Model(session: session, friends: [], icons: [], search_word: "", messages: [])
  let rendered = friend.view(model) |> element.to_string

  rendered |> string.contains("← マイページに戻る") |> should.equal(True)
  rendered |> string.contains("← ホームへ") |> should.equal(False)
}

pub fn friend_page_renders_search_form_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let model = friend.Model(session: session, friends: [], icons: [], search_word: "", messages: [])

  friend.view(model)
  |> element.to_string
  |> string.contains("ユーザーを探す")
  |> should.equal(True)
}

pub fn friend_search_input_is_saved_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let model = friend.Model(session: session, friends: [], icons: [], search_word: "", messages: [])
  let #(updated_model, _) = friend.update(model, friend.SearchInputChanged("Alice"))

  updated_model.search_word |> should.equal("Alice")
}

pub fn friend_search_submission_opens_search_results_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let friend_model = friend.Model(
    session: session,
    friends: [], icons: [],
    search_word: "Alice",
    messages: [],
  )
  let model = frontend.Model(
    current_page: frontend.Friend(friend_model),
    session: session,
    notification: None,
  )

  let #(updated_model, _) = frontend.update(
    model,
    frontend.FriendMsg(friend.SearchSubmitted),
  )

  case updated_model.current_page {
    frontend.Search(search_model) -> search_model.search_word |> should.equal("Alice")
    _ -> should.fail()
  }
}

pub fn search_page_returns_to_friend_management_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let model = search.Model(
    session: session,
    search_word: "Alice",
    search_result: [],
    messages: [],
  )
  let rendered = search.view(model) |> element.to_string

  rendered |> string.contains("← マイページに戻る") |> should.equal(True)
  rendered |> string.contains("← フレンド管理") |> should.equal(False)
}

pub fn search_page_back_link_opens_friend_management_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let search_model = search.Model(
    session: session,
    search_word: "Alice",
    search_result: [],
    messages: [],
  )
  let model = frontend.Model(
    current_page: frontend.Search(search_model),
    session: session,
    notification: None,
  )

  let #(updated_model, _) = frontend.update(
    model,
    frontend.SearchMsg(search.ToFriend),
  )

  case updated_model.current_page {
    frontend.MyPage(page) -> page.friends.search_word |> should.equal("")
    _ -> should.fail()
  }
}

pub fn home_header_matches_page_background_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let model = home.Model(session: session, rooms: [], messages: [])
  let rendered = home.view(model) |> element.to_string

  rendered
  |> string.contains("sticky top-0 z-10 bg-[#f7f5f0]")
  |> should.equal(True)
}

pub fn create_room_home_link_is_aligned_to_the_top_left_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let #(model, _) = create_room.init(session)
  let rendered = create_room.view(model) |> element.to_string

  rendered |> string.contains("self-start") |> should.equal(True)
}

pub fn create_room_hides_room_category_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let #(model, _) = create_room.init(session)
  let rendered = create_room.view(model) |> element.to_string

  rendered |> string.contains("カテゴリ") |> should.equal(False)
}

pub fn room_detail_hides_room_category_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let #(model, _) = room.init(session, room_t.RoomId(10))
  let detail = room_t.RoomDetail(
    room_id: room_t.RoomId(10),
    roomname: room_t.RoomNameType("Room"),
    description: room_t.DescriptionType("Description"),
    category: room_t.Cat1,
    work_style: room_t.Quiet,
    max_number_of_member: 3,
    current_members: 1,
    status: "OPEN",
    created_by: UserInfo("Alice", UserId("2")),
    joinable: True,
    join_restriction: None,
    member: False,
    created_at: "2026-09-10T00:00:00Z",
    updated_at: "2026-09-10T00:00:00Z",
  )
  let rendered = room.view(room.Model(..model, room_detail: Some(detail))) |> element.to_string

  rendered |> string.contains("カテゴリ") |> should.equal(False)
}

pub fn friend_page_uses_shared_error_message_style_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let model = friend.Model(session: session, friends: [], icons: [], search_word: "", messages: ["error"])

  friend.view(model)
  |> element.to_string
  |> string.contains("bg-[#fff3f0]")
  |> should.equal(True)
}

pub fn profile_page_uses_shared_error_message_style_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let model = profile.Model(
    session: session,
    name: "Alice",
    icon_url: "",
    bio: "",
    work_category_id: 0,
    is_public: True,
    loading: False,
    avatar_upload_pending: False,
    messages: [profile.ErrorMessage("error")],
  )

  profile.view(model)
  |> element.to_string
  |> string.contains("bg-[#fff3f0]")
  |> should.equal(True)
}

pub fn profile_page_renders_avatar_file_input_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let model = profile.Model(
    session: session,
    name: "Alice",
    icon_url: "",
    bio: "",
    work_category_id: 0,
    is_public: True,
    loading: False,
    avatar_upload_pending: False,
    messages: [],
  )

  profile.view(model)
  |> element.to_string
  |> string.contains("avatar-file")
  |> should.equal(True)
}

pub fn shared_error_message_style_is_warm_and_consistent_test() {
  ui.error_messages(["error"])
  |> element.to_string
  |> string.contains("border-[#e8c9c1] bg-[#fff3f0] p-3 text-sm text-[#9a4a3c]")
  |> should.equal(True)
}

pub fn mypage_renders_gray_icon_fallback_when_icon_url_is_empty_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let #(model, _) = mypage.init(session)
  let profile = user_wrap.MyProfile(
    name: "Alice",
    icon_url: "",
    is_public: True,
    bio: "",
    work_category_id: 0,
    work_category: "",
    email: "alice@example.com",
  )
  let #(updated_model, _) = mypage.update(
    model,
    mypage.MyProfileLoaded(Ok(profile)),
  )
  let rendered = mypage.view(updated_model) |> element.to_string

  rendered |> string.contains("bg-gray-200") |> should.equal(True)
  rendered |> string.contains("未設定") |> should.equal(True)
  rendered |> string.contains("作業カテゴリ: 未設定") |> should.equal(False)
}

pub fn user_profile_renders_icon_url_as_image_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let target = UserInfo("Alice", UserId("2"))
  let #(model, _) = userinfo.init(session, target)
  let model =
    userinfo.Model(
      ..model,
      profile: Some(user_wrap.UserProfile(
        name: "Alice",
        icon_url: "https://example.com/icon.png",
        is_public: True,
        bio: "alice bio",
        work_category: "未分類",
        friendship: "FRIEND",
      )),
      loading: False,
    )

  userinfo.view(model)
  |> element.to_string
  |> string.contains("src=\"https://example.com/icon.png\"")
  |> should.equal(True)

  userinfo.view(model)
  |> element.to_string
  |> string.contains("alt=\"アイコン\"")
  |> should.equal(True)

  userinfo.view(model)
  |> element.to_string
  |> string.contains("作業カテゴリ")
  |> should.equal(False)
}

pub fn user_profile_renders_fallback_when_icon_url_is_empty_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let target = UserInfo("Alice", UserId("2"))
  let #(model, _) = userinfo.init(session, target)
  let model =
    userinfo.Model(
      ..model,
      profile: Some(user_wrap.UserProfile(
        name: "Alice",
        icon_url: "",
        is_public: True,
        bio: "alice bio",
        work_category: "未分類",
        friendship: "FRIEND",
      )),
      loading: False,
    )

  userinfo.view(model)
  |> element.to_string
  |> string.contains("bg-gray-200")
  |> should.equal(True)
}

pub fn user_profile_renders_gray_fallback_when_icon_loading_fails_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let target = UserInfo("Alice", UserId("2"))
  let #(model, _) = userinfo.init(session, target)
  let model = userinfo.Model(
    ..model,
    profile: Some(user_wrap.UserProfile(
      name: "Alice",
      icon_url: "https://example.com/missing-icon.png",
      is_public: True,
      bio: "alice bio",
      work_category: "未分類",
      friendship: "FRIEND",
    )),
    loading: False,
  )
  let #(failed_model, _) = userinfo.update(model, userinfo.IconLoadFailed)
  let rendered = userinfo.view(failed_model) |> element.to_string

  rendered
  |> string.contains("bg-gray-200")
  |> should.equal(True)

  rendered
  |> string.contains("<img")
  |> should.equal(False)
}

pub fn stored_room_id_is_parsed_test() {
  frontend.parse_stored_room_id("42")
  |> should.equal(Some(room_t.RoomId(42)))

  frontend.parse_stored_room_id("0") |> should.equal(None)
  frontend.parse_stored_room_id("invalid") |> should.equal(None)
}

pub fn full_room_does_not_transition_from_home_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let room_id = room_t.RoomId(42)
  let home_model = home.Model(
    session: session,
    rooms: [
      room_t.RoomInfo(
        roomname: room_t.RoomNameType("Full room"),
        description: room_t.DescriptionType("Description"),
        visibility: room_t.Public,
        category: room_t.Cat1,
        work_style: room_t.Quiet,
        max_number_of_member: 1,
        room_id: room_id,
        current_members: 1,
        status: "OPEN",
        joinable: False,
        join_restriction: Some("FULL"),
        created_at: "2026-09-10T00:00:00Z",
      ),
    ],
    messages: [],
  )
  let model = frontend.Model(
    current_page: frontend.Home(home_model),
    session: session,
    notification: None,
  )

  let #(updated_model, _) =
    frontend.update(model, frontend.HomeMsg(home.ToRoom(room_id)))

  case updated_model.current_page {
    frontend.Home(updated_home) ->
      updated_home.messages
      |> should.equal(["このルームは満員のため入室できません。"])
    _ -> should.fail()
  }
}

pub fn notification_updates_global_model_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let #(home_model, _) = home.init(session)
  let model = frontend.Model(
    current_page: frontend.Home(home_model),
    session: session,
    notification: None,
  )

  let #(updated_model, _) = frontend.update(
    model,
    frontend.NotificationReceived(
      "{\"type\":\"notification\",\"message\":\"ルームを作成しました。\"}",
    ),
  )

  case updated_model.notification {
    Some(message) -> message |> should.equal("ルームを作成しました。")
    None -> should.fail()
  }
}

pub fn login_shows_success_notification_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let #(login_model, _) = login.init(session)
  let model = frontend.Model(
    current_page: frontend.Login(login_model),
    session: session,
    notification: None,
  )

  let #(updated_model, _) = frontend.update(model, frontend.LoginMsg(login.ToHome))

  case updated_model.notification {
    Some(message) -> message |> should.equal("ログインしました。")
    None -> should.fail()
  }
}

pub fn logout_shows_success_notification_test() {
  let session = Authenticated(Token("session"), UserInfo("me", UserId("1")))
  let #(home_model, _) = home.init(session)
  let model = frontend.Model(
    current_page: frontend.Home(home_model),
    session: session,
    notification: None,
  )

  let #(updated_model, _) = frontend.update(
    model,
    frontend.HomeMsg(home.LogoutCompleted(Ok(Nil))),
  )

  case updated_model.notification {
    Some(message) -> message |> should.equal("ログアウトしました。")
    None -> should.fail()
  }
}
