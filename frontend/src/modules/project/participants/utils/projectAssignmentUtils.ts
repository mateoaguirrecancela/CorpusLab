import {
  type ProjectAssignableMember,
  type ProjectParticipantAssignment,
  type ProjectParticipantAssignmentGroup,
} from '@/modules/project/shared/types/project';

export const PROJECT_ASSIGNMENT_GROUP_SEQUENCE: ReadonlyArray<ProjectParticipantAssignmentGroup | null> =
  ['GROUP_A', 'GROUP_B', null];

export type AssignmentByUserId = Record<string, ProjectParticipantAssignmentGroup>;

export function assignmentsByUserId(
  assignments: ProjectParticipantAssignment[],
): AssignmentByUserId {
  const result: AssignmentByUserId = {};

  for (const assignment of assignments) {
    result[assignment.userId] = assignment.iaaGroup;
  }

  return result;
}

export function assignmentGroupForUser(
  assignmentByUserId: AssignmentByUserId,
  userId: number,
  creatorUserId: number | null,
): ProjectParticipantAssignmentGroup | undefined {
  return assignmentByUserId[userId] ?? (userId === creatorUserId ? 'GROUP_A' : undefined);
}

export function nextAssignmentByUserId(
  currentAssignments: AssignmentByUserId,
  userId: number,
  creatorUserId: number | null,
): AssignmentByUserId {
  if (creatorUserId !== null && userId === creatorUserId) {
    const currentGroup = currentAssignments[userId] ?? 'GROUP_A';

    return {
      ...currentAssignments,
      [userId]: currentGroup === 'GROUP_A' ? 'GROUP_B' : 'GROUP_A',
    };
  }

  const currentGroup = currentAssignments[userId];
  const currentIndex = PROJECT_ASSIGNMENT_GROUP_SEQUENCE.indexOf(currentGroup ?? null);
  const nextGroup =
    PROJECT_ASSIGNMENT_GROUP_SEQUENCE[
      (currentIndex + 1) % PROJECT_ASSIGNMENT_GROUP_SEQUENCE.length
    ];
  const nextAssignments = { ...currentAssignments };

  if (nextGroup === null) {
    delete nextAssignments[userId];
  } else {
    nextAssignments[userId] = nextGroup;
  }

  return nextAssignments;
}

export function assignmentsFromAssignmentMap(
  assignmentByUserId: AssignmentByUserId,
): ProjectParticipantAssignment[] {
  return Object.entries(assignmentByUserId).map<ProjectParticipantAssignment>(
    ([userId, iaaGroup]) => ({
      userId: Number(userId),
      iaaGroup,
    }),
  );
}

export function participantAssignmentsFromMembers(
  members: ProjectAssignableMember[],
  assignmentByUserId: AssignmentByUserId,
  creatorUserId: number | null,
): ProjectParticipantAssignment[] {
  return members.flatMap<ProjectParticipantAssignment>((member) => {
    const iaaGroup = assignmentGroupForUser(assignmentByUserId, member.userId, creatorUserId);

    return iaaGroup ? [{ userId: member.userId, iaaGroup }] : [];
  });
}

export function normalizeAssignments(assignments: ProjectParticipantAssignment[]): string[] {
  return assignments
    .map((assignment) => `${assignment.userId}:${assignment.iaaGroup}`)
    .sort((left, right) => left.localeCompare(right));
}

export function haveSameAssignments(
  left: ProjectParticipantAssignment[],
  right: ProjectParticipantAssignment[],
): boolean {
  const normalizedLeft = normalizeAssignments(left);
  const normalizedRight = normalizeAssignments(right);

  return (
    normalizedLeft.length === normalizedRight.length &&
    normalizedLeft.every((assignment, index) => assignment === normalizedRight[index])
  );
}

export function memberInitials(firstName: string, lastName: string): string {
  const first = firstName.trim().charAt(0);
  const last = lastName.trim().charAt(0);
  return `${first}${last}`.toUpperCase();
}
