import { type TFunction } from 'i18next';
import { z } from 'zod';
import { INVITABLE_RESEARCH_GROUP_MEMBER_ROLES } from '@/modules/researchgroup/types/researchGroup';

const invitableRoleSchema = z.enum(INVITABLE_RESEARCH_GROUP_MEMBER_ROLES);

export function createResearchGroupFormSchema(t: TFunction) {
  return z
    .object({
      description: z.string().max(2048, t('researchGroup.validation.descriptionMax')),
      name: z
        .string()
        .trim()
        .min(1, t('researchGroup.validation.nameRequired'))
        .max(256, t('researchGroup.validation.nameMax')),
    })
    .strict();
}

export function createInviteResearchGroupMemberSchema(t: TFunction) {
  return z
    .object({
      email: z
        .string()
        .trim()
        .min(1, t('researchGroup.validation.emailRequired'))
        .max(254, t('researchGroup.validation.emailMax'))
        .email(t('researchGroup.invite.invalidEmail')),
      role: invitableRoleSchema,
    })
    .strict();
}

export function createJoinResearchGroupByCodeSchema(t: TFunction) {
  return z
    .object({
      invitationCode: z
        .string()
        .trim()
        .min(1, t('researchGroup.validation.invitationCodeRequired'))
        .max(64, t('researchGroup.validation.invitationCodeMax')),
    })
    .strict();
}

export type InviteResearchGroupMemberFormValues = z.infer<
  ReturnType<typeof createInviteResearchGroupMemberSchema>
>;
export type JoinResearchGroupByCodeFormValues = z.infer<
  ReturnType<typeof createJoinResearchGroupByCodeSchema>
>;
export type ResearchGroupFormValues = z.infer<ReturnType<typeof createResearchGroupFormSchema>>;
