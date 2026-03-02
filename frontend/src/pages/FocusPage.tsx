import { type FormEvent, useEffect, useMemo, useState } from 'react';
import { api, resolveAssetUrl } from '../lib/apiClient';
import { minutesToHours, shortDateTime } from '../lib/format';
import { useAppShellContext } from '../lib/useAppShellContext';
import type { FocusSession, GoalProgress, ReportItem, TaskItem } from '../types/api';
import { ProgressCard } from '../components/ProgressCard';

function formatElapsed(totalSeconds: number): string {
  const safe = Math.max(0, totalSeconds);
  const hours = String(Math.floor(safe / 3600)).padStart(2, '0');
  const minutes = String(Math.floor((safe % 3600) / 60)).padStart(2, '0');
  const seconds = String(safe % 60).padStart(2, '0');
  return `${hours}:${minutes}:${seconds}`;
}

export function FocusPage() {
  const { selectedGoal } = useAppShellContext();
  const [taskTitle, setTaskTitle] = useState('');
  const [reportComment, setReportComment] = useState('');
  const [reportFile, setReportFile] = useState<File | null>(null);
  const [tasks, setTasks] = useState<TaskItem[]>([]);
  const [progress, setProgress] = useState<GoalProgress | null>(null);
  const [sessions, setSessions] = useState<FocusSession[]>([]);
  const [reports, setReports] = useState<ReportItem[]>([]);
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
      setProgress(null);
      setSessions([]);
      setReports([]);
      return;
    }

    const run = async () => {
      setLoading(true);
      setError(null);
      try {
        const [tasksData, progressData, sessionsData, reportsData] = await Promise.all([
          api.getTasks(selectedGoal.id),
          api.getProgress(selectedGoal.id),
          api.getFocusSessions(selectedGoal.id),
          api.getReports(selectedGoal.id)
        ]);
        setTasks(tasksData);
        setProgress(progressData);
        setSessions(sessionsData);
        setReports(reportsData);
        setNowTs(Date.now());
      } catch (err) {
        setError((err as Error).message);
      } finally {
        setLoading(false);
      }
    };

    void run();
  }, [selectedGoal]);

  async function refreshGoalData() {
    if (!selectedGoal) {
      return;
    }

    const [tasksData, progressData, sessionsData, reportsData] = await Promise.all([
      api.getTasks(selectedGoal.id),
      api.getProgress(selectedGoal.id),
      api.getFocusSessions(selectedGoal.id),
      api.getReports(selectedGoal.id)
    ]);

    setTasks(tasksData);
    setProgress(progressData);
    setSessions(sessionsData);
    setReports(reportsData);
    setNowTs(Date.now());
  }

  async function handleCreateTask(event: FormEvent) {
    event.preventDefault();
    if (!selectedGoal || !taskTitle.trim()) {
      return;
    }

    try {
      await api.createTask(selectedGoal.id, { title: taskTitle.trim() });
      setTaskTitle('');
      await refreshGoalData();
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
      await refreshGoalData();
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
      await refreshGoalData();
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
      await refreshGoalData();
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
      await refreshGoalData();
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
      await refreshGoalData();
    } catch (err) {
      setError((err as Error).message);
    }
  }

  const activeElapsedSeconds = activeSession
    ? Math.floor((nowTs - new Date(activeSession.startedAt).getTime()) / 1000)
    : 0;

  if (!selectedGoal) {
    return (
      <section className="empty-state">
        <h2>Нет активной цели</h2>
        <p>Выберите цель в верхнем списке, чтобы работать с фокусом и задачами.</p>
      </section>
    );
  }

  return (
    <div className="page-grid control-grid">
      {progress ? <ProgressCard progress={progress} /> : <section className="card">Загрузка прогресса...</section>}

      <section className="card">
        <div className="card-header">
          <h3>Фокус-сессия</h3>
          <strong>{minutesToHours(sessions.reduce((sum, s) => sum + (s.durationMinutes ?? 0), 0))}</strong>
        </div>
        {!hasTodayTasks ? (
          <p className="warning-note">
            На сегодня нет задач. Сначала добавьте хотя бы одну задачу дня, затем запускайте фокус-сессию.
          </p>
        ) : null}
        <p className="focus-timer">{activeSession ? formatElapsed(activeElapsedSeconds) : '00:00:00'}</p>
        <div className="inline-actions">
          <button
            className="btn"
            onClick={() => void startFocus()}
            disabled={Boolean(activeSession) || !hasTodayTasks}>
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
      </section>

      <section className="card">
        <div className="card-header">
          <h3>Задачи</h3>
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
        <h3>Проверка AI (фото-отчет)</h3>
        {!hasTodayTasks ? (
          <p className="warning-note">
            Без задач на сегодня AI-проверка недоступна. Добавьте задачу дня и отправьте отчет снова.
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
            placeholder="Комментарий к отчету"
            rows={3}
          />
          <button className="btn" type="submit" disabled={!reportFile || !hasTodayTasks}>
            Отправить на AI-проверку
          </button>
        </form>

        <ul className="feed">
          {reports.map((report) => (
            <li key={report.id}>
              <div>
                <p className="feed-title">
                  {report.status} · {report.aiVerdict ?? 'N/A'}
                </p>
                <p className="feed-subtitle">{report.aiExplanation ?? 'Без пояснения'}</p>
              </div>
              <a href={resolveAssetUrl(report.imagePath)} target="_blank" rel="noreferrer">
                Фото
              </a>
            </li>
          ))}
        </ul>
      </section>

      {loading ? <section className="card">Обновление данных...</section> : null}
      {error ? <section className="card error-card">{error}</section> : null}
    </div>
  );
}
