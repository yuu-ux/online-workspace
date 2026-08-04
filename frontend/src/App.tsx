import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { MyPage } from "./pages/MyPage";
import { WorkHistoryPage } from "./pages/WorkHistoryPage";

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/my-page" replace />} />
        <Route path="/my-page" element={<MyPage />} />
        <Route path="/work-history" element={<WorkHistoryPage />} />
        <Route path="*" element={<Navigate to="/my-page" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
