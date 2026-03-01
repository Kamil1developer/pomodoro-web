import { type FormEvent, useEffect, useRef, useState } from 'react';
import { api } from '../lib/apiClient';
import { shortDateTime } from '../lib/format';
import { useAppShellContext } from '../lib/useAppShellContext';
import type { ChatMessage } from '../types/api';

export function ChatPage() {
  const { selectedGoal } = useAppShellContext();
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [content, setContent] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const scrollRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!selectedGoal) {
      setMessages([]);
      return;
    }

    const run = async () => {
      setLoading(true);
      setError(null);
      try {
        const history = await api.getChatHistory(selectedGoal.id);
        setMessages(history.messages);
      } catch (err) {
        setError((err as Error).message);
      } finally {
        setLoading(false);
      }
    };

    void run();
  }, [selectedGoal]);

  useEffect(() => {
    if (!scrollRef.current) {
      return;
    }
    scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
  }, [messages]);

  async function sendMessage(event: FormEvent) {
    event.preventDefault();
    if (!selectedGoal || !content.trim()) {
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const history = await api.sendChat(selectedGoal.id, content.trim());
      setMessages(history.messages);
      setContent('');
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  }

  if (!selectedGoal) {
    return (
      <section className="empty-state">
        <h2>Нет активной цели</h2>
        <p>Выберите цель для диалога с AI-помощником.</p>
      </section>
    );
  }

  return (
    <div className="page-grid">
      <section className="card">
        <h3>Чат-помощник</h3>
        <p className="muted">Текущая цель: {selectedGoal.title}</p>
        <div className="chat-box" ref={scrollRef}>
          {messages.length === 0 ? <p className="muted">Начните диалог первым сообщением.</p> : null}
          {messages.map((message) => (
            <article key={message.id} className={`chat-msg ${message.role === 'USER' ? 'chat-user' : 'chat-assistant'}`}>
              <p>{message.content}</p>
              <time>{shortDateTime(message.createdAt)}</time>
            </article>
          ))}
        </div>

        <form className="inline-fields" onSubmit={sendMessage}>
          <input
            value={content}
            onChange={(event) => setContent(event.target.value)}
            placeholder="Например: почему мой отчет отклонен и что исправить?"
          />
          <button className="btn" type="submit" disabled={loading || !content.trim()}>
            {loading ? 'Отправка...' : 'Отправить'}
          </button>
        </form>
      </section>

      {error ? <section className="card error-card">{error}</section> : null}
    </div>
  );
}
