import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthCard } from '../components/AuthCard';
import { api } from '../lib/apiClient';
import { setTokens } from '../lib/authStorage';

export function LoginPage() {
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (payload: { email: string; password: string }) => {
    setLoading(true);
    setError(null);
    try {
      const tokens = await api.login(payload);
      setTokens({ accessToken: tokens.accessToken, refreshToken: tokens.refreshToken });
      navigate('/', { replace: true });
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <AuthCard
        title="Вход в Pomodoro Web"
        subtitle="Управляйте целями, фокус-сессиями и AI-ассистентом в одном месте"
        submitLabel={loading ? 'Входим...' : 'Войти'}
        switchText="Еще нет аккаунта?"
        switchLabel="Зарегистрироваться"
        switchTo="/register"
        onSubmit={handleSubmit}
      />
      {error ? <p className="error-toast">{error}</p> : null}
    </>
  );
}
