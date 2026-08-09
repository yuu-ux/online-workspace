import lustre/event.{on_click}
import lustre/attribute
import lustre/element
import lustre/effect
import lustre
import gleam/io
import gleam/list
import lustre/element/html.{button, div, h1, h3, hr, span, style, text}

import session/session
import home
import login

pub type Page {
  Home(home.Model)
  MyPage
  Login(login.Model)
  Register
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
      io.println("home, ToLogin")
      let #(init_login_model, _) = login.init(session.Guest)
      #(Model(..model, current_page: Login(init_login_model)), effect.none())
    }

    Home(home_model), HomeMsg(home.ToLogout) -> {
    }

    // -- login --

    Login(login_model), LoginMsg(login.ToHome) -> {
      io.println("Login, LoginHome")
      let #(init_login_model, _) = login.update(login_model, login.ToHome)
      let #(init_home_model, _) = home.init(init_login_model.session)
      #(Model(session: init_login_model.session, current_page: Home(init_home_model)), effect.none())
    }

    Login(login_model), LoginMsg(login.ToRegister) -> {
    }

    Login(login_model), LoginMsg(login.ToPrivacyPolicy) -> {
    }

    Login(login_model), LoginMsg(login.ToTOS) -> {
    }

    // other
    Login(login_model), LoginMsg(other) -> {
      let #(update_login_model, _) = login.update(login_model, other)
      #(Model(..model, current_page: Login(update_login_model)), effect.none())
    }

    // unreachables
    _, HomeMsg(_) -> {
      io.println("unreachables")
      #(model, effect.none())
    }
    _, LoginMsg(_) -> {
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
    MyPage -> {
      // div([
      //   attribute.attribute("style", "padding: 20px; font-family: sans-serif;")
      // ],
      // [
      //   text("MyPage"),
      //   button([on_click(Navigate(Home))], [text("home")])
      // ])
    }
    Register -> {

    }
  }
}

pub fn main() {
  let app = lustre.application(init, update, view)
  io.println("Done!")
  let assert Ok(_) = lustre.start(app, "#app", Nil)
}
