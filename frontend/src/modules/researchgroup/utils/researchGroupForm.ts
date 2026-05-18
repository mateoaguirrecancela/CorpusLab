import {
  type InviteResearchGroupMemberFormValues,
  type JoinResearchGroupByCodeFormValues,
  type ResearchGroupFormValues,
} from '@/modules/researchgroup/schemas/researchGroupFormSchemas';
import {
  type InviteResearchGroupMemberPayload,
  INVITABLE_RESEARCH_GROUP_MEMBER_ROLES,
  type UpdateResearchGroupPayload,
} from '@/modules/researchgroup/types/researchGroup';
import { type CreateResearchGroupPayload } from '@/modules/researchgroup/types/createResearchGroup';

export const EMPTY_RESEARCH_GROUP_FORM: ResearchGroupFormValues = {
  description: '',
  name: '',
};

export const DEFAULT_INVITE_MEMBER_FORM: InviteResearchGroupMemberFormValues = {
  email: '',
  role: 'ANNOTATOR',
};

export const DEFAULT_JOIN_BY_CODE_FORM: JoinResearchGroupByCodeFormValues = {
  invitationCode: '',
};

export const DIRTY_VALIDATED_FIELD_OPTIONS = {
  shouldDirty: true,
  shouldValidate: true,
} as const;

export const INVITABLE_ROLES = INVITABLE_RESEARCH_GROUP_MEMBER_ROLES;

const RESEARCH_GROUP_FORM_TEXT_FIELDS = ['name', 'description'] as const;

type SubmitState = {
  isPending: boolean;
  isValid: boolean;
};

type ResearchGroupFormSubmitState = SubmitState & {
  values: ResearchGroupFormValues;
  initialValues?: ResearchGroupFormValues;
};

type InviteMemberSubmitState = SubmitState & {
  email: string;
  role: string;
};

type JoinByCodeSubmitState = SubmitState & {
  invitationCode: string;
};

type ResearchGroupPayloadInput = {
  description?: string | null;
  name: string;
};

function hasText(value: string): boolean {
  return value.trim().length > 0;
}

function canSubmitRequiredFields(
  { isPending, isValid }: SubmitState,
  ...values: string[]
): boolean {
  return isValid && !isPending && values.every(hasText);
}

export function buildResearchGroupFormValues(
  name: string,
  description: string | null | undefined,
): ResearchGroupFormValues {
  return {
    description: description ?? '',
    name,
  };
}

export function hasResearchGroupFormChanges(
  values: ResearchGroupFormValues,
  initialValues: ResearchGroupFormValues,
): boolean {
  return RESEARCH_GROUP_FORM_TEXT_FIELDS.some(
    (field) => values[field].trim() !== initialValues[field].trim(),
  );
}

export function canSubmitResearchGroupForm({
  initialValues,
  isPending,
  isValid,
  values,
}: ResearchGroupFormSubmitState): boolean {
  if (!canSubmitRequiredFields({ isPending, isValid }, values.name)) {
    return false;
  }

  return !initialValues || hasResearchGroupFormChanges(values, initialValues);
}

export function canSubmitInviteMember({
  email,
  isPending,
  isValid,
  role,
}: InviteMemberSubmitState): boolean {
  return canSubmitRequiredFields({ isPending, isValid }, email, role);
}

export function canSubmitJoinByCode({
  invitationCode,
  isPending,
  isValid,
}: JoinByCodeSubmitState): boolean {
  return canSubmitRequiredFields({ isPending, isValid }, invitationCode);
}

export function toResearchGroupPayload(
  values: ResearchGroupPayloadInput,
): CreateResearchGroupPayload & UpdateResearchGroupPayload {
  const description = values.description?.trim() ?? '';

  return {
    name: values.name.trim(),
    description: description || undefined,
  };
}

export function toInviteResearchGroupMemberPayload({
  email,
  role,
}: InviteResearchGroupMemberPayload): InviteResearchGroupMemberPayload {
  return {
    email: email.trim(),
    role,
  };
}

export function toJoinByCodePayload(values: JoinResearchGroupByCodeFormValues): string {
  return values.invitationCode.trim();
}
