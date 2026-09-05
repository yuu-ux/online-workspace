import lustre/element
import lustre/effect
import lustre

import types/session
import wrap/session as session_api
import wrap/api as api

import pages/home
import pages/login
import pages/register
import pages/privacypolicy
import pages/terms_of_service
import pages/create_room
import pages/room
import pages/mypage
import pages/friend
import pages/profile
import pages/history
import pages/search

import pages/userinfofromfriend
import pages/userinfofromroom
import pages/userinfofromsearch

pub type Page {
  Home(home.Model)
  MyPage(mypage.Model)
  Friend(friend.Model)
  Profile(profile.Model)
  History(history.Model)
  Login(login.Model)
  Register(register.Model)
  PrivacyPolicy(privacypolicy.Model)
  TermsOfService(terms_of_service.Model)
  CreateRoom(create_room.Model)
  Room(room.Model)
  Search(search.Model)
  // UserInfo
  UserInfoFromFriend(userinfofromfriend.Model)
  UserInfoFromRoom(userinfofromroom.Model)
  UserInfoFromSearch(userinfofromsearch.Model)
}

pub type Model {
  Model(
    current_page: Page,
    session: session.Session
  )
}

pub type Msg {
  MyPageMsg(mypage.Msg)
  FriendMsg(friend.Msg)
  ProfileMsg(profile.Msg)
  HistoryMsg(history.Msg)
  HomeMsg(home.Msg)
  LoginMsg(login.Msg)
  RegisterMsg(register.Msg)
  PrivacyPolicyMsg(privacypolicy.Msg)
  TermsOfServiceMsg(terms_of_service.Msg)
  CreateRoomMsg(create_room.Msg)
  RoomMsg(room.Msg)
  SearchMsg(search.Msg)
  UserInfoFromFriendMsg(userinfofromfriend.Msg)
  UserInfoFromRoomMsg(userinfofromroom.Msg)
  SessionLoaded(Result(session.Session, api.ApiError))
  UserInfoFromSearchMsg(userinfofromsearch.Msg)
}

fn init(_flag) -> #(Model, effect.Effect(Msg)) {
  let #(init_home_model, _) = home.init(session.Guest)
  #(Model(
      current_page: Home(init_home_model),
      session: session.Guest
      ), 
    session_api.get_session(SessionLoaded)
  )
}

