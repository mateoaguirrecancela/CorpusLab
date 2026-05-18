import { useCallback, useEffect, useMemo, useState } from 'react';
import { toast } from 'sonner';
import { useProjectAssignmentContextQuery } from '@/modules/project/hooks/useProjectQueries';
import { getProjectAssignmentContextErrorMessage } from '@/modules/project/services/projectService';
import {
  type ProjectAssignableMember,
  type ProjectParticipantAssignment,
} from '@/modules/project/types/project';
import {
  assignmentsByUserId,
  nextAssignmentByUserId,
  participantAssignmentsFromMembers,
  type AssignmentByUserId,
} from '@/modules/project/utils/projectAssignmentUtils';

type UseProjectAssignmentStepParams = Readonly<{
  groupId: number;
  onCompleted: (participantAssignments: ProjectParticipantAssignment[]) => void | Promise<void>;
}>;

type UseProjectAssignmentStepResult = Readonly<{
  assignmentByUserId: AssignmentByUserId;
  creatorUserId: number | null;
  handleAssign: () => Promise<void>;
  isError: boolean;
  isLoading: boolean;
  members: ProjectAssignableMember[];
  toggleSelection: (userId: number) => void;
}>;

export function useProjectAssignmentStep({
  groupId,
  onCompleted,
}: UseProjectAssignmentStepParams): UseProjectAssignmentStepResult {
  const {
    data: assignmentContext,
    isLoading,
    isError,
    error,
  } = useProjectAssignmentContextQuery(groupId);
  const [assignmentByUserId, setAssignmentByUserId] = useState<AssignmentByUserId>({});

  const creatorUserId = assignmentContext?.currentUserId ?? null;
  const members = useMemo(() => assignmentContext?.members ?? [], [assignmentContext?.members]);
  const defaultAssignmentByUserId = useMemo(
    () => assignmentsByUserId(assignmentContext?.defaultAssignments ?? []),
    [assignmentContext?.defaultAssignments],
  );
  const effectiveAssignmentByUserId = useMemo<AssignmentByUserId>(
    () => ({
      ...defaultAssignmentByUserId,
      ...assignmentByUserId,
    }),
    [assignmentByUserId, defaultAssignmentByUserId],
  );

  useEffect(() => {
    if (isError) {
      toast.error(getProjectAssignmentContextErrorMessage(error), {
        id: `project-assignment-context-${groupId}`,
      });
    }
  }, [error, groupId, isError]);

  const participantAssignments = useMemo(
    () => participantAssignmentsFromMembers(members, effectiveAssignmentByUserId, creatorUserId),
    [creatorUserId, effectiveAssignmentByUserId, members],
  );

  const toggleSelection = useCallback(
    (userId: number) => {
      setAssignmentByUserId((previous) => nextAssignmentByUserId(previous, userId, creatorUserId));
    },
    [creatorUserId],
  );

  const handleAssign = useCallback(async () => {
    await onCompleted(participantAssignments);
  }, [onCompleted, participantAssignments]);

  return {
    assignmentByUserId: effectiveAssignmentByUserId,
    creatorUserId,
    handleAssign,
    isError,
    isLoading,
    members,
    toggleSelection,
  };
}
