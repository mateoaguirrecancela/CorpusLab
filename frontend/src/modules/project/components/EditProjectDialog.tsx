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
import { useProfileQuery } from '@/modules/auth/hooks/useProfileQuery';
import {
  useDeleteProjectMutation,
  useUpdateProjectMutation,
} from '@/modules/project/hooks/useProjectQueries';
import {
  getDeleteProjectErrorMessage,
  getUpdateProjectErrorMessage,
} from '@/modules/project/services/projectService';
import { useResearchGroupDetailQuery } from '@/modules/researchgroup/hooks/useResearchGroupQueries';

type EditProjectDialogProps = {
  groupId: number;
  projectId: number;
  initialName: string;
  initialDescription: string | null;
  initialParticipantUserIds: number[];
  trigger?: React.ReactNode;
  showDeleteButton?: boolean;
  onDeleted?: () => void;
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
};

function membersSelectionCardClassName(isSelected: boolean): string {
  return isSelected
    ? 'border-primary bg-primary/5 shadow-sm shadow-primary/10 ring-2 ring-primary/20'
    : 'border-border bg-background hover:border-primary/40 hover:bg-accent/30';
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
  initialParticipantUserIds,
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
  const [selectedUserIds, setSelectedUserIds] = useState<number[]>(initialParticipantUserIds);

  const isUpdating = updateProjectMutation.isPending;
  const isDeleting = deleteProjectMutation.isPending;
  const isBusy = isUpdating || isDeleting;
  const open = controlledOpen ?? internalOpen;

  const profileEmail = profile?.email?.toLowerCase() ?? '';
  const members = useMemo(
    () => (group?.members ?? []).filter((member) => member.email.toLowerCase() !== profileEmail),
    [group?.members, profileEmail],
  );

  const selectedSet = useMemo(() => new Set(selectedUserIds), [selectedUserIds]);

  const normalizedInitialParticipantIds = useMemo(
    () => [...new Set(initialParticipantUserIds)].sort((a, b) => a - b),
    [initialParticipantUserIds],
  );

  const participantsChanged = useMemo(() => {
    const normalizedSelectedParticipantIds = [...new Set(selectedUserIds)].sort((a, b) => a - b);

    if (normalizedSelectedParticipantIds.length !== normalizedInitialParticipantIds.length) {
      return true;
    }

    return normalizedSelectedParticipantIds.some(
      (participantId, index) => participantId !== normalizedInitialParticipantIds[index],
    );
  }, [normalizedInitialParticipantIds, selectedUserIds]);

  const hasChanges =
    name.trim() !== initialName.trim() ||
    description.trim() !== (initialDescription ?? '').trim() ||
    participantsChanged;

  const canSave = name.trim().length > 0 && hasChanges && !isBusy;

  const resetForm = () => {
    setName(initialName);
    setDescription(initialDescription ?? '');
    setSelectedUserIds(initialParticipantUserIds);
  };

  useEffect(() => {
    if (!open) {
      resetForm();
    }
  }, [initialName, initialDescription, initialParticipantUserIds, open]);

  const toggleSelection = (userId: number) => {
    setSelectedUserIds((prev) =>
      prev.includes(userId) ? prev.filter((id) => id !== userId) : [...prev, userId],
    );
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
          participantUserIds: selectedUserIds,
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

              {!isLoadingMembers && members.length === 0 && (
                <p className="rounded-md border border-dashed border-border bg-background px-4 py-5 text-sm text-muted-foreground">
                  {t('project.create.noAssignableMembers')}
                </p>
              )}

              {!isLoadingMembers && members.length > 0 && (
                <div className="grid gap-3 sm:grid-cols-2">
                  {members.map((member) => {
                    const isSelected = selectedSet.has(member.userId);

                    return (
                      <button
                        className={[
                          'rounded-xl border p-4 text-left transition-all cursor-pointer',
                          membersSelectionCardClassName(isSelected),
                        ].join(' ')}
                        key={member.userId}
                        onClick={() => toggleSelection(member.userId)}
                        type="button"
                      >
                        <div className="flex items-center gap-3">
                          <div className="flex size-10 items-center justify-center rounded-full bg-primary/10 text-xs font-bold text-primary">
                            {initials(member.firstName, member.lastName)}
                          </div>
                          <div className="min-w-0">
                            <p className="truncate text-sm font-semibold text-primary">
                              {member.firstName} {member.lastName}
                            </p>
                            <p className="truncate text-xs text-muted-foreground">{member.email}</p>
                          </div>
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
