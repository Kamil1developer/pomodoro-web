import { Navigate, Route, Routes } from 'react-router-dom';
import { AppShell } from './components/AppShell';
import { ProtectedRoute } from './components/ProtectedRoute';
import { ChatPage } from './pages/ChatPage';
import { ControlPage } from './pages/ControlPage';
import { DashboardPage } from './pages/DashboardPage';
import { FocusPage } from './pages/FocusPage';
import { LoginPage } from './pages/LoginPage';
import { MotivationPage } from './pages/MotivationPage';
import { RegisterPage } from './pages/RegisterPage';
import { StatisticsPage } from './pages/StatisticsPage';

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      <Route element={<ProtectedRoute />}>
        <Route path="/" element={<AppShell />}>
          <Route index element={<DashboardPage />} />
          <Route path="control" element={<ControlPage />} />
          <Route path="focus" element={<FocusPage />} />
          <Route path="motivation" element={<MotivationPage />} />
          <Route path="chat" element={<ChatPage />} />
          <Route path="stats" element={<StatisticsPage />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
