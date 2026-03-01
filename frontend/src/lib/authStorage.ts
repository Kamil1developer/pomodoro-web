const ACCESS_TOKEN_KEY = 'pomodoro_access_token';
const REFRESH_TOKEN_KEY = 'pomodoro_refresh_token';

export interface StoredTokens {
  accessToken: string;
  refreshToken: string;
}

export function getTokens(): StoredTokens | null {
  const accessToken = localStorage.getItem(ACCESS_TOKEN_KEY);
  const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
  if (!accessToken || !refreshToken) {
    return null;
  }
  return { accessToken, refreshToken };
}

export function setTokens(tokens: StoredTokens): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, tokens.accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken);
}

export function clearTokens(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
}
