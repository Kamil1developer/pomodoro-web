import { useEffect, useState } from 'react';
import { api } from '../lib/apiClient';
import { shortDate, shortDateTime } from '../lib/format';
import { useAppShellContext } from '../lib/useAppShellContext';
import type { GoalProgress, ReportItem } from '../types/api';
import { ProgressCard } from '../components/ProgressCard';

export function DashboardPage() {
  const { selectedGoal } = useAppShellContext();
  const [progress, setProgress] = useState<GoalProgress | null>(null);
  const [reports, setReports] = useState<ReportItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!selectedGoal) {
      setProgress(null);
      setReports([]);
      return;
    }

    const run = async () => {
      setLoading(true);
      setError(null);
      try {
        const [progressData, reportData] = await Promise.all([
          api.getProgress(selectedGoal.id),
          api.getReports(selectedGoal.id)
        ]);
        setProgress(progressData);
        setReports(reportData.slice(0, 5));
      } catch (err) {
        setError((err as Error).message);
      } finally {
        setLoading(false);
      }
    };

    void run();
  }, [selectedGoal]);

  if (!selectedGoal) {
    return (
      <section className="empty-state">
        <h2>Нет активной цели</h2>
        <p>Перейдите в раздел «Контроль» и создайте первую цель.</p>
      </section>
    );
  }

  return (
    <div className="page-grid">
      <section className="card page-intro">
        <h2>{selectedGoal.title}</h2>
        <p>{selectedGoal.description || 'Добавьте описание, чтобы AI давал более точные рекомендации.'}</p>
        <div className="chips">
          <span className="chip">Создана: {shortDate(selectedGoal.createdAt)}</span>
          {selectedGoal.deadline ? <span className="chip">Дедлайн: {shortDate(selectedGoal.deadline)}</span> : null}
          {selectedGoal.targetHours ? <span className="chip">Цель: {selectedGoal.targetHours} ч.</span> : null}
        </div>
      </section>

      {loading ? <div className="card">Загрузка сводки...</div> : null}
      {error ? <div className="card error-card">{error}</div> : null}

      {progress ? <ProgressCard progress={progress} /> : null}

      <section className="card">
        <div className="card-header">
          <h3>Последние отчеты</h3>
          <strong>{reports.length}</strong>
        </div>
        {reports.length === 0 ? (
          <p className="muted">Отчетов пока нет.</p>
        ) : (
          <ul className="feed">
            {reports.map((report) => (
              <li key={report.id}>
                <div>
                  <p className="feed-title">
                    {report.status} · {report.aiVerdict ?? 'N/A'}
                  </p>
                  <p className="feed-subtitle">{report.aiExplanation ?? 'Без комментария AI'}</p>
                </div>
                <time>{shortDateTime(report.createdAt)}</time>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
