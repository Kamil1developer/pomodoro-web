export type ReportStatus = 'PENDING' | 'CONFIRMED' | 'REJECTED' | 'OVERDUE';
export type AiVerdict = 'APPROVED' | 'REJECTED' | 'NEEDS_MORE_INFO';
export type ChatRole = 'USER' | 'ASSISTANT' | 'SYSTEM';

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
}

export interface Goal {
  id: number;
  title: string;
  description: string | null;
  targetHours: number | null;
  deadline: string | null;
  themeColor: string;
  currentStreak: number;
  createdAt: string;
}

export interface TaskItem {
  id: number;
  goalId: number;
  title: string;
  isDone: boolean;
  createdAt: string;
}

export interface FocusSession {
  id: number;
  goalId: number;
  startedAt: string;
  endedAt: string | null;
  durationMinutes: number | null;
}

export interface GoalProgress {
  completedTasks: number;
  allTasks: number;
  totalFocusMinutes: number;
  currentStreak: number;
}

export interface ReportItem {
  id: number;
  goalId: number;
  reportDate: string;
  comment: string | null;
  imagePath: string;
  status: ReportStatus;
  aiVerdict: AiVerdict | null;
  aiExplanation: string | null;
  createdAt: string;
}

export interface MotivationImage {
  id: number;
  goalId: number;
  imagePath: string;
  prompt: string;
  isFavorite: boolean;
  generatedBy: 'MANUAL' | 'AUTO';
  favoritedAt: string | null;
  pinnedUntil: string | null;
  isPinned: boolean;
  createdAt: string;
}

export interface MotivationQuote {
  id: number;
  goalId: number;
  quoteText: string;
  quoteTextRu: string;
  quoteAuthor: string;
  quoteDate: string;
}

export interface MotivationFeed {
  images: MotivationImage[];
  quotes: MotivationQuote[];
}

export interface ChatMessage {
  id: number;
  role: ChatRole;
  content: string;
  createdAt: string;
}

export interface ChatHistory {
  threadId: number;
  messages: ChatMessage[];
}

export interface DailyStat {
  date: string;
  completedTasks: number;
  focusMinutes: number;
  streak: number;
}

export interface GoalStats {
  goalId: number;
  days: DailyStat[];
}

export interface ApiError {
  code: string;
  message: string;
  details: string[];
  timestamp: string;
}
