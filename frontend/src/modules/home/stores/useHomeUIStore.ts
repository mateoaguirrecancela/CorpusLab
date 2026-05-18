import { create } from 'zustand';

type HomeUIState = {
  isSidebarCollapsed: boolean;
  closeSidebar: () => void;
  toggleSidebar: () => void;
};

export const useHomeUIStore = create<HomeUIState>((set) => ({
  isSidebarCollapsed: false,
  closeSidebar: () => set({ isSidebarCollapsed: true }),
  toggleSidebar: () =>
    set((state) => ({
      isSidebarCollapsed: !state.isSidebarCollapsed,
    })),
}));