fn update(model: Model, msg: Msg) -> #(Model, effect.Effect(Msg)) {
  case model.current_page, msg {

    _, SessionLoaded(Ok(current_session)) -> {
      let #(home_model, home_effect) = home.init(current_session)
      #(
        Model(session: current_session, current_page: Home(home_model)),
        home_effect |> effect.map(HomeMsg),
      )
    }

    _, SessionLoaded(Error(_)) -> #(model, effect.none())

    // -- mypage --

    MyPage(mypage_model), MyPageMsg(mypage.ToHome) -> {
      let #(update_mypage_model, _) = mypage.update(mypage_model, mypage.ToHome)
      let #(init_home_model, home_effect) = home.init(update_mypage_model.session)
      #(
        Model(session: update_mypage_model.session, current_page: Home(init_home_model)),
        home_effect |> effect.map(HomeMsg),
      )
    }

    MyPage(mypage_model), MyPageMsg(mypage.ToFriend) -> {
      let #(update_mypage_model, _) = mypage.update(mypage_model, mypage.ToFriend)
      let #(init_friend_model, _) = friend.init(update_mypage_model.session)
      #(Model(session: update_mypage_model.session, current_page: Friend(init_friend_model)), effect.none())
    }

    MyPage(mypage_model), MyPageMsg(mypage.ToHistory) -> {
      let #(update_mypage_model, _) = mypage.update(mypage_model, mypage.ToHistory)
      let #(init_history_model, _) = history.init(update_mypage_model.session)
      #(Model(session: update_mypage_model.session, current_page: History(init_history_model)), effect.none())
    }

    MyPage(mypage_model), MyPageMsg(mypage.ToProfile) -> {
      let #(update_mypage_model, _) = mypage.update(mypage_model, mypage.ToProfile)
      let #(init_profile_model, _) = profile.init(update_mypage_model.session)
      #(Model(session: init_profile_model.session, current_page: Profile(init_profile_model)), effect.none())
    }

    MyPage(mypage_model), MyPageMsg(mypage.SubmitClicked) -> {
      let #(init_search_model, _) = search.init(mypage_model.session, mypage_model.current_user_name)
      #(Model(..model, current_page: Search(init_search_model)), effect.none())
    }

    // mypage other

    MyPage(mypage_model), MyPageMsg(other) -> {
      let #(update_mypage_model, update_effect) = mypage.update(mypage_model, other)
      #(Model(..model, current_page: MyPage(update_mypage_model)), update_effect |> effect.map(MyPageMsg))
    }

    // -- friend --

    Friend(friend_model), FriendMsg(friend.ToMyPage) -> {
      let #(update_friend_model, _) = friend.update(friend_model, friend.ToMyPage)
      let #(init_mypage_model, _) = mypage.init(update_friend_model.session)
      #(Model(..model, current_page: MyPage(init_mypage_model)), effect.none())
    }

    Friend(friend_model), FriendMsg(friend.ToHome) -> {
      let #(init_home_model, _) = home.init(friend_model.session)
      #(Model(..model, current_page: Home(init_home_model)), effect.none())
    }

    Friend(friend_model), FriendMsg(friend.ToUserInfo(user_info)) -> {
      let #(update_user_info_model, _) = userinfofromfriend.init(friend_model.session, user_info)
      #(Model(..model, current_page: UserInfoFromFriend(update_user_info_model)), effect.none())
    }

    // friend other

    // -- profile --

    Profile(profile_model), ProfileMsg(profile.ToMyPage) -> {
      let #(update_profile_model, _) = profile.update(profile_model, profile.ToMyPage)
      let #(init_mypage_model, _) = mypage.init(update_profile_model.session)
      #(Model(..model, current_page: MyPage(init_mypage_model)), effect.none())
    }

    Profile(profile_model), ProfileMsg(profile.ToHome) -> {
      let #(init_home_model, _) = home.init(profile_model.session)
      #(Model(..model, current_page: Home(init_home_model)), effect.none())
    }

    // profile other

    // -- history --

    History(history_model), HistoryMsg(history.ToMyPage) -> {
      let #(update_history_model, _) = history.update(history_model, history.ToMyPage)
      let #(init_mypage_model, _) = mypage.init(update_history_model.session)
      #(Model(..model, current_page: MyPage(init_mypage_model)), effect.none())
    }

    History(history_model), HistoryMsg(history.ToHome) -> {
      let #(init_home_model, _) = home.init(history_model.session)
      #(Model(..model, current_page: Home(init_home_model)), effect.none())
    }

    // history other

    // -- home --
    Home(home_model), HomeMsg(home.ToMyPage) -> {
      let #(init_my_page, _) = mypage.init(home_model.session)
      #(Model(..model, current_page: MyPage(init_my_page)), effect.none())
    }

    Home(_), HomeMsg(home.ToLogin) -> {
      let #(init_login_model, _) = login.init(session.Guest)
      #(Model(..model, current_page: Login(init_login_model)), effect.none())
    }

    Home(home_model), HomeMsg(home.ToCreateRoom) -> {
      let #(init_create_room_model, _) = create_room.init(home_model.session)
      #(Model(..model, current_page: CreateRoom(init_create_room_model)), effect.none())
    }

    Home(home_model), HomeMsg(home.ToLogout) -> {
      let #(update_home_model, update_effect) = home.update(home_model, home.ToLogout)
      #(
        Model(..model, current_page: Home(update_home_model)),
        update_effect |> effect.map(HomeMsg),
      )
    }

    Home(home_model), HomeMsg(home.ToRoom(room_id)) -> {
      let #(update_home_model, _) = home.update(home_model, home.ToRoom(room_id))
      let #(init_room_model, update_effect) = room.init(update_home_model.session, room_id)
      #(Model(..model, current_page: Room(init_room_model)), update_effect |> effect.map(RoomMsg))
    }

    Home(home_model), HomeMsg(home.RoomsLoaded(response)) -> {
      let #(update_home_model, update_effect) = home.update(home_model, home.RoomsLoaded(response))
      #(
        Model(..model, current_page: Home(update_home_model)),
        update_effect |> effect.map(HomeMsg),
      )
    }

    Home(home_model), HomeMsg(home.LogoutCompleted(response)) -> {
      let #(update_home_model, update_effect) = home.update(home_model, home.LogoutCompleted(response))
      #(
        Model(
          session: update_home_model.session,
          current_page: Home(update_home_model),
        ),
        update_effect |> effect.map(HomeMsg),
      )
    }

    // -- login --

    Login(login_model), LoginMsg(login.ToHome) -> {
      let #(update_login_model, _) = login.update(login_model, login.ToHome)
      let #(init_home_model, home_effect) = home.init(update_login_model.session)
      #(
        Model(session: update_login_model.session, current_page: Home(init_home_model)),
        home_effect |> effect.map(HomeMsg),
      )
    }

    Login(_login_model), LoginMsg(login.ToRegister) -> {
      let #(init_register_model, _) = register.init(session.Guest)
      #(Model(..model, current_page: Register(init_register_model)), effect.none())
    }

    Login(_login_model), LoginMsg(login.ToPrivacyPolicy) -> {
      let #(init_privacy_policy_model, _) = privacypolicy.init(model.session)
      #(Model(..model, current_page: PrivacyPolicy(init_privacy_policy_model)), effect.none())
    }

    Login(_login_model), LoginMsg(login.ToTOS) -> {
      let #(init_terms_model, _) = terms_of_service.init(model.session)
      #(Model(..model, current_page: TermsOfService(init_terms_model)), effect.none())
    }

    // login other
    Login(login_model), LoginMsg(other) -> {
      let #(update_login_model, update_effect) = login.update(login_model, other)
      #(Model(..model, current_page: Login(update_login_model)), update_effect |> effect.map(LoginMsg))
    }

    // -- register --

    Register(_register_model), RegisterMsg(register.ToLogin) -> {
      let #(init_login_model, _) = login.init(session.Guest)
      #(Model(..model, current_page: Login(init_login_model)), effect.none())
    }

    Register(register_model), RegisterMsg(register.ToHome) -> {
      let #(init_register_model, _) = register.update(register_model, register.ToHome)
      let #(init_home_model, _) = home.init(init_register_model.session)
      #(Model(session: init_register_model.session, current_page: Home(init_home_model)), effect.none())
    }

    Register(_register_model), RegisterMsg(register.ToPrivacyPolicy) -> {
      let #(init_privacy_policy_model, _) = privacypolicy.init(model.session)
      #(Model(..model, current_page: PrivacyPolicy(init_privacy_policy_model)), effect.none())
    }

    Register(_register_model), RegisterMsg(register.ToTOS) -> {
      let #(init_terms_model, _) = terms_of_service.init(model.session)
      #(Model(..model, current_page: TermsOfService(init_terms_model)), effect.none())
    }

    // register other

    Register(register_model), RegisterMsg(other) -> {
      let #(update_register_model, update_effect) = register.update(register_model, other)
      #(Model(..model, current_page: Register(update_register_model)), update_effect |> effect.map(RegisterMsg))
    }

    // -- create room --

    CreateRoom(create_room_model), CreateRoomMsg(create_room.ToHome) -> {
      let #(init_create_room_model, _) = create_room.update(create_room_model, create_room.ToHome)
      let #(init_home_model, home_effect) = home.init(init_create_room_model.session)
      #(
        Model(session: init_create_room_model.session, current_page: Home(init_home_model)),
        home_effect |> effect.map(HomeMsg),
      )
    }

    CreateRoom(create_room_model), CreateRoomMsg(create_room.ToRoom(room_id)) -> {
      let #(update_create_room_model, _) = create_room.update(create_room_model, create_room.ToRoom(room_id))
      let #(init_room_model, init_effect) = room.init(update_create_room_model.session, room_id)
      #(Model(..model, current_page: Room(init_room_model)), init_effect |> effect.map(RoomMsg))
    }

    // create room other

    CreateRoom(create_room_model), CreateRoomMsg(other) -> {
      let #(update_create_room_model, update_effect) = create_room.update(create_room_model, other)
      #(Model(..model, current_page: CreateRoom(update_create_room_model)), update_effect |> effect.map(CreateRoomMsg))
    }

    // -- room --

    Room(room_model), RoomMsg(room.ToHome) -> {
      let #(update_room_model, _) = room.update(room_model, room.ToHome)
      let #(init_home_model, home_effect) = home.init(update_room_model.session)

      #(
        Model(session: init_home_model.session, current_page: Home(init_home_model)),
        home_effect |> effect.map(HomeMsg),
      )
    }

    Room(room_model), RoomMsg(room.ToUserInfo(user_info)) -> {
      let #(update_user_info_model, _) = userinfofromroom.init(room_model.session, user_info, room_model.room_id)
      #(Model(..model, current_page: UserInfoFromRoom(update_user_info_model)), effect.none())
    }

    // room other

    Room (room_model), RoomMsg(other) -> {
      let #(update_room_model, update_effect) = room.update(room_model, other)
      #(Model(..model, current_page: Room(update_room_model)), update_effect |> effect.map(RoomMsg))
    }

    // -- userinfofromfriend --

    UserInfoFromFriend(userinfofromfriend_model), UserInfoFromFriendMsg(userinfofromfriend.ToFriend) -> {
      let #(init_friend_model, _) = friend.init(userinfofromfriend_model.user_info_component.session)
      #(Model(..model, current_page: Friend(init_friend_model)), effect.none())
    }

    // userinfofromfriend other

    UserInfoFromFriend(userinfofromfriend_model), UserInfoFromFriendMsg(other) -> {
      let #(update_userinfofromfriend_model, update_effect) = userinfofromfriend.update(userinfofromfriend_model, other)

      #(Model(..model, current_page: UserInfoFromFriend(update_userinfofromfriend_model)), update_effect |> effect.map(UserInfoFromFriendMsg))
    }

    // -- userinfofromroom --

    UserInfoFromRoom(userinfofromroom_model), UserInfoFromRoomMsg(userinfofromroom.ToRoom(room_id)) -> {
      let #(init_room_model, init_effect) = room.init(userinfofromroom_model.user_info_component.session, room_id)
      #(Model(..model, current_page: Room(init_room_model)), init_effect |> effect.map(RoomMsg))
    }

    // userinfofromroom other

    UserInfoFromRoom(userinfofromroom_model), UserInfoFromRoomMsg(other) -> {
      let #(update_userinfofromroom_model, update_effect) = userinfofromroom.update(userinfofromroom_model, other)
      #(Model(..model, current_page: UserInfoFromRoom(update_userinfofromroom_model)), update_effect |> effect.map(UserInfoFromRoomMsg))
    }

    // -- privacy policy / terms of service --

    PrivacyPolicy(_), PrivacyPolicyMsg(privacypolicy.ToHome) -> {
      let #(init_home_model, _) = home.init(model.session)
      #(Model(..model, current_page: Home(init_home_model)), effect.none())
    }

    TermsOfService(_), TermsOfServiceMsg(terms_of_service.ToHome) -> {
      let #(init_home_model, _) = home.init(model.session)
      #(Model(..model, current_page: Home(init_home_model)), effect.none())
    }

    // -- search --

    Search(search_model), SearchMsg(search.ToHome) -> {
      let #(init_home_model, _) = home.init(search_model.session)
      #(Model(..model, current_page: Home(init_home_model)), effect.none())
    }

    Search(search_model), SearchMsg(search.ToMyPage) -> {
      let #(init_mypage_model, _) = mypage.init(search_model.session)
      #(Model(..model, current_page: MyPage(init_mypage_model)), effect.none())
    }

    Search(search_model), SearchMsg(search.ToUserInfo(user_info)) -> {
      let #(init_user_info_model, _) = userinfofromsearch.init(
        search_model.session,
        user_info,
        search_model.search_word,
      )
      #(Model(..model, current_page: UserInfoFromSearch(init_user_info_model)), effect.none())
    }

    // -- userinfofromsearch --

    UserInfoFromSearch(userinfo_model), UserInfoFromSearchMsg(userinfofromsearch.ToSearch(search_word)) -> {
      let #(init_search_model, _) = search.init(userinfo_model.user_info_component.session, search_word)
      #(Model(..model, current_page: Search(init_search_model)), effect.none())
    }

    UserInfoFromSearch(userinfo_model), UserInfoFromSearchMsg(other) -> {
      let #(updated_userinfo_model, update_effect) = userinfofromsearch.update(userinfo_model, other)
      #(
        Model(..model, current_page: UserInfoFromSearch(updated_userinfo_model)),
        update_effect |> effect.map(UserInfoFromSearchMsg),
      )
    }

    // TODO

    // unreachables
    _, HomeMsg(_) -> {
      #(model, effect.none())
    }
    _, LoginMsg(_) -> {
      #(model, effect.none())
    }
    _, RegisterMsg(_) -> {
      #(model, effect.none())
    }
    _, PrivacyPolicyMsg(_) -> {
      #(model, effect.none())
    }
    _, TermsOfServiceMsg(_) -> {
      #(model, effect.none())
    }
    _, CreateRoomMsg(_) -> {
      #(model, effect.none())
    }
    _, RoomMsg(_) -> {
      #(model, effect.none())
    }
    _, SearchMsg(_) -> {
      #(model, effect.none())
    }
    _, MyPageMsg(_) -> {
      #(model, effect.none())
    }
    _, FriendMsg(_) -> {
      #(model, effect.none())
    }
    _, ProfileMsg(_) -> {
      #(model, effect.none())
    }
    _, HistoryMsg(_) -> {
      #(model, effect.none())
    }

    _, UserInfoFromFriendMsg(_) -> {
      #(model, effect.none())
    }

    _, UserInfoFromRoomMsg(_) -> {
      #(model, effect.none())
    }

    _, UserInfoFromSearchMsg(_) -> {
      #(model, effect.none())
    }

  }
}

