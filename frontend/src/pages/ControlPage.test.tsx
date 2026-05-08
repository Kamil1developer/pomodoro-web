import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { ControlPage } from './ControlPage';

const apiMock = vi.hoisted(() => ({
  createGoal: vi.fn(),
  getGoalExperience: vi.fn()
}));

const appShellMock = vi.hoisted(() => ({
  goals: [],
  selectedGoal: null,
  selectedGoalId: null,
  setSelectedGoalId: vi.fn(),
  reloadGoals: vi.fn(),
  logout: vi.fn()
}));

vi.mock('../lib/apiClient', () => ({
  api: apiMock
}));

vi.mock('../lib/useAppShellContext', () => ({
  useAppShellContext: () => appShellMock
}));

describe('ControlPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows duplicate goal error from backend', async () => {
    apiMock.createGoal.mockRejectedValueOnce(
      new Error('Похожая активная цель уже существует: «Выучить английский». Сначала завершите или закройте её.')
    );

    render(<ControlPage />);

    fireEvent.change(screen.getByPlaceholderText('Название цели'), {
      target: { value: 'Начать изучать английский язык' }
    });

    fireEvent.submit(screen.getByRole('button', { name: 'Добавить цель' }).closest('form')!);

    await waitFor(() => {
      expect(
        screen.getByText('Похожая активная цель уже существует: «Выучить английский». Сначала завершите или закройте её.')
      ).toBeTruthy();
    });
    expect(screen.getByText('Создать цель').closest('section')?.querySelector('.inline-alert')).toBeTruthy();
    expect(document.querySelector('.error-card')).toBeNull();
  });
});
