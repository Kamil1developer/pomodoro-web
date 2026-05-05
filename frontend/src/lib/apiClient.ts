import {
  type ChatHistory,
  type Forecast,
  type FocusSession,
  type Goal,
  type GoalCommitment,
  type GoalEvent,
  type GoalExperience,
  type GoalProgress,
  type GoalStats,
  type MotivationFeed,
  type MotivationFeedResponse,
  type MotivationImage,
  type MotivationQuote,
  type ProfileGoalsResponse,
  type ProfileResponse,
  type ReportMotivationImageRequest,
  type ReportItem,
  type TaskItem,
  type TodayStatus,
  type TokenResponse
} from '../types/api';
import { clearTokens, getTokens, setTokens } from './authStorage';

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '') ?? 'http://localhost:8080/api';
const API_ORIGIN = new URL(API_BASE_URL).origin;

export function resolveAssetUrl(path: string): string {
  if (!path) {
    return '';
  }
  if (path.startsWith('http://') || path.startsWith('https://') || path.startsWith('data:')) {
    return path;
  }
  if (path.startsWith('/')) {
    return `${API_ORIGIN}${path}`;
  }
  return `${API_ORIGIN}/${path}`;
}

export class HttpError extends Error {
  status: number;
  code?: string;
  details?: string[];

  constructor(status: number, message: string, code?: string, details?: string[]) {
    super(message);
    this.name = 'HttpError';
    this.status = status;
    this.code = code;
    this.details = details;
  }
}

let refreshPromise: Promise<boolean> | null = null;

async function refreshTokens(): Promise<boolean> {
  const current = getTokens();
  if (!current?.refreshToken) {
    return false;
  }

  if (refreshPromise) {
    return refreshPromise;
  }

  refreshPromise = fetch(`${API_BASE_URL}/auth/refresh`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ refreshToken: current.refreshToken })
  })
    .then(async (res) => {
      if (!res.ok) {
        clearTokens();
        return false;
      }
      const data = (await res.json()) as TokenResponse;
      setTokens({ accessToken: data.accessToken, refreshToken: data.refreshToken });
      return true;
    })
    .finally(() => {
      refreshPromise = null;
    });

  return refreshPromise;
}

async function parseError(response: Response): Promise<HttpError> {
  try {
    const body = (await response.json()) as {
      code?: string;
      message?: string;
      details?: string[];
    };
    return new HttpError(
      response.status,
      body.message ?? `Request failed with status ${response.status}`,
      body.code,
      body.details
    );
  } catch {
    return new HttpError(response.status, `Request failed with status ${response.status}`);
  }
}

