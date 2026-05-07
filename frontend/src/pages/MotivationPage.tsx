import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { api, resolveAssetUrl } from '../lib/apiClient';
import { useAppShellContext } from '../lib/useAppShellContext';
import type {
  GoalExperience,
  MotivationFeedResponse,
  MotivationImageItem,
  MotivationImageReportReason
} from '../types/api';

const REPORT_REASON_OPTIONS: Array<{ value: MotivationImageReportReason; label: string }> = [
  { value: 'IRRELEVANT_TO_GOAL', label: 'Не относится к моей цели' },
  { value: 'INAPPROPRIATE_IMAGE', label: 'Неподходящее изображение' },
  { value: 'INAPPROPRIATE_TEXT', label: 'Неподходящий текст' },
  { value: 'REPEATS_TOO_OFTEN', label: 'Повторяется слишком часто' },
  { value: 'NSFW', label: 'Нецензурно / NSFW' },
  { value: 'LOW_QUALITY', label: 'Низкое качество' },
  { value: 'BROKEN_IMAGE', label: 'Ошибка загрузки / битое изображение' },
  { value: 'OTHER', label: 'Другое' }
];

const FALLBACK_IMAGE =
  "data:image/svg+xml;utf8," +
  encodeURIComponent(`
    <svg xmlns="http://www.w3.org/2000/svg" width="900" height="1400" viewBox="0 0 900 1400">
      <defs>
        <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stop-color="#dff6e5" />
          <stop offset="100%" stop-color="#f4ead8" />
        </linearGradient>
      </defs>
      <rect width="100%" height="100%" fill="url(#bg)" />
      <circle cx="700" cy="230" r="160" fill="#ffffff55" />
      <circle cx="220" cy="1120" r="190" fill="#ffffff33" />
      <text x="80" y="220" fill="#173b35" font-size="54" font-family="Arial, sans-serif" font-weight="700">Pomodoro Web</text>
      <text x="80" y="300" fill="#35584e" font-size="30" font-family="Arial, sans-serif">Мотивационная карточка временно недоступна</text>
      <text x="80" y="380" fill="#35584e" font-size="28" font-family="Arial, sans-serif">Обновите ленту или продолжайте движение к цели.</text>
    </svg>
  `);

function sanitizeMotivationTitle(title: string | null | undefined): string {
  const value = (title ?? '').trim();
  if (!value || looksTechnicalText(value)) {
    return 'Визуальная мотивация';
  }
  return value.length > 72 ? `${value.slice(0, 72).trim()}…` : value;
}

function sanitizeMotivationDescription(description: string | null | undefined, goalTitle: string): string {
  const value = (description ?? '').trim();
  if (!value || looksTechnicalText(value)) {
    return `Продолжай движение к цели: ${goalTitle}.`;
  }
  return value.length > 180 ? `${value.slice(0, 180).trim()}…` : value;
}

function sanitizeCaption(
  caption: string | null | undefined,
  quoteText: string | null | undefined,
  goalTitle: string
): string {
  const direct = (caption ?? '').trim();
  if (direct && !looksTechnicalText(direct)) {
    return direct.length > 220 ? `${direct.slice(0, 220).trim()}…` : direct;
  }
  const quote = (quoteText ?? '').trim();
  if (quote && !looksTechnicalText(quote)) {
    return quote;
  }
  return `Каждая Pomodoro-сессия приближает тебя к цели: ${goalTitle}.`;
}

function sanitizeGoalReason(goalReason: string | null | undefined, goalTitle: string): string {
  const value = (goalReason ?? '').trim();
  if (!value || looksTechnicalText(value)) {
    return `Карточка подобрана под активную цель «${goalTitle}» и её текущий темп.`;
  }
  return value.length > 170 ? `${value.slice(0, 170).trim()}…` : value;
}

function looksTechnicalText(value: string): boolean {
  const normalized = value.toLowerCase().trim();
  if (!normalized) {
    return true;
  }
  return (
    normalized.startsWith('http://') ||
    normalized.startsWith('https://') ||
    normalized.startsWith('file:') ||
    normalized.includes('.pdf') ||
    normalized.includes('.jpg') ||
    normalized.includes('.jpeg') ||
    normalized.includes('.png') ||
    normalized.includes('.svg') ||
    normalized.includes('wikimedia') ||
    normalized.includes('commons') ||
    normalized.includes('/wiki/') ||
    /^[a-z0-9._-]{18,}$/i.test(normalized)
  );
}

