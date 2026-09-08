import { describe, expect, it } from 'vitest';
import {
  buildResearchGroupFormValues,
  canSubmitInviteMember,
  canSubmitJoinByCode,
  canSubmitResearchGroupForm,
  hasResearchGroupFormChanges,
  toInviteResearchGroupMemberPayload,
  toJoinByCodePayload,
  toResearchGroupPayload,
} from '@/modules/researchgroup/utils/researchGroupForm';

describe('buildResearchGroupFormValues', () => {
  it('defaults a null/undefined description to an empty string', () => {
    expect(buildResearchGroupFormValues('Group', null)).toEqual({
      name: 'Group',
      description: '',
    });
    expect(buildResearchGroupFormValues('Group', undefined)).toEqual({
      name: 'Group',
      description: '',
    });
  });
});

describe('hasResearchGroupFormChanges', () => {
  it('is false when trimmed values match', () => {
    expect(
      hasResearchGroupFormChanges(
        { name: ' Group ', description: 'Desc' },
        { name: 'Group', description: 'Desc' },
      ),
    ).toBe(false);
  });

  it('is true when a field differs', () => {
    expect(
      hasResearchGroupFormChanges(
        { name: 'Group', description: 'Desc 2' },
        { name: 'Group', description: 'Desc' },
      ),
    ).toBe(true);
  });
});

describe('canSubmitResearchGroupForm', () => {
  it('requires a non-empty name, form validity and no pending submission', () => {
    const values = { name: 'Group', description: '' };
    expect(canSubmitResearchGroupForm({ values, isValid: true, isPending: false })).toBe(true);
    expect(
      canSubmitResearchGroupForm({
        values: { name: '  ', description: '' },
        isValid: true,
        isPending: false,
      }),
    ).toBe(false);
    expect(canSubmitResearchGroupForm({ values, isValid: false, isPending: false })).toBe(false);
    expect(canSubmitResearchGroupForm({ values, isValid: true, isPending: true })).toBe(false);
  });

  it('requires an actual change from initialValues when editing', () => {
    const initialValues = { name: 'Group', description: '' };
    expect(
      canSubmitResearchGroupForm({
        values: { ...initialValues },
        isValid: true,
        isPending: false,
        initialValues,
      }),
    ).toBe(false);
    expect(
      canSubmitResearchGroupForm({
        values: { name: 'Group 2', description: '' },
        isValid: true,
        isPending: false,
        initialValues,
      }),
    ).toBe(true);
  });
});

describe('canSubmitInviteMember', () => {
  it('requires email, role, validity and no pending submission', () => {
    expect(
      canSubmitInviteMember({ email: 'a@b.com', role: 'ADMIN', isValid: true, isPending: false }),
    ).toBe(true);
    expect(
      canSubmitInviteMember({ email: '', role: 'ADMIN', isValid: true, isPending: false }),
    ).toBe(false);
    expect(
      canSubmitInviteMember({ email: 'a@b.com', role: 'ADMIN', isValid: true, isPending: true }),
    ).toBe(false);
  });
});

describe('canSubmitJoinByCode', () => {
  it('requires a non-empty code', () => {
    expect(canSubmitJoinByCode({ invitationCode: 'ABC', isValid: true, isPending: false })).toBe(
      true,
    );
    expect(canSubmitJoinByCode({ invitationCode: ' ', isValid: true, isPending: false })).toBe(
      false,
    );
  });
});

describe('toResearchGroupPayload', () => {
  it('trims name and description, and omits an empty description', () => {
    expect(toResearchGroupPayload({ name: ' Group ', description: '  ' })).toEqual({
      name: 'Group',
      description: undefined,
    });
    expect(toResearchGroupPayload({ name: 'Group', description: ' Desc ' })).toEqual({
      name: 'Group',
      description: 'Desc',
    });
  });
});

describe('toInviteResearchGroupMemberPayload', () => {
  it('trims the email', () => {
    expect(toInviteResearchGroupMemberPayload({ email: ' a@b.com ', role: 'ADMIN' })).toEqual({
      email: 'a@b.com',
      role: 'ADMIN',
    });
  });
});

describe('toJoinByCodePayload', () => {
  it('trims the invitation code', () => {
    expect(toJoinByCodePayload({ invitationCode: ' ABC123 ' })).toBe('ABC123');
  });
});
