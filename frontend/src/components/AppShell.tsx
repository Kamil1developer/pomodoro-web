import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { api } from '../lib/apiClient';
import { clearTokens, getTokens } from '../lib/authStorage';
import type { Goal } from '../types/api';
import type { AppShellContext } from '../types/app';
import { GoalSelector } from './GoalSelector';

const SELECTED_GOAL_KEY = 'pomodoro_selected_goal_id';
const NAV_COLLAPSED_KEY = 'pomodoro_nav_collapsed';
const DEFAULT_THEME_COLOR = '#dff6e5';

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
  const [isNavCollapsed, setIsNavCollapsed] = useState(
    () => localStorage.getItem(NAV_COLLAPSED_KEY) === '1'
  );
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

  const toggleNav = useCallback(() => {
    setIsNavCollapsed((prev) => {
      const next = !prev;
      localStorage.setItem(NAV_COLLAPSED_KEY, next ? '1' : '0');
      return next;
    });
  }, []);

  const logout = useCallback(async () => {
    const tokens = getTokens();
    try {
      if (tokens?.refreshToken) {
        await api.logout(tokens.refreshToken);
      }
    } catch {
      // Игнорируем ошибки сети/logout и в любом случае очищаем локальную сессию.
    } finally {
      clearTokens();
      navigate('/login', { replace: true });
    }
  }, [navigate]);

  const selectedGoal = useMemo(
    () => goals.find((goal) => goal.id === selectedGoalId) ?? null,
    [goals, selectedGoalId]
  );

  useEffect(() => {
    const base = selectedGoal?.themeColor ?? DEFAULT_THEME_COLOR;
    const soft = tintColor(base, 0.84);
    const deep = shadeColor(base, 0.52);
    const primary = shadeColor(base, 0.66);
    const primaryStrong = shadeColor(base, 0.5);
    const root = document.documentElement;
    root.style.setProperty('--goal-color', base);
    root.style.setProperty('--goal-color-soft', soft);
    root.style.setProperty('--goal-color-deep', deep);
    root.style.setProperty('--primary', primary);
    root.style.setProperty('--primary-strong', primaryStrong);
  }, [selectedGoal]);

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
      <aside className={`sidebar ${isNavCollapsed ? 'sidebar-collapsed' : ''}`}>
        <Link to="/" className="brand">
          Pomodoro Web
        </Link>
        <p className="brand-subtitle">Goal Experience: фокус, отчёты, streak и мотивация в одной цели</p>

        <button className="btn btn-ghost sidebar-toggle" onClick={toggleNav} type="button">
          {isNavCollapsed ? 'Развернуть вкладки' : 'Свернуть вкладки'}
        </button>

        <nav className="main-nav">
          <NavLink to="/" end>
            Dashboard
          </NavLink>
          <NavLink to="/control">Контроль</NavLink>
          <NavLink to="/focus">Фокус</NavLink>
          <NavLink to="/motivation">Мотивация</NavLink>
          <NavLink to="/chat">Мотиватор</NavLink>
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

function tintColor(hex: string, amount: number): string {
  const rgb = parseHex(hex);
  if (!rgb) {
    return DEFAULT_THEME_COLOR;
  }
  return toHex({
    r: Math.round(rgb.r + (255 - rgb.r) * amount),
    g: Math.round(rgb.g + (255 - rgb.g) * amount),
    b: Math.round(rgb.b + (255 - rgb.b) * amount)
  });
}

function shadeColor(hex: string, amount: number): string {
  const rgb = parseHex(hex);
  if (!rgb) {
    return '#c8d2be';
  }
  return toHex({
    r: Math.round(rgb.r * amount),
    g: Math.round(rgb.g * amount),
    b: Math.round(rgb.b * amount)
  });
}

function parseHex(hex: string): { r: number; g: number; b: number } | null {
  const normalized = hex.trim();
  const match = /^#([0-9a-f]{6})$/i.exec(normalized);
  if (!match) {
    return null;
  }
  const intVal = Number.parseInt(match[1], 16);
  return {
    r: (intVal >> 16) & 0xff,
    g: (intVal >> 8) & 0xff,
    b: intVal & 0xff
  };
}

function toHex(rgb: { r: number; g: number; b: number }): string {
  const clamp = (value: number) => Math.max(0, Math.min(255, value));
  const encode = (value: number) => clamp(value).toString(16).padStart(2, '0');
  return `#${encode(rgb.r)}${encode(rgb.g)}${encode(rgb.b)}`;
}
