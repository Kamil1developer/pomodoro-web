import { describe, expect, it } from 'vitest';
import { minutesToHours, toPercent } from './format';

describe('format helpers', () => {
  it('converts minutes to hour string', () => {
    expect(minutesToHours(90)).toBe('1.5h');
  });

  it('returns zero percent when no tasks', () => {
    expect(toPercent(3, 0)).toBe(0);
  });

  it('calculates rounded percent', () => {
    expect(toPercent(1, 3)).toBe(33);
  });
});
