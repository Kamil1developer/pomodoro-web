import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { MotivationPage } from './MotivationPage';

const { apiMock } = vi.hoisted(() => ({
  apiMock: {
    getMotivationFeed: vi.fn(),
    getGoalExperience: vi.fn(),
    markMotivationImageNotInteresting: vi.fn(),
    reportMotivationImage: vi.fn()
  }
}));

vi.mock('../lib/apiClient', () => ({
  api: apiMock,
  resolveAssetUrl: (path: string) => path
}));

vi.mock('../lib/useAppShellContext', () => ({
  useAppShellContext: () => ({
    selectedGoal: {
      id: 1,
      title: 'Java goal',
      description: 'Учить Java',
      targetHours: 50,
      deadline: null,
      themeColor: '#dff6e5',
      currentStreak: 0,
      createdAt: '2026-01-01T00:00:00Z'
    }
  })
}));

const baseFeed = {
  images: Array.from({ length: 10 }, (_, index) => ({
    id: index + 1,
    imageUrl: `https://example.com/${index + 1}.jpg`,
    sourceUrl: `https://source.example.com/${index + 1}`,
    title: `Image ${index + 1}`,
    description: 'Подобрано по теме цели',
    theme: 'CODE',
    createdAt: '2026-01-01T00:00:00Z'
  })),
  quote: {
    id: 1,
    goalId: 1,
    quoteText: 'Quote',
    quoteTextRu: 'Цитата',
    quoteAuthor: 'Автор',
    quoteDate: '2026-01-01'
  },
  recommendation: 'Сделай одну Pomodoro-сессию.'
};

const baseExperience = {
  goal: {
    id: 1,
    title: 'Java goal',
    description: 'Учить Java',
    targetHours: 50,
    deadline: null,
    themeColor: '#dff6e5',
    currentStreak: 0,
    createdAt: '2026-01-01T00:00:00Z'
  },
  commitment: null,
  today: {
    goalId: 1,
    goalTitle: 'Java goal',
    dailyTargetMinutes: 60,
    completedFocusMinutesToday: 20,
    remainingMinutesToday: 40,
    reportStatusToday: null,
    hasApprovedReportToday: false,
    isDailyTargetReached: false,
    isTodayCompleted: false,
    disciplineScore: 80,
    currentStreak: 2,
    riskStatus: 'LOW',
    motivationalMessage: 'Двигайся дальше',
    nextRecommendedAction: 'Сделай ещё одну сессию'
  },
  forecast: {
    goalId: 1,
    targetHours: 50,
    totalFocusMinutes: 120,
    averageDailyMinutes: 30,
    remainingMinutes: 2880,
    estimatedCompletionDate: '2026-06-01',
    onTrack: true,
    probabilityLabel: 'HIGH',
    explanation: 'Хороший темп'
  },
  recentEvents: [],
  aiRecommendation: 'Сделай одну Pomodoro-сессию.'
};

describe('MotivationPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    apiMock.getMotivationFeed.mockResolvedValue(baseFeed);
    apiMock.getGoalExperience.mockResolvedValue(baseExperience);
    apiMock.markMotivationImageNotInteresting.mockResolvedValue({
      imageId: 1,
      status: 'OK',
      message: 'Больше не будем показывать это изображение'
    });
    apiMock.reportMotivationImage.mockResolvedValue({
      imageId: 2,
      status: 'OK',
      message: 'Спасибо, мы учтём вашу жалобу'
    });
  });

  it('renders motivation image grid', async () => {
    render(<MotivationPage />);

    expect(await screen.findByTestId('motivation-grid')).toBeTruthy();
    expect(screen.getAllByRole('img')).toHaveLength(10);
  });

  it('clicking Неинтересно removes card', async () => {
    render(<MotivationPage />);
    await screen.findByText('Image 1');

    fireEvent.click(screen.getAllByText('Неинтересно')[0]);

    await waitFor(() => {
      expect(screen.queryByText('Image 1')).not.toBeInTheDocument();
    });
  });

  it('report form opens', async () => {
    render(<MotivationPage />);
    await screen.findByText('Image 1');

    fireEvent.click(screen.getAllByText('Пожаловаться')[0]);

    expect(screen.getByText('Почему вы жалуетесь?')).toBeTruthy();
    expect(screen.getByText('Отправить жалобу')).toBeTruthy();
  });

  it('submit report removes card', async () => {
    render(<MotivationPage />);
    await screen.findByText('Image 1');

    fireEvent.click(screen.getAllByText('Пожаловаться')[0]);
    fireEvent.click(screen.getByText('Отправить жалобу'));

    await waitFor(() => {
      expect(screen.queryByText('Image 1')).not.toBeInTheDocument();
    });
  });

  it('API error shows message', async () => {
    apiMock.getMotivationFeed.mockRejectedValueOnce(new Error('Feed error'));

    render(<MotivationPage />);

    expect(await screen.findByText('Feed error')).toBeTruthy();
  });
});
