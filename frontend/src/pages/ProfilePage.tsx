import { type ChangeEvent, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, resolveAssetUrl } from '../lib/apiClient';
import { minutesToHours, shortDate, shortDateTime } from '../lib/format';
import type { ProfileGoalHistoryItem, ProfileResponse } from '../types/api';

function riskLabel(value: string | null): string {
  if (value === 'HIGH') {
    return 'Высокий';
  }
  if (value === 'MEDIUM') {
    return 'Средний';
  }
  if (value === 'LOW') {
    return 'Низкий';
  }
  return 'Не рассчитан';
}

function historyStatusLabel(item: ProfileGoalHistoryItem): string {
  if (item.status === 'COMPLETED') {
    return 'Цель завершена';
  }
  if (item.status === 'FAILED') {
    return 'Цель не выполнена';
  }
  return 'Архив';
}

export function ProfilePage() {
  const [profile, setProfile] = useState<ProfileResponse | null>(null);
  const [fullName, setFullName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);

  useEffect(() => {
    void loadProfile();
  }, []);

  async function loadProfile() {
    setLoading(true);
    setError(null);
    try {
      const data = await api.getProfile();
      setProfile(data);
      setFullName(data.fullName);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  }

  async function saveProfile() {
    setSaving(true);
    setError(null);
    setSuccess(null);
    try {
      const data = await api.updateProfile({ fullName: fullName.trim() || undefined });
      setProfile(data);
      setSuccess('Профиль сохранён.');
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setSaving(false);
    }
  }

  async function handleAvatarChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }
    setUploading(true);
    setError(null);
    setSuccess(null);
    try {
      const data = await api.uploadAvatar(file);
      setProfile(data);
      setSuccess('Аватар обновлён.');
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setUploading(false);
      event.target.value = '';
    }
  }

  const history = useMemo(() => profile?.goalHistory ?? [], [profile]);

  if (loading) {
    return <section className="card">Загрузка профиля...</section>;
  }

  if (!profile) {
    return <section className="card error-card">Не удалось загрузить профиль.</section>;
  }

  return (
    <div className="page-grid">
      <section className="card profile-header-card">
        <div className="profile-header-main">
          <div className="profile-avatar-wrap">
            {profile.avatarPath ? (
              <img className="profile-avatar" src={resolveAssetUrl(profile.avatarPath)} alt={profile.fullName} />
            ) : (
              <div className="profile-avatar profile-avatar-fallback" aria-hidden="true">
                {profile.fullName.slice(0, 1).toUpperCase()}
              </div>
            )}
          </div>
          <div className="profile-header-copy">
            <h2>Профиль</h2>
            <p className="muted">Email: {profile.email}</p>
            <div className="inline-fields profile-inline-fields">
              <input value={fullName} onChange={(event) => setFullName(event.target.value)} placeholder="Ваше имя" />
              <button className="btn" type="button" onClick={() => void saveProfile()} disabled={saving}>
                {saving ? 'Сохраняем...' : 'Сохранить'}
              </button>
            </div>
            <label className="btn btn-ghost profile-avatar-upload">
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp"
                onChange={(event) => void handleAvatarChange(event)}
                hidden
              />
              {uploading ? 'Загрузка аватара...' : 'Загрузить аватар'}
            </label>
          </div>
        </div>
      </section>

      <section className="card">
        <h3>Сводка пользователя</h3>
        <div className="metric-grid compact-grid">
          <div className="metric-card">
            <span>Активные цели</span>
            <strong>{profile.stats.activeGoalsCount}</strong>
          </div>
          <div className="metric-card">
            <span>Завершённые цели</span>
            <strong>{profile.stats.completedGoalsCount}</strong>
          </div>
          <div className="metric-card">
            <span>Невыполненные цели</span>
            <strong>{profile.stats.failedGoalsCount}</strong>
          </div>
          <div className="metric-card">
            <span>Общий фокус</span>
            <strong>{minutesToHours(profile.stats.totalFocusMinutes)}</strong>
          </div>
          <div className="metric-card">
            <span>Лучший streak</span>
            <strong>{profile.stats.bestStreak} дн.</strong>
          </div>
          <div className="metric-card">
            <span>Средняя дисциплина</span>
            <strong>{profile.stats.averageDiscipline != null ? `${profile.stats.averageDiscipline}/100` : 'н/д'}</strong>
          </div>
          <div className="metric-card">
            <span>Сводный риск</span>
            <strong>{riskLabel(profile.stats.riskSummary)}</strong>
          </div>
        </div>
      </section>

      <section className="card">
        <div className="card-header">
          <h3>Текущие цели</h3>
          <strong>{profile.activeGoals.length}</strong>
        </div>
        {profile.activeGoals.length === 0 ? (
          <p className="muted">Сейчас нет активных целей. Создайте новую цель в разделе «Контроль».</p>
        ) : (
          <div className="profile-goal-list">
            {profile.activeGoals.map((goal) => (
              <article key={goal.goalId} className="profile-goal-card">
                <div>
                  <strong>{goal.title}</strong>
                  <p className="muted">Создана {shortDateTime(goal.createdAt)}</p>
                </div>
                <div className="chips">
                  <span className="chip">Норма: {goal.dailyTargetMinutes ?? '—'} мин.</span>
                  <span className="chip">Выполнено сегодня: {goal.completedFocusMinutesToday} мин.</span>
                  <span className="chip">Осталось: {goal.remainingMinutesToday ?? 0} мин.</span>
                  <span className="chip">Серия: {goal.currentStreak} дн.</span>
                  <span className="chip">Дисциплина: {goal.disciplineScore ?? '—'}/100</span>
                  <span className="chip">Риск: {riskLabel(goal.riskStatus)}</span>
                </div>
                <div className="inline-actions">
                  <Link className="btn" to={`/goals/${goal.goalId}`}>
                    Открыть цель
                  </Link>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>

      <section className="card">
        <div className="card-header">
          <h3>История целей</h3>
          <strong>{history.length}</strong>
        </div>
        {history.length === 0 ? (
          <p className="muted">История появится после завершения или закрытия целей.</p>
        ) : (
          <div className="profile-history-list">
            {history.map((item) => (
              <article key={item.goalId} className={`profile-history-card ${item.status === 'FAILED' ? 'profile-history-failed' : ''}`}>
                <div className="card-header">
                  <div>
                    <strong>{item.title}</strong>
                    <p className="muted">{historyStatusLabel(item)}</p>
                  </div>
                  {item.loserBadge ? <span className="loser-badge">◔ Лузер badge</span> : <span className="status-badge">Выполнено</span>}
                </div>
                <div className="chips">
                  <span className="chip">Старт: {shortDate(item.createdAt)}</span>
                  {item.completedAt ? <span className="chip">Завершена: {shortDate(item.completedAt)}</span> : null}
                  {item.closedAt ? <span className="chip">Закрыта: {shortDate(item.closedAt)}</span> : null}
                </div>
                {item.failureReason ? (
                  <div className="failure-reason-box">
                    <strong>Причина невыполнения</strong>
                    <p>{item.failureReason}</p>
                  </div>
                ) : null}
              </article>
            ))}
          </div>
        )}
      </section>

      {success ? <section className="card success-card">{success}</section> : null}
      {error ? <section className="card error-card">{error}</section> : null}
    </div>
  );
}
