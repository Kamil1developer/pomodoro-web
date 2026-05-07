export type ReportStatus = 'PENDING' | 'CONFIRMED' | 'REJECTED' | 'OVERDUE';
export type AiVerdict = 'APPROVED' | 'REJECTED' | 'NEEDS_MORE_INFO';
export type ChatRole = 'USER' | 'ASSISTANT' | 'SYSTEM';
export type CommitmentStatus = 'ACTIVE' | 'COMPLETED' | 'PAUSED' | 'FAILED';
export type RiskStatus = 'LOW' | 'MEDIUM' | 'HIGH';
export type GoalStatus = 'ACTIVE' | 'COMPLETED' | 'FAILED' | 'ARCHIVED';
export type CommitmentMoneyStatus = 'ACTIVE' | 'EMPTY' | 'DISABLED';
export type WalletStatus = 'ACTIVE' | 'EMPTY' | 'LOCKED';
export type WalletTransactionType =
  | 'INITIAL_GRANT'
  | 'GOAL_DEPOSIT_LOCKED'
  | 'DAILY_PENALTY'
  | 'GOAL_COMPLETED_REWARD'
  | 'MANUAL_ADJUSTMENT'
  | 'REFUND'
  | 'ACCOUNT_LOCKED';
export type MotivationImageFeedbackType = 'NOT_INTERESTED' | 'REPORTED';
export type MotivationImageReportReason =
  | 'NSFW'
  | 'IRRELEVANT_TO_GOAL'
  | 'INAPPROPRIATE_IMAGE'
  | 'INAPPROPRIATE_TEXT'
  | 'REPEATS_TOO_OFTEN'
  | 'INAPPROPRIATE'
  | 'UNPLEASANT'
  | 'OFFENSIVE'
  | 'LOW_QUALITY'
  | 'IRRELEVANT'
  | 'DUPLICATE'
  | 'BROKEN_IMAGE'
  | 'SPAM'
  | 'OTHER';
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
  | 'AI_RECOMMENDATION_CREATED'
  | 'MONEY_PENALTY_CHARGED'
  | 'MONEY_EMPTY';

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
  status: GoalStatus;
  currentStreak: number;
  completedAt: string | null;
  closedAt: string | null;
  failureReason: string | null;
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
  moneyEnabled: boolean;
  depositAmount: number;
  dailyPenaltyAmount: number;
  totalPenaltyCharged: number;
  moneyStatus: CommitmentMoneyStatus;
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
  walletBalance: number | null;
  dailyPenaltyAmount: number | null;
  depositAmount: number | null;
  totalPenaltyCharged: number | null;
  moneyEnabled: boolean;
  moneyStatus: CommitmentMoneyStatus | null;
  nextPenaltyWarning: string | null;
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

export interface MotivationImageItem {
  id: number;
  imageUrl: string;
  title: string;
  description: string | null;
  caption: string | null;
  goalReason: string | null;
  createdAt: string;
}

export interface MotivationQuote {
  id: number | null;
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

export interface MotivationFeedResponse {
  images: MotivationImageItem[];
  quote: MotivationQuote;
  recommendation: string;
}

export interface ReportMotivationImageRequest {
  reason: MotivationImageReportReason;
  comment?: string;
}

export interface ProfileStats {
  activeGoalsCount: number;
  completedGoalsCount: number;
  failedGoalsCount: number;
  totalFocusMinutes: number;
  bestStreak: number;
  averageDiscipline: number | null;
  riskSummary: RiskStatus | null;
}

export interface ProfileWallet {
  balance: number;
  initialBalance: number;
  totalPenalties: number;
  status: WalletStatus;
}

export interface ProfileActiveGoalItem {
  goalId: number;
  title: string;
  status: GoalStatus;
  currentStreak: number;
  dailyTargetMinutes: number | null;
  completedFocusMinutesToday: number;
  remainingMinutesToday: number | null;
  disciplineScore: number | null;
  riskStatus: RiskStatus | null;
  moneyEnabled: boolean;
  dailyPenaltyAmount: number;
  totalPenaltyCharged: number;
  moneyStatus: CommitmentMoneyStatus;
  createdAt: string;
}

export interface ProfileGoalHistoryItem {
  goalId: number;
  title: string;
  status: GoalStatus;
  failureReason: string | null;
  createdAt: string;
  completedAt: string | null;
  closedAt: string | null;
  totalPenaltyCharged: number;
  loserBadge: boolean;
}

export interface ProfileResponse {
  userId: number;
  email: string;
  fullName: string;
  avatarPath: string | null;
  stats: ProfileStats;
  wallet: ProfileWallet;
  activeGoals: ProfileActiveGoalItem[];
  goalHistory: ProfileGoalHistoryItem[];
}

export interface ProfileGoalsResponse {
  activeGoals: ProfileActiveGoalItem[];
  history: ProfileGoalHistoryItem[];
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

export interface WalletResponse {
  balance: number;
  initialBalance: number;
  totalAdded: number;
  totalPenalties: number;
  status: WalletStatus;
  createdAt: string;
  updatedAt: string;
}

export interface WalletTransaction {
  id: number;
  type: WalletTransactionType;
  amount: number;
  balanceBefore: number;
  balanceAfter: number;
  reason: string;
  goalTitle: string | null;
  createdAt: string;
}

export interface WalletTransactionHistory {
  transactions: WalletTransaction[];
}

export interface ApiError {
  code: string;
  message: string;
  details: string[];
  timestamp: string;
}
