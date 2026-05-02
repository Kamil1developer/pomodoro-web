import { useEffect, useState } from 'react';
import { api, resolveAssetUrl } from '../lib/apiClient';
import { useAppShellContext } from '../lib/useAppShellContext';
import type { GoalExperience, MotivationImage, MotivationQuote } from '../types/api';

export function MotivationPage() {
  const { selectedGoal } = useAppShellContext();
  const [images, setImages] = useState<MotivationImage[]>([]);
  const [quotes, setQuotes] = useState<MotivationQuote[]>([]);
  const [experience, setExperience] = useState<GoalExperience | null>(null);
  const [initialLoading, setInitialLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function refreshFeed() {
    if (!selectedGoal) {
      return;
    }

    setRefreshing(true);
    setError(null);
    try {
      const [feed, experienceData] = await Promise.all([
        api.refreshMotivationFeed(selectedGoal.id),
        api.getGoalExperience(selectedGoal.id)
      ]);
      setImages(feed.images.slice(0, 3));
      setQuotes(feed.quotes.slice(0, 3));
      setExperience(experienceData);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setRefreshing(false);
    }
  }

  useEffect(() => {
    if (!selectedGoal) {
      setImages([]);
      setQuotes([]);
      setExperience(null);
      setInitialLoading(false);
      setRefreshing(false);
      return;
    }

    const run = async () => {
      setInitialLoading(true);
      setError(null);
      try {
        const [imagesData, quoteData, experienceData] = await Promise.all([
          api.getMotivation(selectedGoal.id),
          api.getMotivationQuote(selectedGoal.id),
          api.getGoalExperience(selectedGoal.id)
        ]);
        setImages(imagesData.slice(0, 3));
        setQuotes([quoteData, quoteData, quoteData]);
        setExperience(experienceData);
      } catch (err) {
        setError((err as Error).message);
      } finally {
        setInitialLoading(false);
      }
    };

    void run();
  }, [selectedGoal]);

  const visibleImages = images.slice(0, 3);
  const visibleQuotes = quotes.slice(0, 3);

  function quoteForImage(index: number) {
    if (visibleQuotes.length === 0) {
      return null;
    }
    return visibleQuotes[index % visibleQuotes.length];
  }

  if (!selectedGoal) {
    return (
      <section className="empty-state">
        <h2>Нет активной цели</h2>
        <p>Выберите цель, чтобы мотивация привязывалась к её реальному прогрессу.</p>
      </section>
    );
  }

  return (
    <div className="page-grid">
      <section className="card">
        <div className="card-header">
          <h3>Мотивация по цели</h3>
          <strong>{visibleImages.length}</strong>
        </div>
        <p className="muted">Цель: {selectedGoal.title}</p>
        <div className="metric-grid compact-grid">
          <div className="metric-card">
            <span>Серия</span>
            <strong>{experience?.today.currentStreak ?? 0} дн.</strong>
          </div>
          <div className="metric-card">
            <span>Дисциплина</span>
            <strong>{experience?.today.disciplineScore ?? 0}/100</strong>
          </div>
          <div className="metric-card">
            <span>Риск</span>
            <strong>{experience?.today.riskStatus ?? 'Не рассчитан'}</strong>
          </div>
          <div className="metric-card">
            <span>Осталось сегодня</span>
            <strong>{experience?.today.remainingMinutesToday ?? 0} мин.</strong>
          </div>
          <div className="metric-card">
            <span>Прогноз</span>
            <strong>{experience?.forecast.probabilityLabel ?? 'UNKNOWN'}</strong>
          </div>
          <div className="metric-card">
            <span>Награда</span>
            <strong>
              {experience?.commitment?.personalRewardTitle || 'Не задана'}
              {experience?.commitment?.rewardUnlocked ? ' · разблокирована' : ''}
            </strong>
          </div>
        </div>
        <div className="quote-block">
          <p>{experience?.today.motivationalMessage ?? 'Лента подстроится под ваш текущий прогресс по цели.'}</p>
          <footer>Сегодня</footer>
        </div>
        <div className="recommendation-box">
          <strong>AI-рекомендация</strong>
          <p>{experience?.aiRecommendation ?? 'Сначала настройте обязательство по цели.'}</p>
        </div>
        <div className="inline-actions">
          <button className="btn" onClick={() => void refreshFeed()} disabled={refreshing}>
            {refreshing ? 'Обновление...' : 'Обновить ленту'}
          </button>
        </div>
        {visibleImages.length === 0 ? (
          <p className="muted">
            {initialLoading ? 'Загрузка ленты...' : 'Пока пусто. Нажмите «Обновить ленту».'}
          </p>
        ) : (
          <div className="motivation-feed">
            {visibleImages.map((image, index) => {
              const quote = quoteForImage(index);
              return (
                <article key={image.id} className="motivation-post motivation-post-quote">
                  <img
                    className="motivation-post-image"
                    src={resolveAssetUrl(image.imagePath)}
                    alt={image.prompt}
                    loading="lazy"
                  />
                  <div className="motivation-quote-overlay">
                    <p>{quote ? `“${quote.quoteTextRu || quote.quoteText}”` : '“Двигайся к цели каждый день.”'}</p>
                    <footer>{quote ? `— ${quote.quoteAuthor}` : '— Pomodoro Web'}</footer>
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </section>

      {error ? <section className="card error-card">{error}</section> : null}
    </div>
  );
}
