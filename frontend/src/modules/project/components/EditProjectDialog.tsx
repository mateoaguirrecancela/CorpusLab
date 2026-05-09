import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';
import { ConfirmDestructiveDialog } from '@/components/common/ConfirmDestructiveDialog';
import { FormFieldControl } from '@/components/common/FormFieldControl';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { Spinner } from '@/components/ui/spinner';
import { cn } from '@/lib/utils';
import { useProfileQuery } from '@/modules/auth/hooks/useProfileQuery';
import {
  useDeleteProjectMutation,
  useUpdateProjectMutation,
} from '@/modules/project/hooks/useProjectQueries';
import {
  getDeleteProjectErrorMessage,
  getUpdateProjectErrorMessage,
} from '@/modules/project/services/projectService';
import {
  type ProjectParticipantAssignment,
  type ProjectParticipantAssignmentGroup,
} from '@/modules/project/types/project';
import { useResearchGroupDetailQuery } from '@/modules/researchgroup/hooks/useResearchGroupQueries';

type EditProjectDialogProps = {
  groupId: number;
  projectId: number;
  initialName: string;
  initialDescription: string | null;
  initialParticipantAssignments: ProjectParticipantAssignment[];
  trigger?: React.ReactNode;
  showDeleteButton?: boolean;
  onDeleted?: () => void;
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
};

const GROUP_SEQUENCE: ReadonlyArray<ProjectParticipantAssignmentGroup | null> = [
  'GROUP_A',
  'GROUP_B',
  null,
];

function assignmentsByUserId(
  assignments: ProjectParticipantAssignment[],
): Record<number, ProjectParticipantAssignmentGroup> {
  return assignments.reduce<Record<number, ProjectParticipantAssignmentGroup>>(
    (accumulator, assignment) => {
      accumulator[assignment.userId] = assignment.iaaGroup;
      return accumulator;
    },
    {},
  );
}

function normalizeAssignments(assignments: ProjectParticipantAssignment[]): string[] {
  return assignments
    .map((assignment) => `${assignment.userId}:${assignment.iaaGroup}`)
    .sort((left, right) => left.localeCompare(right));
}

function membersSelectionCardClassName(
  group: ProjectParticipantAssignmentGroup | undefined,
): string {
  if (group === 'GROUP_A') {
    return 'border-primary bg-primary/5 shadow-sm shadow-primary/10 ring-2 ring-primary/20';
  }

  if (group === 'GROUP_B') {
    return 'border-sky-500/70 bg-sky-50 shadow-sm shadow-sky-500/10 ring-2 ring-sky-500/20';
  }

  return 'border-border bg-background hover:border-primary/40 hover:bg-accent/30';
}

function groupBadgeClassName(group: ProjectParticipantAssignmentGroup): string {
  return group === 'GROUP_A' ? 'bg-primary/10 text-primary' : 'bg-sky-100 text-sky-700';
}

function initials(firstName: string, lastName: string): string {
  const first = firstName.trim().charAt(0);
  const last = lastName.trim().charAt(0);
  return `${first}${last}`.toUpperCase();
}