function formatRisk(value: string | null | undefined): string {
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

function MotivationCard({
  image,
  quoteText,
  goalTitle,
  onNotInterested,
  onReport,
  disabled
}: {
  image: MotivationImageItem;
  quoteText?: string | null;
  goalTitle: string;
  onNotInterested: () => void;
  onReport: () => void;
  disabled: boolean;
}) {
  const [imageFailed, setImageFailed] = useState(false);
  const title = sanitizeMotivationTitle(image.title);
  const description = sanitizeMotivationDescription(image.description, goalTitle);
  const caption = sanitizeCaption(image.caption, quoteText, goalTitle);
  const goalReason = sanitizeGoalReason(image.goalReason, goalTitle);
  const finalImage = imageFailed ? FALLBACK_IMAGE : resolveAssetUrl(image.imageUrl);

  return (
    <article className="motivation-reel-card" data-testid="motivation-card">
      <div className="motivation-reel-image-frame">
        <img
          className="motivation-reel-image"
          src={finalImage}
          alt="Мотивационная иллюстрация"
          loading="lazy"
          onError={() => setImageFailed(true)}
        />
      </div>
      <div className="motivation-reel-body">
        <div className="motivation-reel-copy">
          <span className="motivation-reel-kicker">{title}</span>
          <p className="motivation-reel-quote">{caption}</p>
          <p className="motivation-reel-description">{description}</p>
          <p className="motivation-reel-reason">{goalReason}</p>
        </div>
        <div className="inline-actions motivation-reel-actions">
          <button className="btn btn-ghost" type="button" onClick={onNotInterested} disabled={disabled}>
            Неинтересно
          </button>
          <button className="btn btn-ghost" type="button" onClick={onReport} disabled={disabled}>
            Пожаловаться
          </button>
        </div>
      </div>
    </article>
  );
}

export function MotivationPage() {
  const { selectedGoal } = useAppShellContext();
  const [feed, setFeed] = useState<MotivationFeedResponse | null>(null);
  const [experience, setExperience] = useState<GoalExperience | null>(null);
  const [initialLoading, setInitialLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [workingImageId, setWorkingImageId] = useState<number | null>(null);
  const [reportingImage, setReportingImage] = useState<MotivationImageItem | null>(null);
  const [reportReason, setReportReason] = useState<MotivationImageReportReason>('IRRELEVANT_TO_GOAL');
  const [reportComment, setReportComment] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const scrollRef = useRef<HTMLDivElement | null>(null);

  const images = feed?.images ?? [];
  const quote = feed?.quote;

  const loadPageData = useCallback(
    async (showRefreshState = false) => {
      if (!selectedGoal) {
        return;
      }

      if (showRefreshState) {
        setRefreshing(true);
      }
      setError(null);
      try {
        if (showRefreshState) {
          try {
            await api.refreshMotivationFeed(selectedGoal.id);
          } catch (refreshError) {
            setError('Не удалось обновить ленту. Используем стандартные мотивационные карточки.');
            console.warn(refreshError);
          }
        }
        const [feedData, experienceData] = await Promise.all([
          api.getMotivationFeed(selectedGoal.id, 10),
          api.getGoalExperience(selectedGoal.id)
        ]);
        setFeed(feedData);
        setExperience(experienceData);
        scrollRef.current?.scrollTo({ top: 0, behavior: showRefreshState ? 'smooth' : 'auto' });
      } catch (err) {
        setError((err as Error).message);
      } finally {
        if (showRefreshState) {
          setRefreshing(false);
        }
      }
    },
    [selectedGoal]
  );

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

  function openReport(image: MotivationImageItem) {
    setReportingImage(image);
    setReportReason('IRRELEVANT_TO_GOAL');
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
      setSuccessMessage(
        response.message || 'Жалоба отправлена. Мы больше не будем показывать эту карточку.'
      );
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
      remainingMinutes: experience?.today.remainingMinutesToday ?? 0,
      walletBalance: experience?.today.walletBalance ?? 0,
      penalty: experience?.today.dailyPenaltyAmount ?? 0,
      moneyEnabled: Boolean(experience?.today.moneyEnabled)
    }),
    [experience]
  );

  if (!selectedGoal) {
    return (
      <section className="empty-state card">
        <h2>Создайте или выберите активную цель, чтобы получить мотивационную ленту.</h2>
      </section>
    );
  }

  return (
    <div className="page-grid motivation-page-layout">
      <section className="card motivation-summary-card">
        <div className="card-header">
          <div>
            <h3>Мотивационная лента</h3>
            <p className="muted">Цель: {selectedGoal.title}</p>
          </div>
          <button className="btn" onClick={() => void loadPageData(true)} disabled={refreshing}>
            {refreshing ? 'Обновляем ленту...' : 'Обновить ленту'}
          </button>
        </div>
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
            <strong>{formatRisk(stats.risk)}</strong>
          </div>
          <div className="metric-card">
            <span>Осталось сегодня</span>
            <strong>{stats.remainingMinutes} мин.</strong>
          </div>
          <div className="metric-card">
            <span>Баланс</span>
            <strong>{stats.walletBalance} монет</strong>
          </div>
          <div className="metric-card">
            <span>Штраф за пропуск</span>
            <strong>{stats.moneyEnabled ? `${stats.penalty} монет` : 'выключен'}</strong>
          </div>
        </div>
        <div className="recommendation-box">
          <strong>Рекомендация</strong>
          <p>
            {feed?.recommendation ??
              experience?.aiRecommendation ??
              `Сделай ещё одну короткую сессию сегодня, чтобы сохранить темп.`}
          </p>
        </div>
        {successMessage ? <p className="success-note">{successMessage}</p> : null}
        {initialLoading ? <p className="muted">Загрузка мотивационной ленты...</p> : null}
      </section>

      {images.length === 0 && !initialLoading ? (
        <section className="card empty-state">
          <h3>Пока нет мотивационных карточек для этой цели.</h3>
          <p>Попробуйте обновить ленту или изменить описание цели.</p>
        </section>
      ) : null}

      {images.length > 0 ? (
        <section className="motivation-reels-shell">
          <div className="motivation-reels" ref={scrollRef} data-testid="motivation-feed">
            {images.map((image) => (
              <div key={image.id} className="motivation-reel-section">
                <MotivationCard
                  image={image}
                  quoteText={quote?.quoteTextRu || quote?.quoteText}
                  goalTitle={selectedGoal.title}
                  onNotInterested={() => void handleNotInterested(image.id)}
                  onReport={() => openReport(image)}
                  disabled={workingImageId === image.id}
                />
              </div>
            ))}
          </div>
        </section>
      ) : null}

      {reportingImage ? (
        <section className="card report-modal" role="dialog" aria-modal="true">
          <h3>Почему вы жалуетесь?</h3>
          <p className="muted">После отправки карточка исчезнет из вашей ленты.</p>
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
