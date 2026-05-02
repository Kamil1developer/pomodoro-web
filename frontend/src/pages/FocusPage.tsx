import { type FormEvent, useEffect, useMemo, useState } from 'react';
import { api, resolveAssetUrl } from '../lib/apiClient';
import { minutesToHours, shortDateTime } from '../lib/format';
import { useAppShellContext } from '../lib/useAppShellContext';
import type { FocusSession, GoalExperience, ReportItem, TaskItem } from '../types/api';

function formatElapsed(totalSeconds: number): string {
  const safe = Math.max(0, totalSeconds);
  const hours = String(Math.floor(safe / 3600)).padStart(2, '0');
  const minutes = String(Math.floor((safe % 3600) / 60)).padStart(2, '0');
  const seconds = String(safe % 60).padStart(2, '0');
  return `${hours}:${minutes}:${seconds}`;
}

function reportImpactText(report: ReportItem): string {
  switch (report.status) {
    case 'CONFIRMED':
      return 'Подтверждённый отчёт помогает засчитать день.';
    case 'REJECTED':
      return 'Отклонённый отчёт не засчитывает день, пока вы не исправите подтверждение.';
    case 'PENDING':
      return 'AI просит больше данных. День ещё не подтверждён.';
    case 'OVERDUE':
      return 'Просроченный отчёт больше не может подтвердить прошлый день.';
    default:
      return '';
  }
}

