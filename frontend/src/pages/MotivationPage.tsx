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

const FALLBACK_QUOTES = [
  'Не жди идеального настроения: начни с одного короткого действия.',
  'Дисциплина начинается там, где ты выбираешь сделать шаг, даже если не хочется.',
  'Маленькая Pomodoro-сессия сегодня сильнее большого плана на потом.',
  'Результат складывается не из рывков, а из повторяемых действий.',
  'Сделай первый шаг сейчас, а уверенность догонит тебя по дороге.',
  'Каждая завершённая сессия доказывает, что цель ближе, чем кажется.',
  'Не нужно делать идеально. Нужно сделать достаточно, чтобы продолжить завтра.',
  'Фокус на ближайшие 25 минут важнее тревоги о всём пути.',
  'Сегодня не нужно побеждать весь путь. Достаточно честно пройти следующий отрезок.',
  'Если цель кажется большой, уменьши шаг, но не останавливай движение.',
  'Твоя привычка растёт каждый раз, когда ты возвращаешься к делу.',
  'Сначала действие, потом настроение. Так строится устойчивый темп.',
  'Одна законченная сессия лучше десяти отложенных обещаний.',
  'Сохрани внимание на ближайшей задаче, и прогресс станет видимым.',
  'Ты не обязан делать много. Ты обязан не исчезать из процесса.',
  'Стабильность выигрывает у мотивации, когда приходит обычный трудный день.',
  'Каждый отчёт — это доказательство, что цель живёт не только в голове.',
  'Сделай короткую сессию сейчас, чтобы вечером не догонять самого себя.',
  'Не сравнивай темп с чужим. Сравни сегодняшний шаг со вчерашней паузой.',
  'Хороший день для цели начинается с маленького выполненного обещания.'
];

function sanitizeMotivationTitle(title: string | null | undefined): string {
  const value = (title ?? '').trim();
  if (!value || looksTechnicalText(value)) {
    return 'Визуальная мотивация';
  }
  return value.length > 72 ? `${value.slice(0, 72).trim()}…` : value;
}

function pickMotivationQuote(
  image: MotivationImageItem,
  quoteText: string | null | undefined,
  goalTitle: string,
  goalDescription: string | null | undefined,
  quoteIndex: number
): string {
  const candidates = [image.displayQuote, quoteIndex === 0 ? quoteText : null, image.caption];
  for (const candidate of candidates) {
    const value = cleanMotivationText(candidate, goalTitle, goalDescription);
    if (value) {
      return value;
    }
  }
  return FALLBACK_QUOTES[Math.abs(image.id + quoteIndex) % FALLBACK_QUOTES.length];
}

function cleanMotivationText(
  value: string | null | undefined,
  goalTitle: string,
  goalDescription: string | null | undefined
): string | null {
  const text = (value ?? '').trim();
  if (!text || looksTechnicalText(text) || isGoalEcho(text, goalTitle, goalDescription) || isServicePhrase(text)) {
    return null;
  }
  return text.length > 190 ? `${text.slice(0, 190).trim()}…` : text;
}

