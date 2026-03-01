import { useOutletContext } from 'react-router-dom';
import type { AppShellContext } from '../types/app';

export function useAppShellContext() {
  return useOutletContext<AppShellContext>();
}
