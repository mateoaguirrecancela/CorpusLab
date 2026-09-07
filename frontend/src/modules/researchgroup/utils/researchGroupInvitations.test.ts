import { describe, expect, it } from 'vitest';
import {
  getInvitationListStatus,
  getPendingInvitationId,
} from '@/modules/researchgroup/utils/researchGroupInvitations';
import { type ResearchGroupInvitation } from '@/modules/researchgroup/types/researchGroup';

const invitation: ResearchGroupInvitation = {
  id: 1,
  researchGroupId: 1,
  researchGroupName: 'Group',
  invitedEmail: 'a@b.com',
  inviterFullName: 'Jane Doe',
  role: 'ANNOTATOR',
  status: 'PENDING',
  createdAt: '2026-01-01T00:00:00Z',
  expiresAt: '2026-02-01T00:00:00Z',
};

describe('getInvitationListStatus', () => {
  it('prioritizes loading, then error, then empty/ready', () => {
    expect(getInvitationListStatus({ invitations: [], isError: true, isLoading: true })).toBe(
      'loading',
    );
    expect(getInvitationListStatus({ invitations: [], isError: true, isLoading: false })).toBe(
      'error',
    );
    expect(getInvitationListStatus({ invitations: [], isError: false, isLoading: false })).toBe(
      'empty',
    );
    expect(
      getInvitationListStatus({ invitations: [invitation], isError: false, isLoading: false }),
    ).toBe('ready');
  });
});

describe('getPendingInvitationId', () => {
  it('prefers the accepting id over the declining id', () => {
    expect(getPendingInvitationId(1, 2)).toBe(1);
  });

  it('falls back to the declining id', () => {
    expect(getPendingInvitationId(undefined, 2)).toBe(2);
  });

  it('is undefined when neither is set', () => {
    expect(getPendingInvitationId(undefined, undefined)).toBeUndefined();
  });
});