export function FocusPage() {
  const { selectedGoal } = useAppShellContext();
  const [taskTitle, setTaskTitle] = useState('');
  const [reportComment, setReportComment] = useState('');
  const [reportFile, setReportFile] = useState<File | null>(null);
  const [tasks, setTasks] = useState<TaskItem[]>([]);
  const [sessions, setSessions] = useState<FocusSession[]>([]);
  const [reports, setReports] = useState<ReportItem[]>([]);
  const [experience, setExperience] = useState<GoalExperience | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [nowTs, setNowTs] = useState(() => Date.now());

  const activeSession = useMemo(
    () => sessions.find((session) => session.endedAt === null) ?? null,
    [sessions]
  );
  const todayTasksCount = useMemo(() => {
    const now = new Date();
    return tasks.filter((task) => {
      const createdAt = new Date(task.createdAt);
      return (
        createdAt.getFullYear() === now.getFullYear() &&
        createdAt.getMonth() === now.getMonth() &&
        createdAt.getDate() === now.getDate()
      );
    }).length;
  }, [tasks]);
  const hasTodayTasks = todayTasksCount > 0;

  useEffect(() => {
    if (!activeSession) {
      return;
    }
    const timerId = window.setInterval(() => setNowTs(Date.now()), 1000);
    return () => window.clearInterval(timerId);
  }, [activeSession]);

  useEffect(() => {
    if (!selectedGoal) {
      setTasks([]);
      setSessions([]);
      setReports([]);
      setExperience(null);
      return;
    }

    void refreshGoalData(selectedGoal.id);
  }, [selectedGoal]);

  async function refreshGoalData(goalId: number) {
    setLoading(true);
    setError(null);
    try {
      const [experienceData, tasksData, sessionsData, reportsData] = await Promise.all([
        api.getGoalExperience(goalId),
        api.getTasks(goalId),
        api.getFocusSessions(goalId),
        api.getReports(goalId)
      ]);
      setExperience(experienceData);
      setTasks(tasksData);
      setSessions(sessionsData);
      setReports(reportsData);
      setNowTs(Date.now());
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  }

  async function handleCreateTask(event: FormEvent) {
    event.preventDefault();
    if (!selectedGoal || !taskTitle.trim()) {
      return;
    }

    try {
      await api.createTask(selectedGoal.id, { title: taskTitle.trim() });
      setTaskTitle('');
      await refreshGoalData(selectedGoal.id);
    } catch (err) {
      setError((err as Error).message);
    }
  }

  async function toggleTask(task: TaskItem) {
    if (!selectedGoal) {
      return;
    }

    try {
      await api.updateTask(selectedGoal.id, task.id, {
        title: task.title,
        isDone: !task.isDone
      });
      await refreshGoalData(selectedGoal.id);
    } catch (err) {
      setError((err as Error).message);
    }
  }

  async function deleteTask(task: TaskItem) {
    if (!selectedGoal) {
      return;
    }

    try {
      await api.deleteTask(selectedGoal.id, task.id);
      await refreshGoalData(selectedGoal.id);
    } catch (err) {
      setError((err as Error).message);
    }
  }

  async function startFocus() {
    if (!selectedGoal || !hasTodayTasks) {
      return;
    }

    try {
      await api.startFocus(selectedGoal.id);
      await refreshGoalData(selectedGoal.id);
    } catch (err) {
      setError((err as Error).message);
    }
  }

  async function stopFocus() {
    if (!selectedGoal) {
      return;
    }

    try {
      await api.stopFocus(selectedGoal.id);
      await refreshGoalData(selectedGoal.id);
    } catch (err) {
      setError((err as Error).message);
    }
  }

  async function submitReport(event: FormEvent) {
    event.preventDefault();
    if (!selectedGoal || !reportFile || !hasTodayTasks) {
      return;
    }

    try {
      await api.uploadReport(selectedGoal.id, reportFile, reportComment);
      setReportFile(null);
      setReportComment('');
      await refreshGoalData(selectedGoal.id);
    } catch (err) {
      setError((err as Error).message);
    }
  }

  const activeElapsedSeconds = activeSession
    ? Math.floor((nowTs - new Date(activeSession.startedAt).getTime()) / 1000)
    : 0;
  const totalFocusMinutes = sessions.reduce((sum, session) => sum + (session.durationMinutes ?? 0), 0);

  if (!selectedGoal) {
    return (
      <section className="empty-state">
        <h2>Нет активной цели</h2>
        <p>Выберите цель в верхнем списке, чтобы работать с Pomodoro, задачами и AI-проверкой.</p>
      </section>
    );
  }

  return (
    <div className="page-grid control-grid">
      <section className="card focus-hero-card">
        <div className="card-header">
          <h3>Сегодня</h3>
          {experience?.today.riskStatus ? (
            <span className={`status-badge risk-${experience.today.riskStatus.toLowerCase()}`}>
              Риск: {experience.today.riskStatus}
            </span>
          ) : null}
        </div>
        <div className="metric-grid compact-grid">
          <div className="metric-card">
            <span>Дневная норма</span>
            <strong>{experience?.today.dailyTargetMinutes ?? 0} мин.</strong>
          </div>
          <div className="metric-card">
            <span>Выполнено сегодня</span>
            <strong>{experience?.today.completedFocusMinutesToday ?? 0} мин.</strong>
          </div>
          <div className="metric-card">
            <span>Осталось до дневной нормы</span>
            <strong>{experience?.today.remainingMinutesToday ?? 0} мин.</strong>
          </div>
          <div className="metric-card">
            <span>Серия</span>
            <strong>{experience?.today.currentStreak ?? 0} дн.</strong>
          </div>
          <div className="metric-card">
            <span>Дисциплина</span>
            <strong>{experience?.today.disciplineScore ?? 0}/100</strong>
          </div>
          <div className="metric-card">
            <span>Статус отчёта</span>
            <strong>{experience?.today.reportStatusToday ?? 'Нет отчёта'}</strong>
          </div>
        </div>
        <p className="muted">{experience?.today.motivationalMessage}</p>
        <div className="recommendation-box">
          <strong>Рекомендация</strong>
          <p>{experience?.aiRecommendation ?? 'Система подскажет следующий шаг после настройки обязательства.'}</p>
        </div>
      </section>

      <section className="card">
        <div className="card-header">
          <h3>Pomodoro</h3>
          <strong>{minutesToHours(totalFocusMinutes)}</strong>
        </div>
        {!hasTodayTasks ? (
          <p className="warning-note">
            На сегодня нет задач. Сначала добавьте хотя бы одну задачу дня, затем запускайте фокус-сессию.
          </p>
        ) : null}
        <p className="focus-timer">{activeSession ? formatElapsed(activeElapsedSeconds) : '00:00:00'}</p>
        <div className="inline-actions">
          <button className="btn" onClick={() => void startFocus()} disabled={Boolean(activeSession) || !hasTodayTasks}>
            Старт
          </button>
          <button className="btn" onClick={() => void stopFocus()} disabled={!activeSession}>
            Стоп
          </button>
        </div>
        {activeSession ? (
          <p className="muted">Активна с {shortDateTime(activeSession.startedAt)}</p>
        ) : (
          <p className="muted">Нет активной сессии</p>
        )}
        {experience?.today.isDailyTargetReached && !experience.today.hasApprovedReportToday ? (
          <p className="warning-note">
            Норма по времени выполнена. Теперь отправьте фото-отчёт, чтобы день был засчитан.
          </p>
        ) : null}
      </section>

      <section className="card">
        <div className="card-header">
          <h3>Задачи на сегодня</h3>
          <strong>{tasks.filter((task) => task.isDone).length}/{tasks.length}</strong>
        </div>
        <p className="muted">Задач создано сегодня: {todayTasksCount}</p>
        <form className="inline-fields" onSubmit={handleCreateTask}>
          <input
            placeholder="Новая задача"
            value={taskTitle}
            onChange={(event) => setTaskTitle(event.target.value)}
            required
          />
          <button className="btn" type="submit">
            Добавить
          </button>
        </form>
        <ul className="feed task-feed">
          {tasks.map((task) => (
            <li key={task.id}>
              <label className="task-item">
                <input type="checkbox" checked={task.isDone} onChange={() => void toggleTask(task)} />
                <span>{task.title}</span>
              </label>
              <button className="btn btn-ghost" onClick={() => void deleteTask(task)}>
                Удалить
              </button>
            </li>
          ))}
        </ul>
      </section>

      <section className="card">
        <h3>AI-проверка отчёта</h3>
        {!hasTodayTasks ? (
          <p className="warning-note">
            Без задач на сегодня AI-проверка недоступна. Добавьте задачу дня и отправьте отчёт снова.
          </p>
        ) : null}
        <form className="stack" onSubmit={submitReport}>
          <input
            type="file"
            accept="image/*"
            onChange={(event) => setReportFile(event.target.files?.[0] ?? null)}
            required
          />
          <textarea
            value={reportComment}
            onChange={(event) => setReportComment(event.target.value)}
            placeholder="Комментарий к отчёту"
            rows={3}
          />
          <button className="btn" type="submit" disabled={!reportFile || !hasTodayTasks}>
            Отправить на AI-проверку
          </button>
        </form>

        <div className="report-list">
          {reports.map((report) => (
            <article key={report.id} className="report-card">
              <img src={resolveAssetUrl(report.imagePath)} alt={`Отчёт ${report.reportDate}`} className="report-preview" />
              <div className="stack">
                <strong>
                  {report.status} · {report.aiVerdict ?? 'Без verdict'}
                </strong>
                <small className="muted">{shortDateTime(report.createdAt)}</small>
                <span>Уверенность AI: {report.aiConfidence != null ? `${Math.round(report.aiConfidence * 100)}%` : 'н/д'}</span>
                <p>{report.aiExplanation ?? 'Без пояснения AI.'}</p>
                <small className="muted">{reportImpactText(report)}</small>
              </div>
            </article>
          ))}
        </div>
      </section>

      {loading ? <section className="card">Обновление данных...</section> : null}
      {error ? <section className="card error-card">{error}</section> : null}
    </div>
  );
}
