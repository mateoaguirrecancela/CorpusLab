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
  activeProjectsCount: number;
};

export type ResearchGroupDetail = {
  id: number;
  name: string;
  description: string | null;
  invitationCode: string;
  totalMembers: number;
  activeProjects: number;
  createdAt: string;
  members: ResearchGroupMember[];
};

export type InviteResearchGroupMemberPayload = {
  email: string;
  role: ResearchGroupMemberRole;
  expiresAt: string;
};

export type UpdateResearchGroupPayload = {
  name: string;
  description?: string;
};

export type UpdateResearchGroupMemberRolePayload = {
  role: ResearchGroupMemberRole;
};

export type ResearchGroupInvitationStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'EXPIRED';

export type ResearchGroupInvitation = {
  id: number;
  researchGroupId: number;
  researchGroupName: string;
  invitedEmail: string;
  inviterFullName: string;
  role: ResearchGroupMemberRole;
  status: ResearchGroupInvitationStatus;
  createdAt: string;
  expiresAt: string;
};
