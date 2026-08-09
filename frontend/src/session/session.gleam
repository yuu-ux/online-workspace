pub type Session {
  Guest
  Authenticated(jwt: String, user_id: String)
}
