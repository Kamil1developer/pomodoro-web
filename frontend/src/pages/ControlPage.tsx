import { type CSSProperties, type FormEvent, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../lib/apiClient';
import { shortDate } from '../lib/format';
import { useAppShellContext } from '../lib/useAppShellContext';
import type { GoalExperience } from '../types/api';

interface GoalFormState {
  title: string;
  description: string;
  targetHours: string;
  deadline: string;
  themeColor: string;
}

interface CommitmentFormState {
  dailyTargetMinutes: string;
  startDate: string;
  endDate: string;
  personalRewardTitle: string;
  personalRewardDescription: string;
}

const emptyGoalForm: GoalFormState = {
  title: '',
  description: '',
  targetHours: '',
  deadline: '',
  themeColor: '#dff6e5'
};

function defaultCommitmentForm(): CommitmentFormState {
  const start = new Date();
  const end = new Date();
  end.setDate(end.getDate() + 30);
  return {
    dailyTargetMinutes: '60',
    startDate: start.toISOString().slice(0, 10),
    endDate: end.toISOString().slice(0, 10),
    personalRewardTitle: '',
    personalRewardDescription: ''
  };
}

export function ControlPage() {
  const { goals, selectedGoal, selectedGoalId, setSelectedGoalId, reloadGoals } = useAppShellContext();
  const [goalForm, setGoalForm] = useState<GoalFormState>(emptyGoalForm);
  const [createGoalThemeColor, setCreateGoalThemeColor] = useState('#dff6e5');
  const [commitmentForm, setCommitmentForm] = useState<CommitmentFormState>(defaultCommitmentForm());
  const [experience, setExperience] = useState<GoalExperience | null>(null);
  const [closingFailed, setClosingFailed] = useState(false);
  const [failureReason, setFailureReason] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const createColorStyle = { '--picker-color': createGoalThemeColor } as CSSProperties;
  const editColorStyle = { '--picker-color': goalForm.themeColor } as CSSProperties;

  const hasCommitment = Boolean(experience?.commitment);

  useEffect(() => {
    if (!selectedGoal) {
      setExperience(null);
      setGoalForm(emptyGoalForm);
      setCommitmentForm(defaultCommitmentForm());
      return;
    }

    const run = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await api.getGoalExperience(selectedGoal.id);
        setExperience(data);
        setGoalForm({
          title: selectedGoal.title,
          description: selectedGoal.description ?? '',
          targetHours: selectedGoal.targetHours ? String(selectedGoal.targetHours) : '',
          deadline: selectedGoal.deadline ?? '',
          themeColor: selectedGoal.themeColor ?? '#dff6e5'
        });
        setCommitmentForm(
          data.commitment
            ? {
                dailyTargetMinutes: String(data.commitment.dailyTargetMinutes),
                startDate: data.commitment.startDate,
                endDate: data.commitment.endDate ?? '',
                personalRewardTitle: data.commitment.personalRewardTitle ?? '',
                personalRewardDescription: data.commitment.personalRewardDescription ?? ''
              }
            : defaultCommitmentForm()
        );
      } catch (err) {
        setError((err as Error).message);
      } finally {
        setLoading(false);
      }
    };

    void run();
  }, [selectedGoal]);

  const recentEvents = useMemo(() => experience?.recentEvents.slice(0, 6) ?? [], [experience]);

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
      const refreshed = await api.getGoalExperience(selectedGoal.id);
      setExperience(refreshed);
    } catch (err) {
      setError((err as Error).message);
    }
  }

  async function handleCloseFailedGoal() {
    if (!selectedGoal) {
      return;
    }
    if (!failureReason.trim()) {
      setError('Укажите причину, почему цель не выполнена.');
      return;
    }

    try {
      await api.closeFailedGoal(selectedGoal.id, failureReason.trim());
      await reloadGoals();
      const nextId = goals.filter((goal) => goal.id !== selectedGoal.id)[0]?.id ?? null;
      setSelectedGoalId(nextId);
      setClosingFailed(false);
      setFailureReason('');
    } catch (err) {
      setError((err as Error).message);
    }
  }

  async function handleCreateCommitment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedGoal) {
      return;
    }

    try {
      await api.createCommitment(selectedGoal.id, {
        dailyTargetMinutes: Number(commitmentForm.dailyTargetMinutes),
        startDate: commitmentForm.startDate,
        endDate: commitmentForm.endDate || undefined,
        personalRewardTitle: commitmentForm.personalRewardTitle || undefined,
        personalRewardDescription: commitmentForm.personalRewardDescription || undefined
      });
      const refreshed = await api.getGoalExperience(selectedGoal.id);
      setExperience(refreshed);
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
                <button className="btn" type="button" onClick={() => void handleUpdateGoal()}>
                  Сохранить цель
                </button>
                <button className="btn btn-danger" type="button" onClick={() => setClosingFailed(true)}>
                  Закрыть как невыполненную
                </button>
              </div>
            </div>
          </section>

          <section className="card">
            <div className="card-header">
              <h3>Ежедневное обязательство</h3>
              <Link className="btn btn-ghost" to={`/goals/${selectedGoal.id}`}>
                Полная история цели
              </Link>
            </div>
            {hasCommitment && experience?.commitment ? (
              <div className="stack">
                <div className="metric-grid compact-grid">
                  <div className="metric-card">
                    <span>Норма</span>
                    <strong>{experience.commitment.dailyTargetMinutes} мин/день</strong>
                  </div>
                  <div className="metric-card">
                    <span>Серия</span>
                    <strong>{experience.commitment.currentStreak} дн.</strong>
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
                    <span>Награда</span>
                    <strong>
                      {experience.commitment.personalRewardTitle || 'Не задана'}
                      {experience.commitment.rewardUnlocked ? ' · разблокирована' : ' · заблокирована'}
                    </strong>
                  </div>
                  <div className="metric-card">
                    <span>Период</span>
                    <strong>
                      {shortDate(experience.commitment.startDate)}
                      {experience.commitment.endDate ? ` — ${shortDate(experience.commitment.endDate)}` : ''}
                    </strong>
                  </div>
                </div>
                <p className="muted">{experience.today.nextRecommendedAction}</p>
              </div>
            ) : (
              <form className="stack" onSubmit={handleCreateCommitment}>
                <p className="muted">Создайте обязательство, чтобы цель стала центром daily flow: фокус → отчёт → streak → прогноз.</p>
                <div className="inline-fields commitment-inline">
                  <input
                    type="number"
                    min={1}
                    max={1440}
                    value={commitmentForm.dailyTargetMinutes}
                    onChange={(event) => setCommitmentForm((prev) => ({ ...prev, dailyTargetMinutes: event.target.value }))}
                    placeholder="Минут в день"
                    required
                  />
                  <input
                    type="date"
                    value={commitmentForm.startDate}
                    onChange={(event) => setCommitmentForm((prev) => ({ ...prev, startDate: event.target.value }))}
                    required
                  />
                  <input
                    type="date"
                    value={commitmentForm.endDate}
                    onChange={(event) => setCommitmentForm((prev) => ({ ...prev, endDate: event.target.value }))}
                  />
                </div>
                <input
                  value={commitmentForm.personalRewardTitle}
                  onChange={(event) => setCommitmentForm((prev) => ({ ...prev, personalRewardTitle: event.target.value }))}
                  placeholder="Личная награда"
                />
                <textarea
                  value={commitmentForm.personalRewardDescription}
                  onChange={(event) => setCommitmentForm((prev) => ({ ...prev, personalRewardDescription: event.target.value }))}
                  rows={3}
                  placeholder="Опишите награду после завершения обязательства"
                />
                <button className="btn" type="submit">
                  Создать обязательство
                </button>
              </form>
            )}
          </section>

          <section className="card">
            <h3>История цели</h3>
            {recentEvents.length === 0 ? (
              <p className="muted">События появятся после запуска фокус-сессий, отчётов и закрытия дня scheduler-ом.</p>
            ) : (
              <ul className="timeline-list">
                {recentEvents.map((event) => (
                  <li key={event.id}>
                    <strong>{event.title}</strong>
                    <p>{event.description || 'Без дополнительного описания.'}</p>
                    <time>{shortDate(event.createdAt)}</time>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </>
      )}

      {loading ? <section className="card">Обновление данных...</section> : null}
      {error ? <section className="card error-card">{error}</section> : null}
      {closingFailed && selectedGoal ? (
        <section className="card report-modal" role="dialog" aria-modal="true">
          <h3>Почему цель не выполнена?</h3>
          <p className="muted">
            Цель «{selectedGoal.title}» попадёт в историю профиля как невыполненная и больше не пропадёт бесследно.
          </p>
          <textarea
            rows={4}
            value={failureReason}
            onChange={(event) => setFailureReason(event.target.value)}
            placeholder="Например: не хватило времени, потерял интерес, сменился приоритет"
          />
          <div className="inline-actions">
            <button className="btn btn-danger" type="button" onClick={() => void handleCloseFailedGoal()}>
              Закрыть цель
            </button>
            <button
              className="btn btn-ghost"
              type="button"
              onClick={() => {
                setClosingFailed(false);
                setFailureReason('');
              }}>
              Отмена
            </button>
          </div>
        </section>
      ) : null}
      {selectedGoalId === null ? <section className="card">Выберите цель для управления её параметрами.</section> : null}
    </div>
  );
}
