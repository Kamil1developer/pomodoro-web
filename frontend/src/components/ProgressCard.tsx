import { minutesToHours, toPercent } from '../lib/format';
import type { GoalProgress } from '../types/api';

interface ProgressCardProps {
  progress: GoalProgress;
}

export function ProgressCard({ progress }: ProgressCardProps) {
  const percent = toPercent(progress.completedTasks, progress.allTasks);

  return (
    <section className="card">
      <div className="card-header">
        <h3>Прогресс цели</h3>
        <strong>{percent}%</strong>
      </div>
      <div className="progress-track" role="progressbar" aria-valuemin={0} aria-valuemax={100} aria-valuenow={percent}>
        <div className="progress-fill" style={{ width: `${percent}%` }} />
      </div>
      <div className="stats-grid">
        <div>
          <p className="stat-label">Задачи</p>
          <p className="stat-value">
            {progress.completedTasks}/{progress.allTasks}
          </p>
        </div>
        <div>
          <p className="stat-label">Фокус</p>
          <p className="stat-value">{minutesToHours(progress.totalFocusMinutes)}</p>
        </div>
        <div>
          <p className="stat-label">Streak</p>
          <p className="stat-value">{progress.currentStreak} дн.</p>
        </div>
      </div>
    </section>
  );
}
