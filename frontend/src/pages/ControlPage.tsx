import { type CSSProperties, type FormEvent, useEffect, useState } from 'react';
import { api } from '../lib/apiClient';
import { useAppShellContext } from '../lib/useAppShellContext';
import type { GoalProgress } from '../types/api';
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
  const [progress, setProgress] = useState<GoalProgress | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const createColorStyle = { '--picker-color': createGoalThemeColor } as CSSProperties;
  const editColorStyle = { '--picker-color': goalForm.themeColor } as CSSProperties;

  useEffect(() => {
    if (!selectedGoal) {
      setProgress(null);
      return;
    }

    const run = async () => {
      setLoading(true);
      setError(null);
      try {
        const progressData = await api.getProgress(selectedGoal.id);
        setProgress(progressData);
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
              style={createColorStyle}
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
                  style={editColorStyle}
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
        </>
      )}

      {loading ? <section className="card">Обновление данных...</section> : null}
      {error ? <section className="card error-card">{error}</section> : null}
      {selectedGoalId === null ? <section className="card">Выберите цель для управления данными.</section> : null}
    </div>
  );
}
