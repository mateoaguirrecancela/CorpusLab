import { describe, expect, it } from 'vitest';
import {
  getResearchGroupMemberInitials,
  researchGroupProjectParticipantRoleI18nKey,
} from '@/modules/researchgroup/utils/researchGroupUtils';

describe('getResearchGroupMemberInitials', () => {
  it('builds uppercase initials from first and last name', () => {
    expect(getResearchGroupMemberInitials({ firstName: 'jane', lastName: 'doe' } as never)).toBe(
      'JD',
    );
  });

  it('trims whitespace before taking the initial', () => {
    expect(
      getResearchGroupMemberInitials({ firstName: '  jane', lastName: '  doe' } as never),
    ).toBe('JD');
  });
});

describe('researchGroupProjectParticipantRoleI18nKey', () => {
  it('maps CREATOR to the creator key', () => {
    expect(researchGroupProjectParticipantRoleI18nKey('CREATOR')).toBe(
      'project.list.roles.creator',
    );
  });

  it('maps any other role to the participant key', () => {
    expect(researchGroupProjectParticipantRoleI18nKey('PARTICIPANT')).toBe(
      'project.list.roles.participant',
    );
  });
});
