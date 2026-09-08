import { create } from 'zustand';

type ResearchGroupUIState = {
  archivedProjectsByGroupId: Record<number, boolean>;
  toggleArchivedProjects: (groupId: number) => void;
};

export const useResearchGroupUIStore = create<ResearchGroupUIState>((set) => ({
  archivedProjectsByGroupId: {},
  toggleArchivedProjects: (groupId) =>
    set((state) => ({
      archivedProjectsByGroupId: {
        ...state.archivedProjectsByGroupId,
        [groupId]: !(state.archivedProjectsByGroupId[groupId] ?? false),
      },
    })),
}));
