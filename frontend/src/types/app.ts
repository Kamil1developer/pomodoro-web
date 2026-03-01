import type { Goal } from './api';

export interface AppShellContext {
  goals: Goal[];
  selectedGoal: Goal | null;
  selectedGoalId: number | null;
  setSelectedGoalId: (goalId: number | null) => void;
  reloadGoals: () => Promise<void>;
  logout: () => Promise<void>;
}
