import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { api } from '../lib/apiClient';
import { clearTokens, getTokens } from '../lib/authStorage';
import type { Goal } from '../types/api';
import type { AppShellContext } from '../types/app';
import { GoalSelector } from './GoalSelector';

const SELECTED_GOAL_KEY = 'pomodoro_selected_goal_id';

function getStoredGoalId(): number | null {
  const raw = localStorage.getItem(SELECTED_GOAL_KEY);
  if (!raw) {
    return null;
  }
  const parsed = Number(raw);
  return Number.isNaN(parsed) ? null : parsed;
}

export function AppShell() {
  const navigate = useNavigate();
  const [goals, setGoals] = useState<Goal[]>([]);
  const [selectedGoalId, setSelectedGoalIdState] = useState<number | null>(getStoredGoalId());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const reloadGoals = useCallback(async () => {
    setError(null);
    const list = await api.getGoals();
    setGoals(list);

    if (list.length === 0) {
      setSelectedGoalIdState(null);
      localStorage.removeItem(SELECTED_GOAL_KEY);
      return;
    }

    const containsSelected = selectedGoalId !== null && list.some((goal) => goal.id === selectedGoalId);
    if (!containsSelected) {
      const nextId = list[0].id;
      setSelectedGoalIdState(nextId);
      localStorage.setItem(SELECTED_GOAL_KEY, String(nextId));
    }
  }, [selectedGoalId]);

  useEffect(() => {
    const run = async () => {
      try {
        await reloadGoals();
      } catch (err) {
        setError((err as Error).message);
      } finally {
        setLoading(false);
      }
    };

    void run();
  }, [reloadGoals]);

  const setSelectedGoalId = useCallback((goalId: number | null) => {
    setSelectedGoalIdState(goalId);
    if (goalId === null) {
      localStorage.removeItem(SELECTED_GOAL_KEY);
      return;
    }
    localStorage.setItem(SELECTED_GOAL_KEY, String(goalId));
  }, []);

  const logout = useCallback(async () => {
    const tokens = getTokens();
    try {
      if (tokens?.refreshToken) {
        await api.logout(tokens.refreshToken);
      }
    } catch {
      // Ignore network/logout errors and clear local session anyway.
    } finally {
      clearTokens();
      navigate('/login', { replace: true });
    }
  }, [navigate]);

  const selectedGoal = useMemo(
    () => goals.find((goal) => goal.id === selectedGoalId) ?? null,
    [goals, selectedGoalId]
  );

  const context: AppShellContext = {
    goals,
    selectedGoal,
    selectedGoalId,
    setSelectedGoalId,
    reloadGoals,
    logout
  };

  if (loading) {
    return <div className="screen-center">Загрузка приложения...</div>;
  }

  if (error) {
    return (
      <div className="screen-center">
        <h2>Не удалось загрузить данные</h2>
        <p>{error}</p>
        <button className="btn" onClick={() => void reloadGoals()}>
          Повторить
        </button>
      </div>
    );
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <Link to="/" className="brand">
          Pomodoro Web
        </Link>
        <p className="brand-subtitle">Контроль цели + мотивация</p>

        <nav className="main-nav">
          <NavLink to="/" end>
            Dashboard
          </NavLink>
          <NavLink to="/control">Контроль</NavLink>
          <NavLink to="/motivation">Мотивация</NavLink>
          <NavLink to="/chat">Чат</NavLink>
          <NavLink to="/stats">Статистика</NavLink>
        </nav>

        <button className="btn btn-ghost" onClick={() => void logout()}>
          Выйти
        </button>
      </aside>

      <main className="content">
        <header className="topbar">
          <GoalSelector
            goals={goals}
            selectedGoalId={selectedGoalId}
            onSelect={(goalId) => setSelectedGoalId(goalId)}
          />
        </header>

        <section className="page-container">
          <Outlet context={context} />
        </section>
      </main>
    </div>
  );
}
