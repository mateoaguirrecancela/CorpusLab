export type ResearchGroupMemberRole = 'OWNER' | 'ADMIN' | 'ANNOTATOR';

export type ResearchGroupSummary = {
  id: number;
  name: string;
  description: string | null;
  role: ResearchGroupMemberRole;
  memberCount: number;
  createdAt: string;
};
