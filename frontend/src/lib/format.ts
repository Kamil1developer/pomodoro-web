export function minutesToHours(minutes: number): string {
  const hours = minutes / 60;
  return `${hours.toFixed(1)}h`;
}

export function toPercent(completed: number, all: number): number {
  if (all <= 0) {
    return 0;
  }
  return Math.round((completed / all) * 100);
}

export function shortDate(value: string): string {
  return new Date(value).toLocaleDateString();
}

export function shortDateTime(value: string): string {
  return new Date(value).toLocaleString();
}
