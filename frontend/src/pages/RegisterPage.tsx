import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthCard } from '../components/AuthCard';
import { api } from '../lib/apiClient';
import { setTokens } from '../lib/authStorage';

export function RegisterPage() {
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (payload: { email: string; password: string; fullName?: string }) => {
    setLoading(true);
    setError(null);
    try {
      const tokens = await api.register(payload);
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
        title="Регистрация"
        subtitle="Создайте аккаунт и запустите первый спринт по вашей цели"
        submitLabel={loading ? 'Создаем...' : 'Создать аккаунт'}
        switchText="Уже зарегистрированы?"
        switchLabel="Войти"
        switchTo="/login"
        onSubmit={handleSubmit}
        includeFullName
      />
      {error ? <p className="error-toast">{error}</p> : null}
    </>
  );
}
