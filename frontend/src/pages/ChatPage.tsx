import { type FormEvent, useEffect, useRef, useState } from 'react';
import { api } from '../lib/apiClient';
import { shortDateTime } from '../lib/format';
import { useAppShellContext } from '../lib/useAppShellContext';
import type { ChatMessage } from '../types/api';

type UiChatMessage = ChatMessage & { pending?: boolean };

export function ChatPage() {
  const { selectedGoal } = useAppShellContext();
  const [messages, setMessages] = useState<UiChatMessage[]>([]);
  const [content, setContent] = useState('');
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const scrollRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!selectedGoal) {
      setMessages([]);
      return;
    }

    const run = async () => {
      setLoadingHistory(true);
      setError(null);
      try {
        const history = await api.getChatHistory(selectedGoal.id);
        setMessages(history.messages);
      } catch (err) {
        setError((err as Error).message);
      } finally {
        setLoadingHistory(false);
      }
    };

    void run();
  }, [selectedGoal]);

  useEffect(() => {
    if (!scrollRef.current) {
      return;
    }
    scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
  }, [messages, sending]);

  async function sendMessage(event: FormEvent) {
    event.preventDefault();
    if (!selectedGoal || !content.trim() || sending) {
      return;
    }

    const text = content.trim();
    const optimisticId = -Date.now();
    const optimisticMessage: UiChatMessage = {
      id: optimisticId,
      role: 'USER',
      content: text,
      createdAt: new Date().toISOString(),
      pending: true
    };

    setMessages((prev) => [...prev, optimisticMessage]);
    setContent('');
    setSending(true);
    setError(null);
    try {
      const history = await api.sendChat(selectedGoal.id, text);
      setMessages(history.messages);
    } catch (err) {
      setMessages((prev) =>
        prev.map((message) =>
          message.id === optimisticId
            ? { ...message, pending: false, content: `${message.content}\n(не отправлено)` }
            : message
        )
      );
      setError((err as Error).message);
    } finally {
      setSending(false);
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
          {messages.length === 0 && !loadingHistory ? <p className="muted">Начните диалог первым сообщением.</p> : null}
          {loadingHistory ? <p className="muted">Загрузка истории чата...</p> : null}
          {messages.map((message) => (
            <article key={message.id} className={`chat-msg ${message.role === 'USER' ? 'chat-user' : 'chat-assistant'}`}>
              <p>{message.content}</p>
              <time>{shortDateTime(message.createdAt)}</time>
              {message.pending ? <small className="chat-meta">Отправка...</small> : null}
            </article>
          ))}
          {sending ? (
            <article className="chat-msg chat-assistant chat-thinking">
              <div className="typing-indicator" aria-label="Бот думает">
                <span className="typing-dot" />
                <span className="typing-dot" />
                <span className="typing-dot" />
              </div>
              <p>Бот думает...</p>
            </article>
          ) : null}
        </div>

        <form className="inline-fields" onSubmit={sendMessage}>
          <input
            value={content}
            onChange={(event) => setContent(event.target.value)}
            placeholder="Например: почему мой отчет отклонен и что исправить?"
          />
          <button className="btn" type="submit" disabled={sending || !content.trim()}>
            {sending ? 'Ожидание...' : 'Отправить'}
          </button>
        </form>
      </section>

      {error ? <section className="card error-card">{error}</section> : null}
    </div>
  );
}
