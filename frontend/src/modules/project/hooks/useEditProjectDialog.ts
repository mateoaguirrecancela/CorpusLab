import { useCallback, useEffect, useMemo, useState } from 'react';
import { zodResolver } from '@hookform/resolvers/zod';
import { type FieldErrors, useForm, useWatch } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';
import {
  useDeleteProjectMutation,
  useProjectAssignmentContextQuery,
  useUpdateProjectMutation,
} from '@/modules/project/hooks/useProjectQueries';
import {
  getDeleteProjectErrorMessage,
  getProjectAssignmentContextErrorMessage,
  getUpdateProjectErrorMessage,
} from '@/modules/project/services/projectService';
import {
  createEditProjectSchema,
  type EditProjectFormValues,
} from '@/modules/project/schemas/projectFormSchemas';
import {
  type ProjectAssignableMember,
  type ProjectParticipantAssignment,
} from '@/modules/project/types/project';
import {
  assignmentsByUserId,
  assignmentsFromAssignmentMap,
  haveSameAssignments,
  nextAssignmentByUserId,
  type AssignmentByUserId,
} from '@/modules/project/utils/projectAssignmentUtils';
import {
  DIRTY_VALIDATED_FIELD_OPTIONS,
  hasText,
  toProjectPayload,
} from '@/modules/project/utils/projectFormUtils';

export type UseEditProjectDialogParams = Readonly<{
  controlledOpen?: boolean;
  groupId: number;
  initialDescription: string | null;
  initialName: string;
  initialParticipantAssignments: ProjectParticipantAssignment[];
  onDeleted?: () => void;
  onOpenChange?: (open: boolean) => void;
  projectId: number;
}>;

type UseEditProjectDialogResult = Readonly<{
  assignmentByUserId: AssignmentByUserId;
  canSave: boolean;
  confirmDeleteOpen: boolean;
  creatorUserId: number | null;
  description: string;
  errors: FieldErrors<EditProjectFormValues>;
  handleDeleteProject: () => Promise<void>;
  handleDeleteRequest: () => void;
  handleOpenChange: (nextOpen: boolean) => void;
  handleUpdateProject: () => Promise<void>;
  isBusy: boolean;
  isDeleting: boolean;
  isLoadingMembers: boolean;
  isUpdating: boolean;
  members: ProjectAssignableMember[];
  name: string;
  open: boolean;
  setConfirmDeleteOpen: (nextOpen: boolean) => void;
  setDescription: (nextDescription: string) => void;
  setName: (nextName: string) => void;
  toggleSelection: (userId: number) => void;
}>;