function normalizeComparableText(value: string | null | undefined): string {
  return (value ?? '')
    .toLowerCase()
    .replace(/[ё]/g, 'е')
    .replace(/[^\p{L}\p{N}\s]/gu, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function isGoalEcho(value: string, goalTitle: string, goalDescription: string | null | undefined): boolean {
  const text = normalizeComparableText(value);
  const title = normalizeComparableText(goalTitle);
  const description = normalizeComparableText(goalDescription);
  if (!text) {
    return true;
  }
  if (title && (text === title || text.includes(title))) {
    return true;
  }
  if (description && (text === description || text.includes(description) || description.includes(text))) {
    return true;
  }
  return false;
}

function isServicePhrase(value: string): boolean {
  const normalized = value.toLowerCase();
  return (
    normalized.includes('подобрано по активной цели') ||
    normalized.includes('карточка подобрана') ||
    normalized.includes('мотивационная карточка') ||
    normalized.includes('visual motivation') ||
    normalized.includes('goal:')
  );
}

function formatQuote(value: string): string {
  const trimmed = value.trim();
  if (/^[«“"].*[»”"]$/.test(trimmed)) {
    return trimmed;
  }
  return `«${trimmed}»`;
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
  goalDescription,
  quoteIndex,
  onNotInterested,
  onReport,
  disabled
}: {
  image: MotivationImageItem;
  quoteText?: string | null;
  goalTitle: string;
  goalDescription?: string | null;
  quoteIndex: number;
  onNotInterested: () => void;
  onReport: () => void;
  disabled: boolean;
}) {
  const [imageFailed, setImageFailed] = useState(false);
  const title = cleanMotivationText(image.title, goalTitle, goalDescription) ?? sanitizeMotivationTitle(null);
  const quote = pickMotivationQuote(image, quoteText, goalTitle, goalDescription, quoteIndex);
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
          <p className="motivation-reel-quote">{formatQuote(quote)}</p>
        </div>
        <div className="inline-actions motivation-reel-actions">
          <button className="btn btn-ghost" type="button" onClick={onNotInterested} disabled={disabled}>
            Не интересует
          </button>
          <button className="btn btn-ghost" type="button" onClick={onReport} disabled={disabled}>
            Пожаловаться на контент
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
  const [summaryCollapsed, setSummaryCollapsed] = useState(false);
  const scrollRef = useRef<HTMLDivElement | null>(null);
  const hiddenImageIdsRef = useRef<Set<number>>(new Set());

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
        const hiddenIds = hiddenImageIdsRef.current;
        setFeed({
          ...feedData,
          images: feedData.images.filter((image) => !hiddenIds.has(image.id))
        });
        setExperience(experienceData);
        if (showRefreshState) {
          setSuccessMessage(feedData.refreshMessage);
        }
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
      hiddenImageIdsRef.current = new Set();
      await loadPageData(false);
      setInitialLoading(false);
    };

    void run();
  }, [selectedGoal, loadPageData]);

  async function handleNotInterested(imageId: number) {
    const currentImages = images;
    setWorkingImageId(imageId);
    setSuccessMessage(null);
    hiddenImageIdsRef.current.add(imageId);
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
      hiddenImageIdsRef.current.delete(imageId);
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
    hiddenImageIdsRef.current.add(imageId);
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
      hiddenImageIdsRef.current.delete(imageId);
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
      penalty: experience?.today.dailyPenaltyAmount ?? 10
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
    <div className={`page-grid motivation-page-layout ${summaryCollapsed ? 'motivation-summary-collapsed' : ''}`}>
      <section className="card motivation-summary-card">
        <div className="card-header">
          <div>
            <h3>Мотивационная лента</h3>
            <p className="muted">
              Цель: {selectedGoal.title}
              {summaryCollapsed ? ` · ${stats.remainingMinutes} мин. до нормы · ${stats.walletBalance} монет` : ''}
            </p>
          </div>
          <div className="inline-actions motivation-summary-actions">
            <button className="btn btn-ghost" type="button" onClick={() => setSummaryCollapsed((value) => !value)}>
              {summaryCollapsed ? 'Показать сводку' : 'Свернуть сводку'}
            </button>
            <button className="btn" onClick={() => void loadPageData(true)} disabled={refreshing}>
              {refreshing ? 'Обновляем...' : 'Обновить ленту'}
            </button>
          </div>
        </div>
        {!summaryCollapsed ? (
          <>
            <div className="metric-grid compact-grid motivation-summary-details">
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
                <strong>{stats.penalty} монет</strong>
              </div>
            </div>
            <div className="recommendation-box motivation-summary-details">
              <strong>Рекомендация</strong>
              <p>
                {feed?.recommendation ??
                  experience?.aiRecommendation ??
                  `Сделай ещё одну короткую сессию сегодня, чтобы сохранить темп.`}
              </p>
            </div>
          </>
        ) : null}
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
            {images.map((image, index) => (
              <div
                key={`${image.id}-${feed?.refreshSessionId ?? 'initial'}-${feed?.feedVersion ?? 0}-${index}`}
                className="motivation-reel-section"
              >
                <MotivationCard
                  image={image}
                  quoteText={quote?.quoteTextRu || quote?.quoteText}
                  goalTitle={selectedGoal.title}
                  goalDescription={selectedGoal.description}
                  quoteIndex={index}
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
