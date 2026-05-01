import { useEffect, useState } from 'react';
import { api, resolveAssetUrl } from '../lib/apiClient';
import { useAppShellContext } from '../lib/useAppShellContext';
import type { MotivationImage, MotivationQuote } from '../types/api';

export function MotivationPage() {
  const { selectedGoal } = useAppShellContext();
  const [images, setImages] = useState<MotivationImage[]>([]);
  const [quotes, setQuotes] = useState<MotivationQuote[]>([]);
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
      const feed = await api.refreshMotivationFeed(selectedGoal.id);
      setImages(feed.images.slice(0, 3));
      setQuotes(feed.quotes);
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
      setInitialLoading(false);
      setRefreshing(false);
      return;
    }

    const run = async () => {
      setInitialLoading(true);
      setError(null);
      try {
        const imagesData = await api.getMotivation(selectedGoal.id);
        setImages(imagesData.slice(0, 3));
        setQuotes([]);
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
        <p>Выберите цель, чтобы генерировать мотивационные образы результата.</p>
      </section>
    );
  }

  return (
    <div className="page-grid">
      <section className="card">
        <div className="card-header">
          <h3>Лента мотивации</h3>
          <strong>{visibleImages.length}</strong>
        </div>
        <p className="muted">Цель: {selectedGoal.title}</p>
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