export function useEditProjectDialog({
  controlledOpen,
  groupId,
  initialDescription,
  initialName,
  initialParticipantAssignments,
  onDeleted,
  onOpenChange,
  projectId,
}: UseEditProjectDialogParams): UseEditProjectDialogResult {
  const { t } = useTranslation();
  const {
    data: assignmentContext,
    isLoading: isLoadingMembers,
    isError: isAssignmentContextError,
    error: assignmentContextError,
  } = useProjectAssignmentContextQuery(groupId);
  const updateProjectMutation = useUpdateProjectMutation();
  const deleteProjectMutation = useDeleteProjectMutation();
  const editProjectSchema = useMemo(() => createEditProjectSchema(t), [t]);
  const defaultFormValues = useMemo<EditProjectFormValues>(
    () => ({
      assignmentByUserId: assignmentsByUserId(initialParticipantAssignments),
      description: initialDescription ?? '',
      name: initialName,
    }),
    [initialDescription, initialName, initialParticipantAssignments],
  );
  const editProjectForm = useForm<EditProjectFormValues>({
    defaultValues: defaultFormValues,
    mode: 'onChange',
    resolver: zodResolver(editProjectSchema),
  });
  const {
    formState: { errors, isValid },
    control,
    getValues,
    reset,
    setValue,
  } = editProjectForm;

  const [internalOpen, setInternalOpen] = useState(false);
  const [confirmDeleteOpen, setConfirmDeleteOpen] = useState(false);
  const name = useWatch({ control, name: 'name' });
  const description = useWatch({ control, name: 'description' });
  const assignmentByUserId = useWatch({ control, name: 'assignmentByUserId' });

  const isUpdating = updateProjectMutation.isPending;
  const isDeleting = deleteProjectMutation.isPending;
  const isBusy = isUpdating || isDeleting;
  const open = controlledOpen ?? internalOpen;
  const creatorUserId = assignmentContext?.currentUserId ?? null;
  const members = assignmentContext?.members ?? [];

  const participantAssignments = useMemo(
    () => assignmentsFromAssignmentMap(assignmentByUserId),
    [assignmentByUserId],
  );
  const participantsChanged = useMemo(
    () => !haveSameAssignments(initialParticipantAssignments, participantAssignments),
    [initialParticipantAssignments, participantAssignments],
  );
  const hasChanges =
    name.trim() !== initialName.trim() ||
    description.trim() !== (initialDescription ?? '').trim() ||
    participantsChanged;
  const canSave = isValid && hasText(name) && hasChanges && !isBusy;

  const resetForm = useCallback(() => {
    reset(defaultFormValues);
  }, [defaultFormValues, reset]);

  const setDialogOpen = useCallback(
    (nextOpen: boolean) => {
      onOpenChange?.(nextOpen);

      if (controlledOpen === undefined) {
        setInternalOpen(nextOpen);
      }
    },
    [controlledOpen, onOpenChange],
  );

  useEffect(() => {
    if (!open) {
      resetForm();
    }
  }, [open, resetForm]);

  useEffect(() => {
    if (isAssignmentContextError) {
      toast.error(getProjectAssignmentContextErrorMessage(assignmentContextError), {
        id: `edit-project-assignment-context-${groupId}`,
      });
    }
  }, [assignmentContextError, groupId, isAssignmentContextError]);

  useEffect(() => {
    if (creatorUserId === null) {
      return;
    }

    const currentAssignments = getValues('assignmentByUserId');
    if (currentAssignments[creatorUserId]) {
      return;
    }

    setValue(
      'assignmentByUserId',
      {
        ...currentAssignments,
        [creatorUserId]: 'GROUP_A',
      },
      DIRTY_VALIDATED_FIELD_OPTIONS,
    );
  }, [creatorUserId, getValues, setValue]);

  const handleOpenChange = useCallback(
    (nextOpen: boolean) => {
      if (!nextOpen) {
        resetForm();
        setConfirmDeleteOpen(false);
      }

      setDialogOpen(nextOpen);
    },
    [resetForm, setDialogOpen],
  );

  const setName = useCallback(
    (nextName: string) => {
      setValue('name', nextName, DIRTY_VALIDATED_FIELD_OPTIONS);
    },
    [setValue],
  );

  const setDescription = useCallback(
    (nextDescription: string) => {
      setValue('description', nextDescription, DIRTY_VALIDATED_FIELD_OPTIONS);
    },
    [setValue],
  );

  const toggleSelection = useCallback(
    (userId: number) => {
      const currentAssignments = getValues('assignmentByUserId');
      setValue(
        'assignmentByUserId',
        nextAssignmentByUserId(currentAssignments, userId, creatorUserId),
        DIRTY_VALIDATED_FIELD_OPTIONS,
      );
    },
    [creatorUserId, getValues, setValue],
  );

  const handleDeleteRequest = useCallback(() => {
    setDialogOpen(false);
    setConfirmDeleteOpen(true);
  }, [setDialogOpen]);

  const handleUpdateProject = useCallback(async () => {
    if (!canSave) {
      return;
    }

    try {
      await updateProjectMutation.mutateAsync({
        groupId,
        projectId,
        payload: {
          ...toProjectPayload({ description, name }),
          participantAssignments,
        },
      });

      setDialogOpen(false);
      resetForm();
      toast.success(t('project.edit.updateSuccess'));
    } catch (error) {
      toast.error(getUpdateProjectErrorMessage(error));
    }
  }, [
    canSave,
    description,
    groupId,
    name,
    participantAssignments,
    projectId,
    resetForm,
    setDialogOpen,
    t,
    updateProjectMutation,
  ]);

  const handleDeleteProject = useCallback(async () => {
    if (isDeleting) {
      return;
    }

    try {
      await deleteProjectMutation.mutateAsync({ groupId, projectId });
      setConfirmDeleteOpen(false);
      setDialogOpen(false);
      toast.success(t('project.edit.deleteSuccess'));
      onDeleted?.();
    } catch (error) {
      toast.error(getDeleteProjectErrorMessage(error));
    }
  }, [deleteProjectMutation, groupId, isDeleting, onDeleted, projectId, setDialogOpen, t]);

  return {
    assignmentByUserId,
    canSave,
    confirmDeleteOpen,
    creatorUserId,
    description,
    errors,
    handleDeleteProject,
    handleDeleteRequest,
    handleOpenChange,
    handleUpdateProject,
    isBusy,
    isDeleting,
    isLoadingMembers,
    isUpdating,
    members,
    name,
    open,
    setConfirmDeleteOpen,
    setDescription,
    setName,
    toggleSelection,
  };
}
