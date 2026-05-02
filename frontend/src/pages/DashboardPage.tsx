import { type FormEvent, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../lib/apiClient';
import { shortDate } from '../lib/format';
import { useAppShellContext } from '../lib/useAppShellContext';
import type { GoalExperience, RiskStatus } from '../types/api';

interface CommitmentFormState {
  dailyTargetMinutes: string;
  startDate: string;
  endDate: string;
  personalRewardTitle: string;
  personalRewardDescription: string;
}

function createDefaultCommitmentForm(): CommitmentFormState {
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

function riskLabel(risk: RiskStatus | null): string {
  switch (risk) {
    case 'LOW':
      return 'Низкий';
    case 'MEDIUM':
      return 'Средний';
    case 'HIGH':
      return 'Высокий';
    default:
      return 'Не рассчитан';
  }
}

function forecastLabel(probabilityLabel: string): string {
  switch (probabilityLabel) {
    case 'HIGH':
      return 'Прогноз высокий';
    case 'MEDIUM':
      return 'Прогноз умеренный';
    case 'LOW':
      return 'Прогноз низкий';
    default:
      return 'Прогноз уточняется';
  }
}

export function DashboardPage() {
  const { goals, selectedGoalId, setSelectedGoalId, reloadGoals } = useAppShellContext();
  const [experiences, setExperiences] = useState<GoalExperience[]>([]);
  const [forms, setForms] = useState<Record<number, CommitmentFormState>>({});
  const [loading, setLoading] = useState(false);
  const [savingGoalId, setSavingGoalId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const sortedExperiences = useMemo(() => {
    return [...experiences].sort((left, right) => {
      if (left.goal.id === selectedGoalId) {
        return -1;
      }
      if (right.goal.id === selectedGoalId) {
        return 1;
      }
      return 0;
    });
  }, [experiences, selectedGoalId]);

  useEffect(() => {
    if (goals.length === 0) {
      setExperiences([]);
      return;
    }

    const run = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await api.getDashboardExperience();
        setExperiences(data);
        setForms((prev) => {
          const next = { ...prev };
          for (const item of data) {
            if (!next[item.goal.id]) {
              next[item.goal.id] = createDefaultCommitmentForm();
            }
          }
          return next;
        });
      } catch (err) {
        setError((err as Error).message);
      } finally {
        setLoading(false);
      }
    };

    void run();
  }, [goals]);

  function updateForm(goalId: number, patch: Partial<CommitmentFormState>) {
    setForms((prev) => ({
      ...prev,
      [goalId]: {
        ...(prev[goalId] ?? createDefaultCommitmentForm()),
        ...patch
      }
    }));
  }

  async function submitCommitment(goalId: number, event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = forms[goalId] ?? createDefaultCommitmentForm();
    setSavingGoalId(goalId);
    setError(null);
    try {
      await api.createCommitment(goalId, {
        dailyTargetMinutes: Number(form.dailyTargetMinutes),
        startDate: form.startDate,
        endDate: form.endDate || undefined,
        personalRewardTitle: form.personalRewardTitle || undefined,
        personalRewardDescription: form.personalRewardDescription || undefined
      });
      const data = await api.getDashboardExperience();
      setExperiences(data);
      await reloadGoals();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setSavingGoalId(null);
    }
  }

  if (goals.length === 0) {
    return (
      <section className="empty-state card">
        <h2>Нет активных целей</h2>
        <p>Создайте первую цель в разделе «Контроль», и dashboard начнет собирать единый сценарий дня.</p>
      </section>
    );
  }

  return (
    <div className="page-grid">
      <section className="card page-intro">
        <h2>Что нужно сделать сегодня</h2>
        <p>
          Dashboard объединяет обязательство, Pomodoro, фото-отчет, streak, дисциплину, риск,
          прогноз и мотивацию вокруг каждой вашей цели.
        </p>
      </section>

      {loading ? <section className="card">Собираем статус целей...</section> : null}
      {error ? <section className="card error-card">{error}</section> : null}

      <section className="goal-experience-list">
        {sortedExperiences.map((experience) => {
          const form = forms[experience.goal.id] ?? createDefaultCommitmentForm();
          const isSelected = experience.goal.id === selectedGoalId;
          return (
            <article key={experience.goal.id} className={`card experience-card ${isSelected ? 'experience-card-active' : ''}`}>
              <div className="card-header">
                <div>
                  <h3>{experience.goal.title}</h3>
                  <p className="muted">{experience.goal.description || 'Добавьте описание, чтобы AI давал точнее рекомендации.'}</p>
                </div>
                <div className="inline-actions">
                  <button className="btn btn-ghost" type="button" onClick={() => setSelectedGoalId(experience.goal.id)}>
                    {isSelected ? 'Активная цель' : 'Сделать активной'}
                  </button>
                  <Link className="btn" to={`/goals/${experience.goal.id}`}>
                    Открыть цель
                  </Link>
                </div>
              </div>

              <div className="chips">
                <span className="chip">Создана: {shortDate(experience.goal.createdAt)}</span>
                {experience.goal.deadline ? <span className="chip">Дедлайн: {shortDate(experience.goal.deadline)}</span> : null}
                {experience.forecast.targetHours ? <span className="chip">Target: {experience.forecast.targetHours} ч.</span> : null}
                <span className={`status-badge risk-${(experience.today.riskStatus ?? 'LOW').toLowerCase()}`}>
                  Риск: {riskLabel(experience.today.riskStatus)}
                </span>
              </div>

              {experience.commitment ? (
                <div className="metric-grid compact-grid">
                  <div className="metric-card">
                    <span>Сегодня</span>
                    <strong>
                      {experience.today.completedFocusMinutesToday}/{experience.commitment.dailyTargetMinutes} мин.
                    </strong>
                  </div>
                  <div className="metric-card">
                    <span>Осталось до дневной нормы</span>
                    <strong>{experience.today.remainingMinutesToday ?? 0} мин.</strong>
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
                    <span>Личная награда</span>
                    <strong>
                      {experience.commitment.personalRewardTitle || 'Не задана'}
                      {experience.commitment.rewardUnlocked ? ' · разблокирована' : ' · заблокирована'}
                    </strong>
                  </div>
                  <div className="metric-card">
                    <span>Прогноз</span>
                    <strong>{forecastLabel(experience.forecast.probabilityLabel)}</strong>
                  </div>
                </div>
              ) : (
                <form className="stack commitment-form" onSubmit={(event) => void submitCommitment(experience.goal.id, event)}>
                  <h4>Настроить ежедневное обязательство</h4>
                  <p className="muted">Без обязательства система не сможет засчитывать день, считать дисциплину и строить прогноз.</p>
                  <div className="inline-fields commitment-inline">
                    <input
                      type="number"
                      min={1}
                      max={1440}
                      value={form.dailyTargetMinutes}
                      onChange={(event) => updateForm(experience.goal.id, { dailyTargetMinutes: event.target.value })}
                      placeholder="Минут в день"
                      required
                    />
                    <input
                      type="date"
                      value={form.startDate}
                      onChange={(event) => updateForm(experience.goal.id, { startDate: event.target.value })}
                      required
                    />
                    <input
                      type="date"
                      value={form.endDate}
                      onChange={(event) => updateForm(experience.goal.id, { endDate: event.target.value })}
                    />
                  </div>
                  <input
                    value={form.personalRewardTitle}
                    onChange={(event) => updateForm(experience.goal.id, { personalRewardTitle: event.target.value })}
                    placeholder="Личная награда"
                  />
                  <textarea
                    rows={3}
                    value={form.personalRewardDescription}
                    onChange={(event) => updateForm(experience.goal.id, { personalRewardDescription: event.target.value })}
                    placeholder="Что вы получите после выполнения обязательства"
                  />
                  <button className="btn" type="submit" disabled={savingGoalId === experience.goal.id}>
                    {savingGoalId === experience.goal.id ? 'Сохраняем...' : 'Создать обязательство'}
                  </button>
                </form>
              )}

              <div className="stack">
                <div className="quote-block">
                  <p>{experience.today.motivationalMessage}</p>
                  <footer>Сегодня</footer>
                </div>
                <div className="recommendation-box">
                  <strong>Рекомендация</strong>
                  <p>{experience.aiRecommendation}</p>
                </div>
                <div className="forecast-box">
                  <strong>Следующее действие</strong>
                  <p>{experience.today.nextRecommendedAction}</p>
                  <small className="muted">{experience.forecast.explanation}</small>
                </div>
              </div>
            </article>
          );
        })}
      </section>
    </div>
  );
}
