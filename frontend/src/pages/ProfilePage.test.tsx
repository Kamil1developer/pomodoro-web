import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { ProfilePage } from './ProfilePage';

const { apiMock } = vi.hoisted(() => ({
  apiMock: {
    getProfile: vi.fn(),
    updateProfile: vi.fn(),
    uploadAvatar: vi.fn(),
    getProfileAvatar: vi.fn(),
    getWalletTransactions: vi.fn()
  }
}));

vi.mock('../lib/apiClient', () => ({
  api: apiMock,
  resolveAssetUrl: (path: string) => path
}));

const profileResponse = {
  userId: 1,
  email: 'user@test.dev',
  fullName: 'Камиль Хусаинов',
  avatarPath: null,
  stats: {
    activeGoalsCount: 2,
    completedGoalsCount: 1,
    failedGoalsCount: 1,
    totalFocusMinutes: 180,
    bestStreak: 5,
    averageDiscipline: 77.5,
    riskSummary: 'LOW'
  },
  wallet: {
    balance: 950,
    initialBalance: 1000,
    totalPenalties: 50,
    status: 'ACTIVE'
  },
  activeGoals: [
    {
      goalId: 11,
      title: 'Java goal',
      status: 'ACTIVE',
      currentStreak: 2,
      dailyTargetMinutes: 60,
      completedFocusMinutesToday: 20,
      remainingMinutesToday: 40,
      disciplineScore: 80,
      riskStatus: 'LOW',
      moneyEnabled: true,
      dailyPenaltyAmount: 10,
      totalPenaltyCharged: 50,
      moneyStatus: 'ACTIVE',
      createdAt: '2026-01-01T00:00:00Z'
    }
  ],
  goalHistory: [
    {
      goalId: 12,
      title: 'English goal',
      status: 'FAILED',
      failureReason: 'Не хватило времени',
      createdAt: '2026-01-01T00:00:00Z',
      completedAt: null,
      closedAt: '2026-02-01T00:00:00Z',
      totalPenaltyCharged: 50,
      loserBadge: true
    }
  ]
};

describe('ProfilePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    Object.defineProperty(URL, 'createObjectURL', {
      configurable: true,
      value: vi.fn(() => 'blob:avatar')
    });
    Object.defineProperty(URL, 'revokeObjectURL', {
      configurable: true,
      value: vi.fn()
    });
    apiMock.getProfile.mockResolvedValue(profileResponse);
    apiMock.updateProfile.mockResolvedValue(profileResponse);
    apiMock.uploadAvatar.mockResolvedValue({ ...profileResponse, avatarPath: '/uploads/avatars/test.png' });
    apiMock.getProfileAvatar.mockResolvedValue(new Blob(['avatar'], { type: 'image/png' }));
    apiMock.getWalletTransactions.mockResolvedValue({
      transactions: [
        {
          id: 1,
          type: 'INITIAL_GRANT',
          amount: 1000,
          balanceBefore: 0,
          balanceAfter: 1000,
          reason: 'Стартовый баланс',
          goalTitle: null,
          createdAt: '2026-01-01T00:00:00Z'
        }
      ]
    });
  });

  it('renders profile tab content', async () => {
    render(
      <MemoryRouter>
        <ProfilePage />
      </MemoryRouter>
    );

    expect(await screen.findByText('Профиль')).toBeTruthy();
    expect(screen.getByText('Текущие цели')).toBeTruthy();
    expect(screen.getByText('История целей')).toBeTruthy();
  });

  it('avatar upload UI exists', async () => {
    render(
      <MemoryRouter>
        <ProfilePage />
      </MemoryRouter>
    );

    expect(await screen.findByText('Загрузить аватар')).toBeTruthy();
  });

  it('updates avatar through file input', async () => {
    render(
      <MemoryRouter>
        <ProfilePage />
      </MemoryRouter>
    );

    await screen.findByText('Загрузить аватар');
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    const file = new File(['avatar'], 'avatar.png', { type: 'image/png' });
    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(apiMock.uploadAvatar).toHaveBeenCalled();
    });
  });
});
