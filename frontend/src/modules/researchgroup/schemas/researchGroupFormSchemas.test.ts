import { describe, expect, it } from 'vitest';
import { type TFunction } from 'i18next';
import {
  createInviteResearchGroupMemberSchema,
  createJoinResearchGroupByCodeSchema,
  createResearchGroupFormSchema,
} from '@/modules/researchgroup/schemas/researchGroupFormSchemas';

const t = ((key: string) => key) as TFunction;

describe('createResearchGroupFormSchema', () => {
  const schema = createResearchGroupFormSchema(t);

  it('requires a non-empty name but allows an empty description', () => {
    expect(schema.safeParse({ name: 'Group', description: '' }).success).toBe(true);
    expect(schema.safeParse({ name: '  ', description: '' }).success).toBe(false);
  });

  it('enforces the max lengths', () => {
    expect(schema.safeParse({ name: 'a'.repeat(257), description: '' }).success).toBe(false);
    expect(schema.safeParse({ name: 'Group', description: 'a'.repeat(2049) }).success).toBe(false);
  });
});

describe('createInviteResearchGroupMemberSchema', () => {
  const schema = createInviteResearchGroupMemberSchema(t);

  it('accepts a valid invitable role', () => {
    expect(schema.safeParse({ email: 'a@b.com', role: 'ADMIN' }).success).toBe(true);
    expect(schema.safeParse({ email: 'a@b.com', role: 'ANNOTATOR' }).success).toBe(true);
  });

  it('rejects a non-invitable role such as OWNER', () => {
    expect(schema.safeParse({ email: 'a@b.com', role: 'OWNER' }).success).toBe(false);
  });

  it('rejects an invalid email', () => {
    expect(schema.safeParse({ email: 'not-an-email', role: 'ADMIN' }).success).toBe(false);
  });
});

describe('createJoinResearchGroupByCodeSchema', () => {
  const schema = createJoinResearchGroupByCodeSchema(t);

  it('requires a non-empty invitation code', () => {
    expect(schema.safeParse({ invitationCode: 'ABC123' }).success).toBe(true);
    expect(schema.safeParse({ invitationCode: '' }).success).toBe(false);
  });

  it('rejects a code longer than 64 characters', () => {
    expect(schema.safeParse({ invitationCode: 'a'.repeat(65) }).success).toBe(false);
  });
});