export function EditProjectDialog({
  groupId,
  projectId,
  initialName,
  initialDescription,
  initialParticipantAssignments,
  trigger,
  showDeleteButton = false,
  onDeleted,
  open: controlledOpen,
  onOpenChange,
}: Readonly<EditProjectDialogProps>) {
  const { t } = useTranslation();
  const { data: profile } = useProfileQuery();
  const { data: group, isLoading: isLoadingMembers } = useResearchGroupDetailQuery(groupId);

  const updateProjectMutation = useUpdateProjectMutation();
  const deleteProjectMutation = useDeleteProjectMutation();

  const [internalOpen, setInternalOpen] = useState(false);
  const [confirmDeleteOpen, setConfirmDeleteOpen] = useState(false);
  const [name, setName] = useState(initialName);
  const [description, setDescription] = useState(initialDescription ?? '');
  const [assignmentByUserId, setAssignmentByUserId] = useState<
    Record<number, ProjectParticipantAssignmentGroup>
  >(assignmentsByUserId(initialParticipantAssignments));

  const isUpdating = updateProjectMutation.isPending;
  const isDeleting = deleteProjectMutation.isPending;
  const isBusy = isUpdating || isDeleting;
  const open = controlledOpen ?? internalOpen;

  const profileEmail = profile?.email?.toLowerCase() ?? null;
  const members = group?.members ?? [];
  const creatorMember = useMemo(
    () =>
      profileEmail === null
        ? null
        : members.find((member) => member.email.toLowerCase() === profileEmail) ?? null,
    [members, profileEmail],
  );
  const creatorUserId = creatorMember?.userId ?? null;
  const orderedMembers = useMemo(() => {
    if (creatorUserId === null) {
      return members;
    }

    const creatorFirst: typeof members = [];
    const rest: typeof members = [];
    for (const member of members) {
      if (member.userId === creatorUserId) {
        creatorFirst.push(member);
      } else {
        rest.push(member);
      }
    }

    return [...creatorFirst, ...rest];
  }, [creatorUserId, members]);

  const participantAssignments = useMemo(
    () =>
      Object.entries(assignmentByUserId).map<ProjectParticipantAssignment>(
        ([userId, iaaGroup]) => ({
          userId: Number(userId),
          iaaGroup,
        }),
      ),
    [assignmentByUserId],
  );

  const normalizedInitialAssignments = useMemo(
    () => normalizeAssignments(initialParticipantAssignments),
    [initialParticipantAssignments],
  );

  const participantsChanged = useMemo(() => {
    const normalizedSelectedAssignments = normalizeAssignments(participantAssignments);

    if (normalizedSelectedAssignments.length !== normalizedInitialAssignments.length) {
      return true;
    }

    return normalizedSelectedAssignments.some(
      (participantAssignment, index) =>
        participantAssignment !== normalizedInitialAssignments[index],
    );
  }, [normalizedInitialAssignments, participantAssignments]);

  const hasChanges =
    name.trim() !== initialName.trim() ||
    description.trim() !== (initialDescription ?? '').trim() ||
    participantsChanged;

  const canSave = name.trim().length > 0 && hasChanges && !isBusy;

  const resetForm = () => {
    setName(initialName);
    setDescription(initialDescription ?? '');
    setAssignmentByUserId(assignmentsByUserId(initialParticipantAssignments));
  };

  useEffect(() => {
    if (!open) {
      resetForm();
    }
  }, [initialName, initialDescription, initialParticipantAssignments, open]);

  useEffect(() => {
    if (creatorUserId === null) {
      return;
    }

    setAssignmentByUserId((prev) => {
      if (prev[creatorUserId]) {
        return prev;
      }

      return {
        ...prev,
        [creatorUserId]: 'GROUP_A',
      };
    });
  }, [creatorUserId]);

  const toggleSelection = (userId: number) => {
    setAssignmentByUserId((prev) => {
      if (creatorUserId !== null && userId === creatorUserId) {
        const currentGroup = prev[userId] ?? 'GROUP_A';
        const nextAssignments = { ...prev };
        nextAssignments[userId] = currentGroup === 'GROUP_A' ? 'GROUP_B' : 'GROUP_A';
        return nextAssignments;
      }

      const currentGroup = prev[userId];
      const currentIndex = GROUP_SEQUENCE.indexOf(currentGroup ?? null);
      const nextGroup = GROUP_SEQUENCE[(currentIndex + 1) % GROUP_SEQUENCE.length];
      const nextAssignments = { ...prev };

      if (nextGroup === null) {
        delete nextAssignments[userId];
      } else {
        nextAssignments[userId] = nextGroup;
      }

      return nextAssignments;
    });
  };

  const setDialogOpen = (nextOpen: boolean) => {
    onOpenChange?.(nextOpen);

    if (controlledOpen === undefined) {
      setInternalOpen(nextOpen);
    }
  };

  const handleOpenChange = (nextOpen: boolean) => {
    if (!nextOpen) {
      resetForm();
      setConfirmDeleteOpen(false);
    }

    setDialogOpen(nextOpen);
  };

  const handleUpdateProject = async () => {
    if (!canSave) {
      return;
    }

    try {
      await updateProjectMutation.mutateAsync({
        groupId,
        projectId,
        payload: {
          name: name.trim(),
          description: description.trim() || undefined,
          participantAssignments,
        },
      });

      setDialogOpen(false);
      resetForm();
      toast.success(t('project.edit.updateSuccess'));
    } catch (error) {
      toast.error(getUpdateProjectErrorMessage(error));
    }
  };

  const handleDeleteProject = async () => {
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
  };

  return (
    <>
      <Dialog open={open} onOpenChange={handleOpenChange}>
        {trigger ? <DialogTrigger render={trigger as React.JSX.Element} /> : undefined}

        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>{t('project.edit.title')}</DialogTitle>
          </DialogHeader>

          <div className="my-8 space-y-5">
            <FormFieldControl
              id="edit-project-name"
              inputProps={{
                autoFocus: true,
                maxLength: 256,
                placeholder: t('project.edit.namePlaceholder'),
                required: true,
              }}
              label={t('project.edit.nameLabel')}
              onValueChange={setName}
              required
              value={name}
            />

            <FormFieldControl
              controlType="textarea"
              id="edit-project-description"
              label={t('project.edit.descriptionLabel')}
              onValueChange={setDescription}
              textareaProps={{
                maxLength: 2048,
                placeholder: t('project.edit.descriptionPlaceholder'),
              }}
              value={description}
            />

            <div className="space-y-3">
              <p className="text-sm font-semibold text-primary">
                {t('project.edit.participantsLabel')}
              </p>

              {isLoadingMembers && (
                <div className="rounded-md border border-border bg-background px-4 py-6 text-sm text-muted-foreground">
                  <span className="inline-flex items-center gap-2">
                    <Spinner aria-hidden className="size-4" />
                    {t('project.create.loadingMembers')}
                  </span>
                </div>
              )}

              {!isLoadingMembers && orderedMembers.length === 0 && (
                <p className="rounded-md border border-dashed border-border bg-background px-4 py-5 text-sm text-muted-foreground">
                  {t('project.create.noAssignableMembers')}
                </p>
              )}

              {!isLoadingMembers && orderedMembers.length > 0 && (
                <div className="grid gap-3 sm:grid-cols-2">
                  {orderedMembers.map((member) => {
                    const group =
                      assignmentByUserId[member.userId] ??
                      (member.userId === creatorUserId ? 'GROUP_A' : undefined);
                    const memberName = `${member.firstName} ${member.lastName}`;

                    return (
                      <button
                        aria-label={t('project.create.assignmentCycleLabel', {
                          name: memberName,
                          state: group
                            ? t(`project.create.assignmentGroups.${group}`)
                            : t('project.create.assignmentGroups.unassigned'),
                        })}
                        className={cn(
                          'rounded-md border p-4 text-left transition-all cursor-pointer',
                          membersSelectionCardClassName(group),
                        )}
                        key={member.userId}
                        onClick={() => toggleSelection(member.userId)}
                        type="button"
                      >
                        <div className="flex items-center gap-3">
                          <div className="flex size-10 items-center justify-center rounded-full bg-primary/10 text-xs font-bold text-primary">
                            {initials(member.firstName, member.lastName)}
                          </div>
                          <div className="min-w-0 flex-1">
                            <p className="truncate text-sm font-semibold text-primary">
                              {memberName}
                            </p>
                            <p className="truncate text-xs text-muted-foreground">{member.email}</p>
                          </div>
                          {group ? (
                            <span
                              className={cn(
                                'shrink-0 rounded-full px-2.5 py-1 text-xs font-bold',
                                groupBadgeClassName(group),
                              )}
                            >
                              {t(`project.create.assignmentGroups.${group}`)}
                            </span>
                          ) : null}
                        </div>
                      </button>
                    );
                  })}
                </div>
              )}
            </div>
          </div>

          <DialogFooter>
            {showDeleteButton ? (
              <Button
                className="h-10 min-w-32 rounded-md bg-destructive text-sm font-semibold text-white transition-colors hover:brightness-95 disabled:cursor-not-allowed disabled:opacity-70 cursor-pointer"
                disabled={isBusy}
                onClick={() => {
                  setDialogOpen(false);
                  setConfirmDeleteOpen(true);
                }}
                type="button"
              >
                {t('researchGroup.edit.delete')}
              </Button>
            ) : undefined}
            <Button
              className="h-10 min-w-32 rounded-md bg-primary text-sm font-semibold text-white transition-colors hover:bg-primary-strong disabled:bg-secondary cursor-pointer"
              disabled={!canSave}
              onClick={() => void handleUpdateProject()}
              type="button"
            >
              {isUpdating ? (
                <span className="inline-flex items-center gap-2">
                  <Spinner aria-hidden className="size-4" />
                  {t('common.actions.saving')}
                </span>
              ) : (
                t('project.edit.submit')
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <ConfirmDestructiveDialog
        confirmLabel={t('project.edit.delete')}
        confirmingLabel={t('project.edit.deleting')}
        description={t('project.edit.deleteConfirm', { projectName: name.trim() || initialName })}
        isConfirming={isDeleting}
        onConfirm={handleDeleteProject}
        onOpenChange={setConfirmDeleteOpen}
        open={confirmDeleteOpen}
        title={t('project.edit.deleteTitle')}
      />
    </>
  );
}
