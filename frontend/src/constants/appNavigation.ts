import { LayoutGrid, Microscope, TestTubeDiagonal, type LucideIcon } from 'lucide-react';

export type AppNavigationItem = {
  key: string;
  to: string;
  icon: LucideIcon;
};

export const APP_NAVIGATION_ITEMS: AppNavigationItem[] = [
  { key: 'dashboard', to: '/home', icon: LayoutGrid },
  { key: 'experiments', to: '/home/experiments', icon: TestTubeDiagonal },
  { key: 'laboratories', to: '/home/laboratories', icon: Microscope },
];
