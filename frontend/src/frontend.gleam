import lustre/element
import lustre/effect
import lustre
import gleam/io

import types/session.{stringify_session}
import home
import login
import register
import privacypolicy
import create_room
import room
import report
import invitation

pub type Page {
  Home(home.Model)
  MyPage
  Login(login.Model)
  Register(register.Model)
  CreateRoom(create_room.Model)
  Room(room.Model)
  Report(report.Model)
  Invitation(invitation.Model)
}

pub type Model {
  Model(
    current_page: Page,
    session: session.Session
  )
}

pub type Msg {
  HomeMsg(home.Msg)
  LoginMsg(login.Msg)
  RegisterMsg(register.Msg)
  CreateRoomMsg(create_room.Msg)
  RoomMsg(room.Msg)
  ReportMsg(report.Msg)
  InvitationMsg(invitation.Msg)
}

fn init(_flag) -> #(Model, effect.Effect(Msg)) {
  let #(init_home_model, _) = home.init(session.Guest)
  #(Model(
      current_page: Home(init_home_model),
      session: session.Guest
      ), 
    effect.none()
  )
}

fn update(model: Model, msg: Msg) -> #(Model, effect.Effect(Msg)) {
  io.println("--- Update ---")
  case model.current_page, msg {
    // -- home --
    Home(_), HomeMsg(home.ToMyPage) -> {
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
    }

    Home(home_model), HomeMsg(home.ToRoom(room_id)) -> {
      let #(update_home_model, _) = home.update(home_model, home.ToRoom(room_id))
      let #(init_room_model, _) = room.init(update_home_model.session, room_id)
      #(Model(..model, current_page: Room(init_room_model)), effect.none())
    }

    // -- login --

    Login(login_model), LoginMsg(login.ToHome) -> {
      let #(update_login_model, _) = login.update(login_model, login.ToHome)
      let #(init_home_model, _) = home.init(update_login_model.session)
      #(Model(session: update_login_model.session, current_page: Home(init_home_model)), effect.none())
    }

    Login(_login_model), LoginMsg(login.ToRegister) -> {
      let #(init_register_model, _) = register.init(session.Guest)
      #(Model(..model, current_page: Register(init_register_model)), effect.none())
    }

    Login(login_model), LoginMsg(login.ToPrivacyPolicy) -> {
    }

    Login(login_model), LoginMsg(login.ToTOS) -> {
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

    Register(register_model), RegisterMsg(register.ToPrivacyPolicy) -> {
    }

    Register(register_model), RegisterMsg(register.ToTOS) -> {
    }

    // register other

    Register(register_model), RegisterMsg(other) -> {
      let #(update_register_model, update_effect) = register.update(register_model, other)
      #(Model(..model, current_page: Register(update_register_model)), update_effect |> effect.map(RegisterMsg))
    }

    // -- create room --

    CreateRoom(create_room_model), CreateRoomMsg(create_room.ToHome) -> {
      let #(init_create_room_model, _) = create_room.update(create_room_model, create_room.ToHome)
      let #(init_home_model, _) = home.init(init_create_room_model.session)
      #(Model(session: init_create_room_model.session, current_page: Home(init_home_model)), effect.none())
    }

    CreateRoom(create_room_model), CreateRoomMsg(create_room.ToRoom(room_id)) -> {
      let #(update_create_room_model, _) = create_room.update(create_room_model, create_room.ToRoom(room_id))
      let #(init_room_model, _) = room.init(update_create_room_model.session, room_id)
      #(Model(..model, current_page: Room(init_room_model)), effect.none())
    }

    // create room other

    CreateRoom(create_room_model), CreateRoomMsg(other) -> {
      let #(update_create_room_model, update_effect) = create_room.update(create_room_model, other)
      #(Model(..model, current_page: CreateRoom(update_create_room_model)), update_effect |> effect.map(CreateRoomMsg))
    }

    // -- room --

    Room(room_model), RoomMsg(room.ToHome) -> {
      let #(update_room_model, _) = room.update(room_model, room.ToHome)
      let #(init_home_model, _) = home.init(update_room_model.session)

      #(Model(session: init_home_model.session, current_page: Home(init_home_model)), effect.none())
    }

    Room(room_model), RoomMsg(room.ToReport(user_id)) -> {
      let #(update_room_model, update_effect) = room.update(room_model, room.ToReport(user_id))
      let #(init_report_model, _) = report.init(update_room_model.session, update_room_model.room_id, user_id)
      #(Model(..model, current_page: Report(init_report_model)), effect.none())
    }

    Room(room_model), RoomMsg(room.ToInvitation) -> {
      let #(update_room_model, update_effect) = room.update(room_model, room.ToInvitation)

      let #(init_invitation_model, _) = invitation.init(update_room_model.session, update_room_model.room_id)
      #(Model(..model, current_page: Invitation(init_invitation_model)), effect.none())
    }

    // room other

    Room (room_model), RoomMsg(other) -> {
      let #(update_room_model, update_effect) = room.update(room_model, other)
      #(Model(..model, current_page: Room(update_room_model)), update_effect |> effect.map(RoomMsg))
    }

    // -- report --

    // Report(report_model), ReportMsg(report.ToHome) -> {
    //   
    // }

    Report(report_model), ReportMsg(report.BackToRoom) -> {
      let #(update_report_model, _) = report.update(report_model, report.BackToRoom)
      let #(init_room_model, _) = room.init(update_report_model.session, update_report_model.room_id)
      #(Model(..model, current_page: Room(init_room_model)), effect.none())
    }

    // report other

    Report(report_model), ReportMsg(other) -> {
      let #(update_report_model, update_effect) = report.update(report_model, other)
      #(Model(..model, current_page: Report(update_report_model)), update_effect |> effect.map(ReportMsg))
    }

    // -- invitation --

    Invitation(invitation_model), InvitationMsg(invitation.ToHome) -> {
    }

    Invitation(invitation_model), InvitationMsg(invitation.BackToRoom) -> {
      let #(update_invitation_model, _) = invitation.update(invitation_model, invitation.BackToRoom)
      let #(init_room_model, _) = room.init(update_invitation_model.session, update_invitation_model.room_id) 
      #(Model(..model, current_page: Room(init_room_model)), effect.none())
    }

    // invitation other

    Invitation(invitation_model), InvitationMsg(other) -> {
      let #(update_invitation_model, update_effect) = invitation.update(invitation_model, other)
      #(Model(..model, current_page: Invitation(update_invitation_model)), update_effect |> effect.map(InvitationMsg))
    }

    // TODO

    // unreachables
    _, HomeMsg(_) -> {
      io.println("unreachables")
      #(model, effect.none())
    }
    _, LoginMsg(_) -> {
      io.println("unreachables")
      #(model, effect.none())
    }
    _, RegisterMsg(_) -> {
      io.println("unreachables")
      #(model, effect.none())
    }
    _, CreateRoomMsg(_) -> {
      io.println("unreachables")
      #(model, effect.none())
    }
    _, RoomMsg(_) -> {
      io.println("unreachables")
      #(model, effect.none())
    }
    _, ReportMsg(_) -> {
      io.println("unreachables")
      #(model, effect.none())
    }
    _, InvitationMsg(_) -> {
      io.println("unreachables")
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
    CreateRoom(create_room_model) -> {
      create_room.view(create_room_model) |> element.map(CreateRoomMsg)
    }
    Room(room_model) -> {
      room.view(room_model) |> element.map(RoomMsg)
    }
    Report(report_model) -> {
      report.view(report_model) |> element.map(ReportMsg)
    }
    Invitation(invitation_model) -> {
      invitation.view(invitation_model) |> element.map(InvitationMsg)
    }
    MyPage -> {
    }
  }
}

pub fn main() {
  let app = lustre.application(init, update, view)
  io.println("Done!")
  let assert Ok(_) = lustre.start(app, "#app", Nil)
}

