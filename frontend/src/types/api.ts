export type ReportStatus = 'PENDING' | 'CONFIRMED' | 'REJECTED' | 'OVERDUE';
export type AiVerdict = 'APPROVED' | 'REJECTED' | 'NEEDS_MORE_INFO';
export type ChatRole = 'USER' | 'ASSISTANT' | 'SYSTEM';
export type CommitmentStatus = 'ACTIVE' | 'COMPLETED' | 'PAUSED' | 'FAILED';
export type RiskStatus = 'LOW' | 'MEDIUM' | 'HIGH';
export type GoalEventType =
  | 'GOAL_CREATED'
  | 'COMMITMENT_CREATED'
  | 'FOCUS_SESSION_STARTED'
  | 'FOCUS_SESSION_COMPLETED'
  | 'REPORT_SUBMITTED'
  | 'REPORT_APPROVED'
  | 'REPORT_REJECTED'
  | 'REPORT_NEEDS_MORE_INFO'
  | 'DAY_COMPLETED'
  | 'DAY_MISSED'
  | 'STREAK_UPDATED'
  | 'DISCIPLINE_SCORE_CHANGED'
  | 'RISK_STATUS_CHANGED'
  | 'REWARD_UNLOCKED'
  | 'AI_RECOMMENDATION_CREATED';

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
  aiConfidence: number | null;
  aiExplanation: string | null;
  createdAt: string;
}

export interface GoalCommitment {
  id: number;
  goalId: number;
  dailyTargetMinutes: number;
  startDate: string;
  endDate: string | null;
  status: CommitmentStatus;
  disciplineScore: number;
  currentStreak: number;
  bestStreak: number;
  completedDays: number;
  missedDays: number;
  personalRewardTitle: string | null;
  personalRewardDescription: string | null;
  rewardUnlocked: boolean;
  riskStatus: RiskStatus;
  createdAt: string;
  updatedAt: string;
}

export interface TodayStatus {
  goalId: number;
  goalTitle: string;
  dailyTargetMinutes: number | null;
  completedFocusMinutesToday: number;
  remainingMinutesToday: number | null;
  reportStatusToday: ReportStatus | null;
  hasApprovedReportToday: boolean;
  isDailyTargetReached: boolean;
  isTodayCompleted: boolean;
  disciplineScore: number | null;
  currentStreak: number | null;
  riskStatus: RiskStatus | null;
  motivationalMessage: string;
  nextRecommendedAction: string;
}

export interface Forecast {
  goalId: number;
  targetHours: number | null;
  totalFocusMinutes: number;
  averageDailyMinutes: number;
  remainingMinutes: number | null;
  estimatedCompletionDate: string | null;
  onTrack: boolean;
  probabilityLabel: string;
  explanation: string;
}

export interface GoalEvent {
  id: number;
  goalId: number;
  type: GoalEventType;
  title: string;
  description: string | null;
  oldValue: string | null;
  newValue: string | null;
  createdAt: string;
}

export interface GoalExperience {
  goal: Goal;
  commitment: GoalCommitment | null;
  today: TodayStatus;
  forecast: Forecast;
  recentEvents: GoalEvent[];
  aiRecommendation: string;
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