async function request<T>(
  path: string,
  options: RequestInit = {},
  withAuth = true,
  retry = true
): Promise<T> {
  const headers = new Headers(options.headers ?? {});
  const tokens = getTokens();

  if (withAuth && tokens?.accessToken) {
    headers.set('Authorization', `Bearer ${tokens.accessToken}`);
  }

  const isFormData = options.body instanceof FormData;
  if (!isFormData && options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers
  });

  if (response.status === 401 && withAuth && retry) {
    const refreshed = await refreshTokens();
    if (refreshed) {
      return request<T>(path, options, withAuth, false);
    }
    clearTokens();
    throw new HttpError(401, 'Unauthorized');
  }

  if (!response.ok) {
    throw await parseError(response);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const contentType = response.headers.get('content-type');
  if (!contentType?.includes('application/json')) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export const api = {
  register(payload: { email: string; password: string; fullName?: string }) {
    return request<TokenResponse>('/auth/register', {
      method: 'POST',
      body: JSON.stringify(payload)
    }, false);
  },
  login(payload: { email: string; password: string }) {
    return request<TokenResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify(payload)
    }, false);
  },
  logout(refreshToken: string) {
    return request<void>('/auth/logout', {
      method: 'POST',
      body: JSON.stringify({ refreshToken })
    });
  },
  getGoals() {
    return request<Goal[]>('/goals');
  },
  getGoal(id: number) {
    return request<Goal>(`/goals/${id}`);
  },
  createGoal(payload: {
    title: string;
    description?: string;
    targetHours?: number;
    deadline?: string;
    themeColor?: string;
  }) {
    return request<Goal>('/goals', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
  },
  updateGoal(
    id: number,
    payload: {
      title: string;
      description?: string;
      targetHours?: number;
      deadline?: string;
      themeColor?: string;
    }
  ) {
    return request<Goal>(`/goals/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    });
  },
  deleteGoal(id: number) {
    return request<void>(`/goals/${id}`, {
      method: 'DELETE'
    });
  },
  closeFailedGoal(id: number, reason: string) {
    return request<Goal>(`/goals/${id}/close-failed`, {
      method: 'POST',
      body: JSON.stringify({ reason })
    });
  },
  getTasks(goalId: number) {
    return request<TaskItem[]>(`/goals/${goalId}/tasks`);
  },
  createTask(goalId: number, payload: { title: string }) {
    return request<TaskItem>(`/goals/${goalId}/tasks`, {
      method: 'POST',
      body: JSON.stringify(payload)
    });
  },
  updateTask(goalId: number, taskId: number, payload: { title: string; isDone?: boolean }) {
    return request<TaskItem>(`/goals/${goalId}/tasks/${taskId}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    });
  },
  deleteTask(goalId: number, taskId: number) {
    return request<void>(`/goals/${goalId}/tasks/${taskId}`, {
      method: 'DELETE'
    });
  },
  getProgress(goalId: number) {
    return request<GoalProgress>(`/goals/${goalId}/progress`);
  },
  getStats(goalId: number) {
    return request<GoalStats>(`/goals/${goalId}/stats`);
  },
  startFocus(goalId: number) {
    return request<FocusSession>(`/goals/${goalId}/focus/start`, {
      method: 'POST'
    });
  },
  stopFocus(goalId: number) {
    return request<FocusSession>(`/goals/${goalId}/focus/stop`, {
      method: 'POST'
    });
  },
  getFocusSessions(goalId: number) {
    return request<FocusSession[]>(`/goals/${goalId}/focus`);
  },
  uploadReport(goalId: number, file: File, comment: string) {
    const form = new FormData();
    form.append('file', file);
    form.append('comment', comment);
    return request<ReportItem>(`/goals/${goalId}/reports`, {
      method: 'POST',
      body: form
    });
  },
  getReports(goalId: number) {
    return request<ReportItem[]>(`/goals/${goalId}/reports`);
  },
  createCommitment(
    goalId: number,
    payload: {
      dailyTargetMinutes: number;
      startDate: string;
      endDate?: string;
      personalRewardTitle?: string;
      personalRewardDescription?: string;
    }
  ) {
    return request<GoalCommitment>(`/goals/${goalId}/commitment`, {
      method: 'POST',
      body: JSON.stringify(payload)
    });
  },
  getCommitment(goalId: number) {
    return request<GoalCommitment>(`/goals/${goalId}/commitment`);
  },
  getTodayStatus(goalId: number) {
    return request<TodayStatus>(`/goals/${goalId}/today`);
  },
  getForecast(goalId: number) {
    return request<Forecast>(`/goals/${goalId}/forecast`);
  },
  getGoalEvents(goalId: number) {
    return request<GoalEvent[]>(`/goals/${goalId}/events`);
  },
  getGoalExperience(goalId: number) {
    return request<GoalExperience>(`/goals/${goalId}/experience`);
  },
  getDashboardExperience() {
    return request<GoalExperience[]>('/goal-experience');
  },
  generateMotivation(goalId: number, styleOptions: string) {
    return request<MotivationImage>(`/goals/${goalId}/motivation/generate`, {
      method: 'POST',
      body: JSON.stringify({ styleOptions })
    });
  },
  getMotivation(goalId: number) {
    return request<MotivationImage[]>(`/goals/${goalId}/motivation`);
  },
  getMotivationQuote(goalId: number) {
    return request<MotivationQuote>(`/goals/${goalId}/motivation/quote`);
  },
  getMotivationFeed(goalId?: number, limit = 10) {
    const params = new URLSearchParams();
    if (goalId != null) {
      params.set('goalId', String(goalId));
    }
    if (limit) {
      params.set('limit', String(limit));
    }
    return request<MotivationFeedResponse>(`/motivation/feed?${params.toString()}`);
  },
  refreshMotivationFeed(goalId: number) {
    return request<MotivationFeed>(`/goals/${goalId}/motivation/refresh-feed`, {
      method: 'POST'
    });
  },
  markMotivationImageNotInteresting(imageId: number) {
    return request<{ imageId: number; status: string; message: string }>(
      `/motivation/images/${imageId}/not-interested`,
      {
        method: 'POST'
      }
    );
  },
  reportMotivationImage(imageId: number, payload: ReportMotivationImageRequest) {
    return request<{ imageId: number; status: string; message: string }>(
      `/motivation/images/${imageId}/report`,
      {
        method: 'POST',
        body: JSON.stringify(payload)
      }
    );
  },
  favoriteMotivation(imageId: number, isFavorite: boolean) {
    return request<MotivationImage>(`/motivation/${imageId}/favorite`, {
      method: 'PATCH',
      body: JSON.stringify({ isFavorite })
    });
  },
  deleteMotivation(imageId: number) {
    return request<void>(`/motivation/${imageId}`, {
      method: 'DELETE'
    });
  },
  sendChat(goalId: number, content: string) {
    return request<ChatHistory>(`/goals/${goalId}/chat/send`, {
      method: 'POST',
      body: JSON.stringify({ content })
    });
  },
  getChatHistory(goalId: number) {
    return request<ChatHistory>(`/goals/${goalId}/chat/history`);
  },
  clearChatHistory(goalId: number) {
    return request<ChatHistory>(`/goals/${goalId}/chat/history`, {
      method: 'DELETE'
    });
  },
  getProfile() {
    return request<ProfileResponse>('/profile');
  },
  updateProfile(payload: { fullName?: string }) {
    return request<ProfileResponse>('/profile', {
      method: 'PUT',
      body: JSON.stringify(payload)
    });
  },
  uploadAvatar(file: File) {
    const form = new FormData();
    form.append('file', file);
    return request<ProfileResponse>('/profile/avatar', {
      method: 'POST',
      body: form
    });
  },
  getProfileGoals() {
    return request<ProfileGoalsResponse>('/profile/goals');
  }
};
