import { useEffect, useMemo, useState } from 'react';
import { api } from '../lib/apiClient';
import { useAppShellContext } from '../lib/useAppShellContext';
import type { DailyStat, GoalEvent, GoalExperience } from '../types/api';

function maxValue(values: number[]): number {
  if (values.length === 0) {
    return 1;
  }
  return Math.max(...values, 1);
}

export function StatisticsPage() {
  const { selectedGoal } = useAppShellContext();
  const [days, setDays] = useState<DailyStat[]>([]);
  const [experience, setExperience] = useState<GoalExperience | null>(null);
  const [events, setEvents] = useState<GoalEvent[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!selectedGoal) {
      setDays([]);
      setExperience(null);
      setEvents([]);
      return;
    }

    const run = async () => {
      setError(null);
      try {
        const [stats, experienceData, eventsData] = await Promise.all([
          api.getStats(selectedGoal.id),
          api.getGoalExperience(selectedGoal.id),
          api.getGoalEvents(selectedGoal.id)
        ]);
        setDays(stats.days.slice(-14));
        setExperience(experienceData);
        setEvents(eventsData);
      } catch (err) {
        setError((err as Error).message);
      }
    };

    void run();
  }, [selectedGoal]);

  const maxFocus = useMemo(() => maxValue(days.map((day) => day.focusMinutes)), [days]);
  const maxTasks = useMemo(() => maxValue(days.map((day) => day.completedTasks)), [days]);
  const disciplineEvents = events.filter((event) => event.type === 'DISCIPLINE_SCORE_CHANGED').slice(0, 8);

  if (!selectedGoal) {
    return (
      <section className="empty-state">
        <h2>Нет активной цели</h2>
        <p>Выберите цель для просмотра аналитики обязательства и прогноза.</p>
      </section>
    );
  }

  return (
    <div className="page-grid">
      <section className="card">
        <h3>Commitment analytics</h3>
        <p className="muted">Статистика показывает, как цель выглядит как продуктовый поток: фокус, отчёт, streak, дисциплина и риск.</p>
      </section>

      {error ? <section className="card error-card">{error}</section> : null}

      {experience?.commitment ? (
        <section className="card">
          <div className="metric-grid compact-grid">
            <div className="metric-card">
              <span>Выполненные дни</span>
              <strong>{experience.commitment.completedDays}</strong>
            </div>
            <div className="metric-card">
              <span>Пропущенные дни</span>
              <strong>{experience.commitment.missedDays}</strong>
            </div>
            <div className="metric-card">
              <span>Текущая серия</span>
              <strong>{experience.commitment.currentStreak}</strong>
            </div>
            <div className="metric-card">
              <span>Лучшая серия</span>
              <strong>{experience.commitment.bestStreak}</strong>
            </div>
            <div className="metric-card">
              <span>Дисциплина</span>
              <strong>{experience.commitment.disciplineScore}/100</strong>
            </div>
            <div className="metric-card">
              <span>Риск</span>
              <strong>{experience.commitment.riskStatus}</strong>
            </div>
            <div className="metric-card">
              <span>Прогноз</span>
              <strong>{experience.forecast.probabilityLabel}</strong>
            </div>
            <div className="metric-card">
              <span>On track</span>
              <strong>{experience.forecast.onTrack ? 'Да' : 'Нет'}</strong>
            </div>
          </div>
        </section>
      ) : (
        <section className="card warning-note">
          Для этой цели ещё нет обязательства. Сначала настройте его, чтобы аналитика стала полной.
        </section>
      )}

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
        <h3>Forecast</h3>
        <p>{experience?.forecast.explanation ?? 'Прогноз появится после первых фокус-сессий.'}</p>
        <div className="chips">
          <span className="chip">Среднее в день: {experience?.forecast.averageDailyMinutes ?? 0} мин.</span>
          <span className="chip">Осталось: {experience?.forecast.remainingMinutes ?? 0} мин.</span>
          <span className="chip">
            Оценка завершения: {experience?.forecast.estimatedCompletionDate ?? 'недостаточно данных'}
          </span>
        </div>
      </section>

      <section className="card">
        <h3>История изменения дисциплины</h3>
        {disciplineEvents.length === 0 ? (
          <p className="muted">Изменения дисциплины появятся после первых закрытых дней.</p>
        ) : (
          <ul className="timeline-list">
            {disciplineEvents.map((event) => (
              <li key={event.id}>
                <strong>{event.title}</strong>
                <p>{event.description || 'Без описания.'}</p>
                <small className="muted">
                  {event.oldValue ?? '—'} → {event.newValue ?? '—'}
                </small>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
