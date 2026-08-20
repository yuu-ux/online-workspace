import { LoginPage } from "./features/auth/pages/LoginPage";
import { RegistrationPage } from "./features/auth/pages/RegistrationPage";
import { SessionStatusPage } from "./features/auth/pages/SessionStatusPage";

export function App() {
  if (window.location.pathname === "/register") {
    return <RegistrationPage />;
  }
  if (window.location.pathname === "/login") {
    return <LoginPage />;
  }
  return <SessionStatusPage />;
}
