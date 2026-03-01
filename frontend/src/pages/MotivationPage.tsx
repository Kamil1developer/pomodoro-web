import { useEffect, useState } from 'react';
import { api, resolveAssetUrl } from '../lib/apiClient';
import { shortDateTime } from '../lib/format';
import { useAppShellContext } from '../lib/useAppShellContext';
import type { MotivationImage, MotivationQuote } from '../types/api';

export function MotivationPage() {
  const { selectedGoal } = useAppShellContext();
  const [images, setImages] = useState<MotivationImage[]>([]);
  const [quote, setQuote] = useState<MotivationQuote | null>(null);
  const [showRussianQuote, setShowRussianQuote] = useState(true);
  const [initialLoading, setInitialLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function refreshFeed(background = false) {
    if (!selectedGoal) {
      return;
    }

    setRefreshing(true);
    if (!background) {
      setError(null);
    }
    try {
      const feed = await api.refreshMotivationFeed(selectedGoal.id);
      setImages(feed.images);
      setQuote(feed.quote);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setRefreshing(false);
    }
  }

  useEffect(() => {
    if (!selectedGoal) {
      setImages([]);
      setQuote(null);
      setInitialLoading(false);
      setRefreshing(false);
      return;
    }

    const run = async () => {
      setInitialLoading(true);
      setError(null);
      try {
        const [imagesData, quoteData] = await Promise.all([
          api.getMotivation(selectedGoal.id),
          api.getMotivationQuote(selectedGoal.id)
        ]);
        setImages(imagesData);
        setQuote(quoteData);
      } catch (err) {
        setError((err as Error).message);
      } finally {
        setInitialLoading(false);
        void (async () => {
          try {
            setRefreshing(true);
            const feed = await api.refreshMotivationFeed(selectedGoal.id);
            setImages(feed.images);
            setQuote(feed.quote);
          } catch (err) {
            setError((err as Error).message);
          } finally {
            setRefreshing(false);
          }
        })();
      }
    };

    void run();
  }, [selectedGoal]);

  async function toggleFavorite(image: MotivationImage) {
    try {
      const updated = await api.favoriteMotivation(image.id, !image.isFavorite);
      setImages((prev) => prev.map((item) => (item.id === updated.id ? updated : item)));
    } catch (err) {
      setError((err as Error).message);
    }
  }

  async function deleteImage(imageId: number) {
    try {
      await api.deleteMotivation(imageId);
      setImages((prev) => prev.filter((item) => item.id !== imageId));
    } catch (err) {
      setError((err as Error).message);
    }
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
        <h3>Генерация мотивации</h3>
        <p className="muted">Цель: {selectedGoal.title}</p>
        <p className="muted">Лента обновляется при открытии страницы или по кнопке ниже.</p>
        {quote ? (
          <blockquote className="quote-block">
            <p>“{showRussianQuote ? quote.quoteTextRu || quote.quoteText : quote.quoteText}”</p>
            <footer>— {quote.quoteAuthor}</footer>
          </blockquote>
        ) : null}
        <div className="inline-actions">
          <button className="btn" onClick={() => void refreshFeed()} disabled={refreshing}>
            {refreshing ? 'Обновление...' : 'Обновить ленту'}
          </button>
          {quote ? (
            <button className="btn btn-ghost" onClick={() => setShowRussianQuote((prev) => !prev)}>
              {showRussianQuote ? 'Показать оригинал' : 'Перевести на русский'}
            </button>
          ) : null}
        </div>
      </section>

      {error ? <section className="card error-card">{error}</section> : null}

      <section className="card">
        <div className="card-header">
          <h3>Галерея</h3>
          <strong>{images.length}</strong>
        </div>
        {images.length === 0 ? (
          <p className="muted">
            {initialLoading ? 'Загрузка ленты...' : 'Пока пусто. Нажмите «Обновить ленту».'}
          </p>
        ) : (
          <div className="gallery">
            {images.map((image) => (
              <article key={image.id} className="gallery-item">
                <a href={resolveAssetUrl(image.imagePath)} target="_blank" rel="noreferrer">
                  <img src={resolveAssetUrl(image.imagePath)} alt={image.prompt} loading="lazy" />
                </a>
                <p>{image.prompt}</p>
                <small>{shortDateTime(image.createdAt)}</small>
                <small>
                  {image.generatedBy === 'AUTO' ? 'Автогенерация' : 'Ручная генерация'}
                  {image.isPinned && image.pinnedUntil ? ` · В избранном до ${shortDateTime(image.pinnedUntil)}` : ''}
                </small>
                <div className="inline-actions">
                  <button className="btn btn-ghost" onClick={() => void toggleFavorite(image)}>
                    {image.isFavorite ? 'Убрать из избранного' : 'В избранное'}
                  </button>
                  <button className="btn btn-danger" onClick={() => void deleteImage(image.id)}>
                    Удалить
                  </button>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
