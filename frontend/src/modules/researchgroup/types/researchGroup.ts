export type ResearchGroupMemberRole = 'OWNER' | 'ADMIN' | 'ANNOTATOR';

export type ResearchGroupSummary = {
  id: number;
  name: string;
  description: string | null;
  role: ResearchGroupMemberRole;
  memberCount: number;
  createdAt: string;
};

export type ResearchGroupMember = {
  userId: number;
  firstName: string;
  lastName: string;
  email: string;
  role: ResearchGroupMemberRole;
};

export type ResearchGroupDetail = {
  id: number;
  name: string;
  description: string | null;
  totalMembers: number;
  activeProjects: number;
  createdAt: string;
  members: ResearchGroupMember[];
};
