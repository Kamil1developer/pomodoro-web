import { useEffect, useState } from 'react';
import { api } from '../lib/apiClient';
import { shortDateTime } from '../lib/format';
import { useAppShellContext } from '../lib/useAppShellContext';
import type { MotivationImage } from '../types/api';

export function MotivationPage() {
  const { selectedGoal } = useAppShellContext();
  const [images, setImages] = useState<MotivationImage[]>([]);
  const [styleOptions, setStyleOptions] = useState('cinematic success poster');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!selectedGoal) {
      setImages([]);
      return;
    }

    const run = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await api.getMotivation(selectedGoal.id);
        setImages(data);
      } catch (err) {
        setError((err as Error).message);
      } finally {
        setLoading(false);
      }
    };

    void run();
  }, [selectedGoal]);

  async function generateImage() {
    if (!selectedGoal) {
      return;
    }

    setLoading(true);
    setError(null);
    try {
      await api.generateMotivation(selectedGoal.id, styleOptions.trim());
      const data = await api.getMotivation(selectedGoal.id);
      setImages(data);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  }

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
        <div className="inline-fields">
          <input
            value={styleOptions}
            onChange={(event) => setStyleOptions(event.target.value)}
            placeholder="Стиль изображения"
          />
          <button className="btn" onClick={() => void generateImage()} disabled={loading}>
            {loading ? 'Генерация...' : 'Сгенерировать'}
          </button>
        </div>
      </section>

      {error ? <section className="card error-card">{error}</section> : null}

      <section className="card">
        <div className="card-header">
          <h3>Галерея</h3>
          <strong>{images.length}</strong>
        </div>
        {images.length === 0 ? (
          <p className="muted">Пока пусто. Нажмите «Сгенерировать».</p>
        ) : (
          <div className="gallery">
            {images.map((image) => (
              <article key={image.id} className="gallery-item">
                <a href={image.imagePath} target="_blank" rel="noreferrer">
                  <img src={image.imagePath} alt={image.prompt} loading="lazy" />
                </a>
                <p>{image.prompt}</p>
                <small>{shortDateTime(image.createdAt)}</small>
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
