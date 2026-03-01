import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { ProgressCard } from './ProgressCard';

describe('ProgressCard', () => {
  it('renders progress values', () => {
    render(
      <ProgressCard
        progress={{
          completedTasks: 2,
          allTasks: 4,
          totalFocusMinutes: 120,
          currentStreak: 3
        }}
      />
    );

    expect(screen.getByText('50%')).toBeInTheDocument();
    expect(screen.getByText('2/4')).toBeInTheDocument();
    expect(screen.getByText('2.0h')).toBeInTheDocument();
    expect(screen.getByText('3 дн.')).toBeInTheDocument();
  });
});
