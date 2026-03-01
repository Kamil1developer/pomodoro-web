import type { Goal } from '../types/api';

interface GoalSelectorProps {
  goals: Goal[];
  selectedGoalId: number | null;
  onSelect: (goalId: number) => void;
}

export function GoalSelector({ goals, selectedGoalId, onSelect }: GoalSelectorProps) {
  return (
    <label className="goal-selector">
      <span>Активная цель</span>
      <select
        value={selectedGoalId ?? ''}
        onChange={(event) => onSelect(Number(event.target.value))}
        disabled={goals.length === 0}
      >
        {goals.length === 0 ? <option value="">Сначала создайте цель</option> : null}
        {goals.map((goal) => (
          <option key={goal.id} value={goal.id}>
            {goal.title}
          </option>
        ))}
      </select>
    </label>
  );
}
