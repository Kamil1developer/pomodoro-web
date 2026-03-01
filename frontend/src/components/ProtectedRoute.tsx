import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { getTokens } from '../lib/authStorage';

export function ProtectedRoute() {
  const location = useLocation();
  const tokens = getTokens();

  if (!tokens?.accessToken) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return <Outlet />;
}
