import { type FormEvent, useEffect, useMemo, useState } from 'react';
import { api, resolveAssetUrl } from '../lib/apiClient';
import { minutesToHours, shortDateTime } from '../lib/format';
import { useAppShellContext } from '../lib/useAppShellContext';
import type { FocusSession, GoalProgress, ReportItem, TaskItem } from '../types/api';
import { ProgressCard } from '../components/ProgressCard';

interface GoalFormState {
  title: string;
  description: string;
  targetHours: string;
  deadline: string;
  themeColor: string;
}

const emptyGoalForm: GoalFormState = {
  title: '',
  description: '',
  targetHours: '',
  deadline: '',
  themeColor: '#dff6e5'
};

export function ControlPage() {
  const { goals, selectedGoal, selectedGoalId, setSelectedGoalId, reloadGoals } = useAppShellContext();
  const [goalForm, setGoalForm] = useState<GoalFormState>(emptyGoalForm);
  const [createGoalThemeColor, setCreateGoalThemeColor] = useState('#dff6e5');
  const [taskTitle, setTaskTitle] = useState('');
  const [reportComment, setReportComment] = useState('');
  const [reportFile, setReportFile] = useState<File | null>(null);
  const [tasks, setTasks] = useState<TaskItem[]>([]);
  const [progress, setProgress] = useState<GoalProgress | null>(null);
  const [sessions, setSessions] = useState<FocusSession[]>([]);
  const [reports, setReports] = useState<ReportItem[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const activeSession = useMemo(
    () => sessions.find((session) => session.endedAt === null) ?? null,
    [sessions]
  );

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
        setGoalForm({
          title: selectedGoal.title,
          description: selectedGoal.description ?? '',
          targetHours: selectedGoal.targetHours ? String(selectedGoal.targetHours) : '',
          deadline: selectedGoal.deadline ?? '',
          themeColor: selectedGoal.themeColor ?? '#dff6e5'
        });
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
  }

  async function handleCreateGoal(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const title = String(formData.get('title') ?? '').trim();
    if (!title) {
      return;
    }

    try {
      const created = await api.createGoal({
        title,
        description: String(formData.get('description') ?? '').trim() || undefined,
        targetHours: Number(formData.get('targetHours') || 0) || undefined,
        deadline: String(formData.get('deadline') ?? '').trim() || undefined,
        themeColor: createGoalThemeColor || '#dff6e5'
      });
      await reloadGoals();
      setSelectedGoalId(created.id);
      event.currentTarget.reset();
      setCreateGoalThemeColor('#dff6e5');
    } catch (err) {
      setError((err as Error).message);
    }
  }

  async function handleUpdateGoal() {
    if (!selectedGoal) {
      return;
    }

    try {
      await api.updateGoal(selectedGoal.id, {
        title: goalForm.title,
        description: goalForm.description || undefined,
        targetHours: Number(goalForm.targetHours) || undefined,
        deadline: goalForm.deadline || undefined,
        themeColor: goalForm.themeColor || '#dff6e5'
      });
      await reloadGoals();
      await refreshGoalData();
    } catch (err) {
      setError((err as Error).message);
    }
  }

  async function handleDeleteGoal() {
    if (!selectedGoal) {
      return;
    }

    try {
      await api.deleteGoal(selectedGoal.id);
      await reloadGoals();
      const nextId = goals.filter((goal) => goal.id !== selectedGoal.id)[0]?.id ?? null;
      setSelectedGoalId(nextId);
    } catch (err) {
      setError((err as Error).message);
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
    if (!selectedGoal) {
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
    if (!selectedGoal || !reportFile) {
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

  return (
    <div className="page-grid control-grid">
      <section className="card">
        <h3>Создать цель</h3>
        <form className="stack" onSubmit={handleCreateGoal}>
          <input name="title" placeholder="Название цели" required />
          <textarea name="description" placeholder="Описание" rows={3} />
          <div className="inline-fields">
            <input name="targetHours" type="number" min={0} placeholder="Target hours" />
            <input name="deadline" type="date" />
          </div>
          <label>
            <span>Цвет цели</span>
            <input
              className="goal-color-input"
              name="themeColor"
              type="color"
              value={createGoalThemeColor}
              onChange={(event) => setCreateGoalThemeColor(event.target.value)}
            />
          </label>
          <button className="btn" type="submit">
            Добавить цель
          </button>
        </form>
      </section>

      {!selectedGoal ? (
        <section className="card empty-state">
          <h3>Выберите цель</h3>
          <p>Выберите цель в верхнем списке или создайте новую.</p>
        </section>
      ) : (
        <>
          <section className="card">
            <h3>Редактирование цели</h3>
            <div className="stack">
              <input
                value={goalForm.title}
                onChange={(event) => setGoalForm((prev) => ({ ...prev, title: event.target.value }))}
                placeholder="Название"
              />
              <textarea
                value={goalForm.description}
                onChange={(event) => setGoalForm((prev) => ({ ...prev, description: event.target.value }))}
                rows={3}
                placeholder="Описание"
              />
              <div className="inline-fields">
                <input
                  type="number"
                  min={0}
                  value={goalForm.targetHours}
                  onChange={(event) => setGoalForm((prev) => ({ ...prev, targetHours: event.target.value }))}
                  placeholder="Target hours"
                />
                <input
                  type="date"
                  value={goalForm.deadline}
                  onChange={(event) => setGoalForm((prev) => ({ ...prev, deadline: event.target.value }))}
                />
              </div>
              <label>
                <span>Цвет цели</span>
                <input
                  className="goal-color-input"
                  type="color"
                  value={goalForm.themeColor}
                  onChange={(event) => setGoalForm((prev) => ({ ...prev, themeColor: event.target.value }))}
                />
              </label>
              <div className="inline-actions">
                <button className="btn" onClick={() => void handleUpdateGoal()}>
                  Сохранить цель
                </button>
                <button className="btn btn-danger" onClick={() => void handleDeleteGoal()}>
                  Удалить
                </button>
              </div>
            </div>
          </section>

          {progress ? <ProgressCard progress={progress} /> : <section className="card">Загрузка прогресса...</section>}

          <section className="card">
            <div className="card-header">
              <h3>Задачи</h3>
              <strong>{tasks.filter((task) => task.isDone).length}/{tasks.length}</strong>
            </div>
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
                    <input
                      type="checkbox"
                      checked={task.isDone}
                      onChange={() => void toggleTask(task)}
                    />
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
            <div className="card-header">
              <h3>Фокус-сессии</h3>
              <strong>{minutesToHours(sessions.reduce((sum, s) => sum + (s.durationMinutes ?? 0), 0))}</strong>
            </div>
            <div className="inline-actions">
              <button className="btn" onClick={() => void startFocus()} disabled={Boolean(activeSession)}>
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
            <h3>Отправить фото-отчет</h3>
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
              <button className="btn" type="submit" disabled={!reportFile}>
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
        </>
      )}

      {loading ? <section className="card">Обновление данных...</section> : null}
      {error ? <section className="card error-card">{error}</section> : null}
      {selectedGoalId === null ? <section className="card">Выберите цель для управления данными.</section> : null}
    </div>
  );
}
