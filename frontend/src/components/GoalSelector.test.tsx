import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { GoalSelector } from './GoalSelector';

describe('GoalSelector', () => {
  it('calls onSelect with selected goal', () => {
    const onSelect = vi.fn();

    render(
      <GoalSelector
        goals={[
          {
            id: 1,
            title: 'Goal 1',
            description: null,
            targetHours: null,
            deadline: null,
            themeColor: '#dff6e5',
            currentStreak: 0,
            createdAt: '2026-01-01T00:00:00Z'
          },
          {
            id: 2,
            title: 'Goal 2',
            description: null,
            targetHours: null,
            deadline: null,
            themeColor: '#d4e9ff',
            currentStreak: 0,
            createdAt: '2026-01-01T00:00:00Z'
          }
        ]}
        selectedGoalId={1}
        onSelect={onSelect}
      />
    );

    fireEvent.change(screen.getByRole('combobox'), { target: { value: '2' } });
    expect(onSelect).toHaveBeenCalledWith(2);
  });
});
