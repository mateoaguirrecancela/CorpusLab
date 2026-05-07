import { type ResearchGroupMember } from '@/modules/researchgroup/types/researchGroup';

export function getResearchGroupMemberInitials(member: ResearchGroupMember): string {
  const firstInitial = member.firstName.trim().charAt(0).toUpperCase();
  const lastInitial = member.lastName.trim().charAt(0).toUpperCase();

  return `${firstInitial}${lastInitial}`.trim();
}
