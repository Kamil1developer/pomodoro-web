import { beforeEach, describe, expect, it } from 'vitest';
import { clearTokens, getTokens, setTokens } from './authStorage';

describe('authStorage', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('stores and reads tokens', () => {
    setTokens({ accessToken: 'a', refreshToken: 'r' });
    expect(getTokens()).toEqual({ accessToken: 'a', refreshToken: 'r' });
  });

  it('clears tokens', () => {
    setTokens({ accessToken: 'a', refreshToken: 'r' });
    clearTokens();
    expect(getTokens()).toBeNull();
  });
});
