import { create } from 'zustand';

type AppShellState = {
  isSidebarCollapsed: boolean;
  closeSidebar: () => void;
  toggleSidebar: () => void;
};

export const useAppShellStore = create<AppShellState>((set) => ({
  isSidebarCollapsed: false,
  closeSidebar: () => set({ isSidebarCollapsed: true }),
  toggleSidebar: () =>
    set((state) => ({
      isSidebarCollapsed: !state.isSidebarCollapsed,
    })),
}));
