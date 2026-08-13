import { MyPage } from "./pages/MyPage";
import { WorkHistoryPage } from "./pages/WorkHistoryPage";

export function App() {
  return window.location.pathname === "/work-history" ? <WorkHistoryPage /> : <MyPage />;
}
