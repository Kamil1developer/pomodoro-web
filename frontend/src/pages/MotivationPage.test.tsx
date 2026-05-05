import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { MotivationPage } from './MotivationPage';

const { apiMock } = vi.hoisted(() => ({
  apiMock: {
    getMotivationFeed: vi.fn(),
    getGoalExperience: vi.fn(),
    refreshMotivationFeed: vi.fn(),
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
      status: 'ACTIVE',
      currentStreak: 0,
      completedAt: null,
      closedAt: null,
      failureReason: null,
      createdAt: '2026-01-01T00:00:00Z'
    }
  })
}));

const baseFeed = {
  images: Array.from({ length: 10 }, (_, index) => ({
    id: index + 1,
    imageUrl: `https://example.com/${index + 1}.jpg`,
    title: `Image ${index + 1}`,
    description: 'Подобрано по теме цели',
    caption: 'Делай маленький шаг и двигайся дальше.',
    goalReason: 'Карточка подобрана под активную цель.',
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
    status: 'ACTIVE',
    currentStreak: 0,
    completedAt: null,
    closedAt: null,
    failureReason: null,
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
    apiMock.refreshMotivationFeed.mockResolvedValue(baseFeed);
    apiMock.markMotivationImageNotInteresting.mockResolvedValue({
      imageId: 1,
      status: 'OK',
      message: 'Больше не будем показывать эту карточку'
    });
    apiMock.reportMotivationImage.mockResolvedValue({
      imageId: 2,
      status: 'OK',
      message: 'Жалоба отправлена. Мы больше не будем показывать эту карточку.'
    });
  });

  it('renders vertical motivation feed', async () => {
    render(<MotivationPage />);

    expect(await screen.findByTestId('motivation-feed')).toBeTruthy();
    expect(screen.getAllByTestId('motivation-card')).toHaveLength(10);
  });

  it('clicking Не интересует removes card', async () => {
    render(<MotivationPage />);
    await screen.findByText('Image 1');

    fireEvent.click(screen.getAllByText('Не интересует')[0]);

    await waitFor(() => {
      expect(screen.queryByText('Image 1')).not.toBeInTheDocument();
    });
  });

  it('report form opens with categories', async () => {
    render(<MotivationPage />);
    await screen.findByText('Image 1');

    fireEvent.click(screen.getAllByText('Пожаловаться')[0]);

    expect(screen.getByText('Почему вы жалуетесь?')).toBeTruthy();
    expect(screen.getByText('Не относится к моей цели')).toBeTruthy();
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

  it('broken image switches to fallback source', async () => {
    render(<MotivationPage />);

    const images = await screen.findAllByAltText('Мотивационная иллюстрация');
    const image = images[0];
    fireEvent.error(image);

    await waitFor(() => {
      expect((image as HTMLImageElement).src).toContain('data:image/svg+xml');
    });
  });

  it('API error shows message', async () => {
    apiMock.getMotivationFeed.mockRejectedValueOnce(new Error('Feed error'));

    render(<MotivationPage />);

    expect(await screen.findByText('Feed error')).toBeTruthy();
  });
});
