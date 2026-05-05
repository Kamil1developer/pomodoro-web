import { useCallback, useEffect, useMemo, useState } from 'react';
import { api, resolveAssetUrl } from '../lib/apiClient';
import { useAppShellContext } from '../lib/useAppShellContext';
import type {
  GoalExperience,
  MotivationFeedResponse,
  MotivationImageItem,
  MotivationImageReportReason
} from '../types/api';

const REPORT_REASON_OPTIONS: Array<{ value: MotivationImageReportReason; label: string }> = [
  { value: 'INAPPROPRIATE', label: 'Неподходящее изображение' },
  { value: 'OFFENSIVE', label: 'Оскорбительное' },
  { value: 'LOW_QUALITY', label: 'Низкое качество' },
  { value: 'IRRELEVANT', label: 'Не относится к моей цели' },
  { value: 'SPAM', label: 'Спам' },
  { value: 'OTHER', label: 'Другое' }
];

export function MotivationPage() {
  const { selectedGoal } = useAppShellContext();
  const [feed, setFeed] = useState<MotivationFeedResponse | null>(null);
  const [experience, setExperience] = useState<GoalExperience | null>(null);
  const [initialLoading, setInitialLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [workingImageId, setWorkingImageId] = useState<number | null>(null);
  const [reportingImage, setReportingImage] = useState<MotivationImageItem | null>(null);
  const [reportReason, setReportReason] = useState<MotivationImageReportReason>('IRRELEVANT');
  const [reportComment, setReportComment] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const images = feed?.images ?? [];
  const quote = feed?.quote;

  const loadPageData = useCallback(async (showRefreshState = false) => {
    if (!selectedGoal) {
      return;
    }

    if (showRefreshState) {
      setRefreshing(true);
    }
    setError(null);
    try {
      const [feedData, experienceData] = await Promise.all([
        api.getMotivationFeed(selectedGoal.id, 10),
        api.getGoalExperience(selectedGoal.id)
      ]);
      setFeed(feedData);
      setExperience(experienceData);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      if (showRefreshState) {
        setRefreshing(false);
      }
    }
  }, [selectedGoal]);

  useEffect(() => {
    if (!selectedGoal) {
      setFeed(null);
      setExperience(null);
      setInitialLoading(false);
      return;
    }

    const run = async () => {
      setInitialLoading(true);
      setSuccessMessage(null);
      await loadPageData(false);
      setInitialLoading(false);
    };

    void run();
  }, [selectedGoal, loadPageData]);

  async function handleNotInterested(imageId: number) {
    const currentImages = images;
    setWorkingImageId(imageId);
    setSuccessMessage(null);
    setFeed((prev) =>
      prev
        ? {
            ...prev,
            images: prev.images.filter((image) => image.id !== imageId)
          }
        : prev
    );

    try {
      const response = await api.markMotivationImageNotInteresting(imageId);
      setSuccessMessage(response.message || 'Больше не будем показывать это изображение');
      if (currentImages.length <= 10) {
        await loadPageData(false);
      }
    } catch (err) {
      setFeed((prev) =>
        prev
          ? {
              ...prev,
              images: currentImages
            }
          : prev
      );
      setError((err as Error).message);
    } finally {
      setWorkingImageId(null);
    }
  }

  function openReport(image: MotivationImageItem) {
    setReportingImage(image);
    setReportReason('IRRELEVANT');
    setReportComment('');
    setSuccessMessage(null);
  }

  async function submitReport() {
    if (!reportingImage) {
      return;
    }

    const imageId = reportingImage.id;
    const currentImages = images;
    setWorkingImageId(imageId);
    setError(null);
    setFeed((prev) =>
      prev
        ? {
            ...prev,
            images: prev.images.filter((image) => image.id !== imageId)
          }
        : prev
    );

    try {
      const response = await api.reportMotivationImage(imageId, {
        reason: reportReason,
        comment: reportComment.trim() || undefined
      });
      setSuccessMessage(response.message || 'Спасибо, мы учтём вашу жалобу');
      setReportingImage(null);
      await loadPageData(false);
    } catch (err) {
      setFeed((prev) =>
        prev
          ? {
              ...prev,
              images: currentImages
            }
          : prev
      );
      setError((err as Error).message);
    } finally {
      setWorkingImageId(null);
    }
  }

  const stats = useMemo(
    () => ({
      streak: experience?.today.currentStreak ?? 0,
      discipline: experience?.today.disciplineScore ?? 0,
      risk: experience?.today.riskStatus ?? 'Не рассчитан',
      remainingMinutes: experience?.today.remainingMinutesToday ?? 0
    }),
    [experience]
  );

  if (!selectedGoal) {
    return (
      <section className="empty-state">
        <h2>Нет активной цели</h2>
        <p>Выберите цель, чтобы мотивация подстраивалась под её тему и прогресс.</p>
      </section>
    );
  }

  return (
    <div className="page-grid">
      <section className="card">
        <div className="card-header">
          <h3>Мотивация по цели</h3>
          <strong>{images.length}</strong>
        </div>
        <p className="muted">Цель: {selectedGoal.title}</p>
        <div className="metric-grid compact-grid">
          <div className="metric-card">
            <span>Серия</span>
            <strong>{stats.streak} дн.</strong>
          </div>
          <div className="metric-card">
            <span>Дисциплина</span>
            <strong>{stats.discipline}/100</strong>
          </div>
          <div className="metric-card">
            <span>Риск</span>
            <strong>{stats.risk}</strong>
          </div>
          <div className="metric-card">
            <span>Осталось сегодня</span>
            <strong>{stats.remainingMinutes} мин.</strong>
          </div>
        </div>
        {quote ? (
          <div className="quote-block">
            <p>{quote.quoteTextRu || quote.quoteText}</p>
            <footer>{quote.quoteAuthor}</footer>
          </div>
        ) : null}
        <div className="recommendation-box">
          <strong>Рекомендация</strong>
          <p>{feed?.recommendation ?? experience?.aiRecommendation ?? 'Сделайте один маленький шаг к цели уже сегодня.'}</p>
        </div>
        <div className="inline-actions">
          <button className="btn" onClick={() => void loadPageData(true)} disabled={refreshing}>
            {refreshing ? 'Обновление...' : 'Обновить ленту'}
          </button>
        </div>
        {successMessage ? <p className="success-note">{successMessage}</p> : null}
        {initialLoading ? <p className="muted">Загрузка мотивационной ленты...</p> : null}
        {images.length === 0 && !initialLoading ? (
          <p className="muted">Подходящие изображения ещё подбираются. Нажмите «Обновить ленту».</p>
        ) : null}
        <div className="motivation-grid" data-testid="motivation-grid">
          {images.map((image) => (
            <article key={image.id} className="motivation-grid-card">
              <img
                className="motivation-grid-image"
                src={resolveAssetUrl(image.imageUrl)}
                alt={image.title}
                loading="lazy"
              />
              <div className="motivation-grid-body">
                <strong>{image.title}</strong>
                <p>{image.description || image.theme}</p>
                <div className="inline-actions motivation-card-actions">
                  <button
                    className="btn btn-ghost"
                    type="button"
                    onClick={() => void handleNotInterested(image.id)}
                    disabled={workingImageId === image.id}
                  >
                    Неинтересно
                  </button>
                  <button
                    className="btn btn-ghost"
                    type="button"
                    onClick={() => openReport(image)}
                    disabled={workingImageId === image.id}
                  >
                    Пожаловаться
                  </button>
                </div>
              </div>
            </article>
          ))}
        </div>
      </section>

      {reportingImage ? (
        <section className="card report-modal" role="dialog" aria-modal="true">
          <h3>Почему вы жалуетесь?</h3>
          <p className="muted">Изображение больше не будет показываться вам после отправки жалобы.</p>
          <select value={reportReason} onChange={(event) => setReportReason(event.target.value as MotivationImageReportReason)}>
            {REPORT_REASON_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
          <textarea
            rows={3}
            value={reportComment}
            onChange={(event) => setReportComment(event.target.value)}
            placeholder="Комментарий (необязательно)"
          />
          <div className="inline-actions">
            <button className="btn" type="button" onClick={() => void submitReport()} disabled={workingImageId === reportingImage.id}>
              Отправить жалобу
            </button>
            <button className="btn btn-ghost" type="button" onClick={() => setReportingImage(null)}>
              Отмена
            </button>
          </div>
        </section>
      ) : null}

      {error ? <section className="card error-card">{error}</section> : null}
    </div>
  );
}