fn view (model: Model) -> element.Element(Msg) {
  case model.current_page {
    Home(home_model) -> {
      home.view(home_model) |> element.map(HomeMsg)
    }
    Login(login_model) -> {
      login.view(login_model) |> element.map(LoginMsg)
    }
    Register(register_model) -> {
      register.view(register_model) |> element.map(RegisterMsg)
    }
    PrivacyPolicy(privacy_policy_model) -> {
      privacypolicy.view(privacy_policy_model) |> element.map(PrivacyPolicyMsg)
    }
    TermsOfService(terms_model) -> {
      terms_of_service.view(terms_model) |> element.map(TermsOfServiceMsg)
    }
    CreateRoom(create_room_model) -> {
      create_room.view(create_room_model) |> element.map(CreateRoomMsg)
    }
    Room(room_model) -> {
      room.view(room_model) |> element.map(RoomMsg)
    }
    MyPage(mypage_model) -> {
      mypage.view(mypage_model) |> element.map(MyPageMsg)
    }
    Friend(friend_model) -> {
      friend.view(friend_model) |> element.map(FriendMsg)
    }
    Profile(profile_model) -> {
      profile.view(profile_model) |> element.map(ProfileMsg)
    }
    History(history_model) -> {
      history.view(history_model) |> element.map(HistoryMsg)
    }
    Search(search_model) -> {
      search.view(search_model) |> element.map(SearchMsg)
    }

    // UserInfo
    UserInfoFromFriend(userinfofromfriend_model) -> {
      userinfofromfriend.view(userinfofromfriend_model) |> element.map(UserInfoFromFriendMsg)
    }

    UserInfoFromRoom(userinfofromroom_model) -> {
      userinfofromroom.view(userinfofromroom_model) |> element.map(UserInfoFromRoomMsg)
    }
    UserInfoFromSearch(userinfofromsearch_model) -> {
      userinfofromsearch.view(userinfofromsearch_model) |> element.map(UserInfoFromSearchMsg)
    }
  }
}

pub fn main() {
  let app = lustre.application(init, update, view)
  let assert Ok(_) = lustre.start(app, "#app", Nil)
}
