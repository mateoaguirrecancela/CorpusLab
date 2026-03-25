import { LayoutGrid, Microscope, TestTubeDiagonal, type LucideIcon } from 'lucide-react'

export type AppNavigationItem = {
  key: string
  label: string
  to: string
  icon: LucideIcon
}

export const APP_NAVIGATION_ITEMS: AppNavigationItem[] = [
  { key: 'dashboard', label: 'Dashboard', to: '/home', icon: LayoutGrid },
  { key: 'experiments', label: 'Experiments', to: '/home/experiments', icon: TestTubeDiagonal },
  { key: 'laboratories', label: 'Laboratories', to: '/home/laboratories', icon: Microscope },
]
