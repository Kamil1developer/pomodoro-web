import { useEffect, useMemo, useState } from 'react';
import { api } from '../lib/apiClient';
import { useAppShellContext } from '../lib/useAppShellContext';
import type { DailyStat } from '../types/api';

function maxValue(values: number[]): number {
  if (values.length === 0) {
    return 1;
  }
  return Math.max(...values, 1);
}

export function StatisticsPage() {
  const { selectedGoal } = useAppShellContext();
  const [days, setDays] = useState<DailyStat[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!selectedGoal) {
      setDays([]);
      return;
    }

    const run = async () => {
      setError(null);
      try {
        const stats = await api.getStats(selectedGoal.id);
        setDays(stats.days.slice(-14));
      } catch (err) {
        setError((err as Error).message);
      }
    };

    void run();
  }, [selectedGoal]);

  const maxFocus = useMemo(() => maxValue(days.map((day) => day.focusMinutes)), [days]);
  const maxTasks = useMemo(() => maxValue(days.map((day) => day.completedTasks)), [days]);

  if (!selectedGoal) {
    return (
      <section className="empty-state">
        <h2>Нет активной цели</h2>
        <p>Выберите цель для просмотра статистики.</p>
      </section>
    );
  }

  return (
    <div className="page-grid">
      <section className="card">
        <h3>Статистика за последние 14 дней</h3>
        <p className="muted">Если блоки пустые, дождитесь ежедневного перерасчета scheduler (00:05).</p>
      </section>

      {error ? <section className="card error-card">{error}</section> : null}

      <section className="card">
        <h3>Фокус-минуты по дням</h3>
        <div className="bars">
          {days.map((day) => {
            const height = Math.max(8, Math.round((day.focusMinutes / maxFocus) * 100));
            return (
              <div className="bar-col" key={day.date}>
                <div className="bar" style={{ height: `${height}%` }} title={`${day.date}: ${day.focusMinutes} мин`} />
                <small>{day.date.slice(5)}</small>
              </div>
            );
          })}
        </div>
      </section>

      <section className="card">
        <h3>Выполненные задачи по дням</h3>
        <div className="bars secondary">
          {days.map((day) => {
            const height = Math.max(8, Math.round((day.completedTasks / maxTasks) * 100));
            return (
              <div className="bar-col" key={`${day.date}-tasks`}>
                <div className="bar" style={{ height: `${height}%` }} title={`${day.date}: ${day.completedTasks} задач`} />
                <small>{day.date.slice(5)}</small>
              </div>
            );
          })}
        </div>
      </section>

      <section className="card">
        <h3>Streak</h3>
        <div className="streak-line">
          {days.map((day) => (
            <span key={`${day.date}-streak`} className="streak-pill">
              {day.date.slice(5)} · {day.streak}
            </span>
          ))}
        </div>
      </section>
    </div>
  );
}
