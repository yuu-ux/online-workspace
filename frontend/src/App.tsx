import { RegistrationPage } from "./features/auth/pages/RegistrationPage";
import { SessionStatusPage } from "./features/auth/pages/SessionStatusPage";

export function App() {
  return window.location.pathname === "/register" ? (
    <RegistrationPage />
  ) : (
    <SessionStatusPage />
  );
}
