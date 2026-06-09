import {
  type ResearchGroupMember,
  type ResearchGroupProjectParticipantRole,
} from '@/modules/researchgroup/types/researchGroup';

export function getResearchGroupMemberInitials(member: ResearchGroupMember): string {
  const firstInitial = member.firstName.trim().charAt(0).toUpperCase();
  const lastInitial = member.lastName.trim().charAt(0).toUpperCase();

  return `${firstInitial}${lastInitial}`.trim();
}

export function researchGroupProjectParticipantRoleI18nKey(
  role: ResearchGroupProjectParticipantRole,
): string {
  if (role === 'CREATOR') {
    return 'project.list.roles.creator';
  }

  return 'project.list.roles.participant';
}
