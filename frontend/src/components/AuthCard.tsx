import { Link } from 'react-router-dom';

interface AuthCardProps {
  title: string;
  subtitle: string;
  switchLabel: string;
  switchTo: string;
  switchText: string;
  onSubmit: (payload: { email: string; password: string }) => Promise<void>;
  submitLabel: string;
}

export function AuthCard({
  title,
  subtitle,
  switchLabel,
  switchTo,
  switchText,
  onSubmit,
  submitLabel
}: AuthCardProps) {
  return (
    <div className="auth-layout">
      <section className="auth-card">
        <h1>{title}</h1>
        <p>{subtitle}</p>

        <form
          className="auth-form"
          onSubmit={async (event) => {
            event.preventDefault();
            const formData = new FormData(event.currentTarget);
            const email = String(formData.get('email') ?? '').trim();
            const password = String(formData.get('password') ?? '');
            await onSubmit({ email, password });
          }}
        >
          <label>
            <span>Email</span>
            <input name="email" type="email" required placeholder="you@example.com" />
          </label>

          <label>
            <span>Пароль</span>
            <input name="password" type="password" required minLength={8} placeholder="Минимум 8 символов" />
          </label>

          <button className="btn" type="submit">
            {submitLabel}
          </button>
        </form>

        <p className="auth-switch">
          {switchText} <Link to={switchTo}>{switchLabel}</Link>
        </p>
      </section>
    </div>
  );
}
