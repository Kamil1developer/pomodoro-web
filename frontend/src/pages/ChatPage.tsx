import { type FormEvent, useEffect, useRef, useState } from 'react';
import { api } from '../lib/apiClient';
import { shortDateTime } from '../lib/format';
import { useAppShellContext } from '../lib/useAppShellContext';
import type { ChatMessage, GoalExperience } from '../types/api';

type UiChatMessage = ChatMessage & { pending?: boolean };

export function ChatPage() {
  const { selectedGoal } = useAppShellContext();
  const [messages, setMessages] = useState<UiChatMessage[]>([]);
  const [experience, setExperience] = useState<GoalExperience | null>(null);
  const [content, setContent] = useState('');
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [sending, setSending] = useState(false);
  const [clearing, setClearing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const scrollRef = useRef<HTMLDivElement | null>(null);
  const quickPrompts = [
    'Что мне сделать сегодня?',
    'Почему я отстаю?',
    'Как не сорвать streak?',
    'Как не потерять деньги?',
    'Составь план на вечер.'
  ];

  useEffect(() => {
    if (!selectedGoal) {
      setMessages([]);
      setExperience(null);
      return;
    }

    const run = async () => {
      setLoadingHistory(true);
      setError(null);
      try {
        const [history, experienceData] = await Promise.all([
          api.getChatHistory(selectedGoal.id),
          api.getGoalExperience(selectedGoal.id)
        ]);
        setMessages(history.messages);
        setExperience(experienceData);
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
      const experienceData = await api.getGoalExperience(selectedGoal.id);
      setExperience(experienceData);
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

  async function clearConversation() {
    if (!selectedGoal || clearing || sending) {
      return;
    }
    setClearing(true);
    setError(null);
    try {
      const history = await api.clearChatHistory(selectedGoal.id);
      setMessages(history.messages);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setClearing(false);
    }
  }

  if (!selectedGoal) {
    return (
      <section className="empty-state">
        <h2>Нет активной цели</h2>
        <p>Выберите цель для диалога с Мотиватором.</p>
      </section>
    );
  }

  return (
    <div className="page-grid">
      <section className="card">
        <div className="card-header">
          <h3>Мотиватор</h3>
          <button
            className="btn btn-ghost"
            onClick={() => void clearConversation()}
            disabled={clearing || sending || loadingHistory}>
            {clearing ? 'Очистка...' : 'Очистить диалог'}
          </button>
        </div>
        <p className="muted">Текущая цель: {selectedGoal.title}</p>
        {experience ? (
          <div className="chips">
            <span className="chip">Осталось сегодня: {experience.today.remainingMinutesToday ?? 0} мин.</span>
            <span className="chip">Серия: {experience.today.currentStreak ?? 0} дн.</span>
            <span className="chip">Дисциплина: {experience.today.disciplineScore ?? 0}/100</span>
            <span className="chip">Риск: {experience.today.riskStatus ?? 'Не рассчитан'}</span>
            <span className="chip">Баланс: {experience.today.walletBalance ?? 0} монет</span>
            <span className="chip">Штраф: {experience.today.dailyPenaltyAmount ?? 10} монет</span>
          </div>
        ) : null}
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
              <p>Мотиватор думает...</p>
            </article>
          ) : null}
        </div>

        <div className="chips">
          {quickPrompts.map((prompt) => (
            <button
              key={prompt}
              className="chip chip-button"
              type="button"
              onClick={() => setContent(prompt)}
              disabled={sending || clearing}>
              {prompt}
            </button>
          ))}
        </div>

        <form className="inline-fields" onSubmit={sendMessage}>
          <input
            value={content}
            onChange={(event) => setContent(event.target.value)}
            placeholder="Например: что мне сделать сегодня, почему я отстаю или как не сорвать streak?"
            disabled={clearing}
          />
          <button className="btn" type="submit" disabled={sending || clearing || !content.trim()}>
            {sending ? 'Ожидание...' : 'Отправить'}
          </button>
        </form>
      </section>

      {error ? <section className="card error-card">{error}</section> : null}
    </div>
  );
}
