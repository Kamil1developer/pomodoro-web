import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { api, resolveAssetUrl } from '../lib/apiClient';
import { minutesToHours, shortDate, shortDateTime } from '../lib/format';
import { useAppShellContext } from '../lib/useAppShellContext';
import type { FocusSession, GoalExperience, ReportItem } from '../types/api';

export function GoalExperiencePage() {
  const { goalId } = useParams();
  const parsedGoalId = Number(goalId);
  const { setSelectedGoalId } = useAppShellContext();
  const [experience, setExperience] = useState<GoalExperience | null>(null);
  const [reports, setReports] = useState<ReportItem[]>([]);
  const [sessions, setSessions] = useState<FocusSession[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!Number.isFinite(parsedGoalId)) {
      return;
    }
    setSelectedGoalId(parsedGoalId);

    const run = async () => {
      setLoading(true);
      setError(null);
      try {
        const [experienceData, reportsData, sessionsData] = await Promise.all([
          api.getGoalExperience(parsedGoalId),
          api.getReports(parsedGoalId),
          api.getFocusSessions(parsedGoalId)
        ]);
        setExperience(experienceData);
        setReports(reportsData);
        setSessions(sessionsData);
      } catch (err) {
        setError((err as Error).message);
      } finally {
        setLoading(false);
      }
    };

    void run();
  }, [parsedGoalId, setSelectedGoalId]);

  if (!Number.isFinite(parsedGoalId)) {
    return <section className="card error-card">Некорректный идентификатор цели.</section>;
  }

  if (loading) {
    return <section className="card">Загрузка Goal Experience...</section>;
  }

  if (error) {
    return <section className="card error-card">{error}</section>;
  }

  if (!experience) {
    return <section className="card">Цель не найдена.</section>;
  }

  const totalFocusMinutes = sessions.reduce((sum, session) => sum + (session.durationMinutes ?? 0), 0);

  return (
    <div className="page-grid">
      <section className="card page-intro">
        <h2>{experience.goal.title}</h2>
        <p>{experience.goal.description || 'Описание цели пока не добавлено.'}</p>
        <div className="chips">
          <span className="chip">Создана: {shortDate(experience.goal.createdAt)}</span>
          {experience.goal.deadline ? <span className="chip">Дедлайн: {shortDate(experience.goal.deadline)}</span> : null}
          <span className="chip">Серия: {experience.today.currentStreak ?? 0} дн.</span>
          <span className="chip">Дисциплина: {experience.today.disciplineScore ?? 0}/100</span>
        </div>
      </section>

      <section className="card">
        <h3>Сегодня</h3>
        <div className="metric-grid compact-grid">
          <div className="metric-card">
            <span>Норма</span>
            <strong>{experience.today.dailyTargetMinutes ?? 0} мин.</strong>
          </div>
          <div className="metric-card">
            <span>Выполнено</span>
            <strong>{experience.today.completedFocusMinutesToday} мин.</strong>
          </div>
          <div className="metric-card">
            <span>Осталось</span>
            <strong>{experience.today.remainingMinutesToday ?? 0} мин.</strong>
          </div>
          <div className="metric-card">
            <span>Статус отчёта</span>
            <strong>{experience.today.reportStatusToday ?? 'Нет отчёта'}</strong>
          </div>
        </div>
        <div className="recommendation-box">
          <strong>Рекомендация</strong>
          <p>{experience.aiRecommendation}</p>
        </div>
      </section>

      <section className="card">
        <h3>Обязательство и награда</h3>
        {experience.commitment ? (
          <div className="metric-grid compact-grid">
            <div className="metric-card">
              <span>Статус</span>
              <strong>{experience.commitment.status}</strong>
            </div>
            <div className="metric-card">
              <span>Выполненные дни</span>
              <strong>{experience.commitment.completedDays}</strong>
            </div>
            <div className="metric-card">
              <span>Пропущенные дни</span>
              <strong>{experience.commitment.missedDays}</strong>
            </div>
            <div className="metric-card">
              <span>Лучшая серия</span>
              <strong>{experience.commitment.bestStreak}</strong>
            </div>
            <div className="metric-card">
              <span>Личная награда</span>
              <strong>{experience.commitment.personalRewardTitle || 'Не задана'}</strong>
            </div>
            <div className="metric-card">
              <span>Награда</span>
              <strong>{experience.commitment.rewardUnlocked ? 'Разблокирована' : 'Заблокирована'}</strong>
            </div>
          </div>
        ) : (
          <p className="muted">Для цели ещё не создано ежедневное обязательство.</p>
        )}
      </section>

      <section className="card">
        <h3>Forecast</h3>
        <p>{experience.forecast.explanation}</p>
        <div className="chips">
          <span className="chip">Общий фокус: {minutesToHours(experience.forecast.totalFocusMinutes)}</span>
          <span className="chip">Среднее в день: {experience.forecast.averageDailyMinutes} мин.</span>
          <span className="chip">Осталось: {experience.forecast.remainingMinutes ?? 0} мин.</span>
          <span className="chip">Прогноз: {experience.forecast.probabilityLabel}</span>
        </div>
      </section>

      <section className="card">
        <h3>История цели</h3>
        <ul className="timeline-list">
          {experience.recentEvents.map((event) => (
            <li key={event.id}>
              <strong>{event.title}</strong>
              <p>{event.description || 'Без описания.'}</p>
              {(event.oldValue || event.newValue) ? (
                <small className="muted">
                  {event.oldValue ?? '—'} → {event.newValue ?? '—'}
                </small>
              ) : null}
              <time>{shortDateTime(event.createdAt)}</time>
            </li>
          ))}
        </ul>
      </section>

      <section className="card">
        <h3>Отчёты</h3>
        <div className="report-list">
          {reports.map((report) => (
            <article key={report.id} className="report-card">
              <img src={resolveAssetUrl(report.imagePath)} alt={`Отчёт ${report.reportDate}`} className="report-preview" />
              <div className="stack">
                <strong>{report.status}</strong>
                <small className="muted">{shortDateTime(report.createdAt)}</small>
                <span>AI verdict: {report.aiVerdict ?? 'Нет verdict'}</span>
                <span>Уверенность: {report.aiConfidence != null ? `${Math.round(report.aiConfidence * 100)}%` : 'н/д'}</span>
                <p>{report.aiExplanation ?? 'Без пояснения.'}</p>
              </div>
            </article>
          ))}
        </div>
      </section>

      <section className="card">
        <h3>Фокус-сессии</h3>
        <p className="muted">Всего завершённых сессий: {sessions.filter((session) => session.durationMinutes != null).length}</p>
        <strong>{minutesToHours(totalFocusMinutes)}</strong>
      </section>
    </div>
  );
}
