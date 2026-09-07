import { describe, expect, it } from 'vitest';
import {
  assignmentGroupForUser,
  assignmentsByUserId,
  assignmentsFromAssignmentMap,
  haveSameAssignments,
  memberInitials,
  nextAssignmentByUserId,
  normalizeAssignments,
  participantAssignmentsFromMembers,
} from '@/modules/project/participants/utils/projectAssignmentUtils';
import { type ProjectAssignableMember } from '@/modules/project/shared/types/project';

describe('assignmentsByUserId', () => {
  it('indexes assignments by user id', () => {
    expect(
      assignmentsByUserId([
        { userId: 1, iaaGroup: 'GROUP_A' },
        { userId: 2, iaaGroup: 'GROUP_B' },
      ]),
    ).toEqual({ 1: 'GROUP_A', 2: 'GROUP_B' });
  });
});

describe('assignmentGroupForUser', () => {
  it('returns the explicit assignment when present', () => {
    expect(assignmentGroupForUser({ 1: 'GROUP_B' }, 1, null)).toBe('GROUP_B');
  });

  it('defaults the creator to GROUP_A when unassigned', () => {
    expect(assignmentGroupForUser({}, 5, 5)).toBe('GROUP_A');
  });

  it('is undefined for a non-creator with no assignment', () => {
    expect(assignmentGroupForUser({}, 5, 9)).toBeUndefined();
  });
});

describe('nextAssignmentByUserId', () => {
  it('toggles the creator between GROUP_A and GROUP_B only', () => {
    expect(nextAssignmentByUserId({}, 1, 1)).toEqual({ 1: 'GROUP_B' });
    expect(nextAssignmentByUserId({ 1: 'GROUP_B' }, 1, 1)).toEqual({ 1: 'GROUP_A' });
  });

  it('cycles a regular user through GROUP_A -> GROUP_B -> unassigned', () => {
    let assignments = nextAssignmentByUserId({}, 2, 1);
    expect(assignments).toEqual({ 2: 'GROUP_A' });

    assignments = nextAssignmentByUserId(assignments, 2, 1);
    expect(assignments).toEqual({ 2: 'GROUP_B' });

    assignments = nextAssignmentByUserId(assignments, 2, 1);
    expect(assignments).toEqual({});
  });

  it('does not mutate the input map', () => {
    const original = { 2: 'GROUP_A' as const };
    nextAssignmentByUserId(original, 2, 1);
    expect(original).toEqual({ 2: 'GROUP_A' });
  });
});

describe('assignmentsFromAssignmentMap', () => {
  it('converts the map back into an assignment list with numeric ids', () => {
    expect(assignmentsFromAssignmentMap({ 1: 'GROUP_A', 2: 'GROUP_B' })).toEqual([
      { userId: 1, iaaGroup: 'GROUP_A' },
      { userId: 2, iaaGroup: 'GROUP_B' },
    ]);
  });
});

describe('participantAssignmentsFromMembers', () => {
  const members: ProjectAssignableMember[] = [
    { userId: 1, firstName: 'A', lastName: 'A', email: 'a@a.com' },
    { userId: 2, firstName: 'B', lastName: 'B', email: 'b@b.com' },
  ];

  it('includes only members that resolve to an assignment group', () => {
    expect(participantAssignmentsFromMembers(members, { 1: 'GROUP_A' }, 2)).toEqual([
      { userId: 1, iaaGroup: 'GROUP_A' },
      { userId: 2, iaaGroup: 'GROUP_A' },
    ]);
  });

  it('excludes members with no assignment and no creator default', () => {
    expect(participantAssignmentsFromMembers(members, {}, null)).toEqual([]);
  });
});

describe('normalizeAssignments / haveSameAssignments', () => {
  it('normalizes into sorted, comparable strings', () => {
    expect(
      normalizeAssignments([
        { userId: 2, iaaGroup: 'GROUP_B' },
        { userId: 1, iaaGroup: 'GROUP_A' },
      ]),
    ).toEqual(['1:GROUP_A', '2:GROUP_B']);
  });

  it('treats differently-ordered but equal assignment lists as the same', () => {
    expect(
      haveSameAssignments(
        [
          { userId: 1, iaaGroup: 'GROUP_A' },
          { userId: 2, iaaGroup: 'GROUP_B' },
        ],
        [
          { userId: 2, iaaGroup: 'GROUP_B' },
          { userId: 1, iaaGroup: 'GROUP_A' },
        ],
      ),
    ).toBe(true);
  });

  it('detects a difference in group or membership', () => {
    expect(
      haveSameAssignments(
        [{ userId: 1, iaaGroup: 'GROUP_A' }],
        [{ userId: 1, iaaGroup: 'GROUP_B' }],
      ),
    ).toBe(false);
    expect(
      haveSameAssignments(
        [{ userId: 1, iaaGroup: 'GROUP_A' }],
        [
          { userId: 1, iaaGroup: 'GROUP_A' },
          { userId: 2, iaaGroup: 'GROUP_A' },
        ],
      ),
    ).toBe(false);
  });
});

describe('memberInitials', () => {
  it('builds uppercase initials, trimming whitespace', () => {
    expect(memberInitials(' jane', 'doe ')).toBe('JD');
  });
});
