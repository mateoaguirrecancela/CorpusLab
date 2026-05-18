import { type ReactElement, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
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
import { ProjectAssignmentMembersList } from '@/modules/project/components/ProjectAssignmentMembersList';
import { useEditProjectDialog } from '@/modules/project/hooks/useEditProjectDialog';
import { type ProjectParticipantAssignment } from '@/modules/project/types/project';

type EditProjectDialogProps = Readonly<{
  groupId: number;
  projectId: number;
  initialName: string;
  initialDescription: string | null;
  initialParticipantAssignments: ProjectParticipantAssignment[];
  trigger?: ReactNode;
  showDeleteButton?: boolean;
  onDeleted?: () => void;
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
}>;

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
}: EditProjectDialogProps) {
  const { t } = useTranslation();
  const {
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
  } = useEditProjectDialog({
    controlledOpen,
    groupId,
    initialDescription,
    initialName,
    initialParticipantAssignments,
    onDeleted,
    onOpenChange,
    projectId,
  });

  return (
    <>
      <Dialog open={open} onOpenChange={handleOpenChange}>
        {trigger ? <DialogTrigger render={trigger as ReactElement} /> : undefined}

        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>{t('project.edit.title')}</DialogTitle>
          </DialogHeader>

          <div className="my-8 space-y-5">
            <FormFieldControl
              id="edit-project-name"
              inputProps={{
                'aria-invalid': Boolean(errors.name),
                autoFocus: true,
                maxLength: 256,
                placeholder: t('project.edit.namePlaceholder'),
                required: true,
              }}
              label={t('project.edit.nameLabel')}
              message={errors.name?.message}
              onValueChange={setName}
              required
              value={name}
            />

            <FormFieldControl
              controlType="textarea"
              id="edit-project-description"
              label={t('project.edit.descriptionLabel')}
              message={errors.description?.message}
              onValueChange={setDescription}
              textareaProps={{
                'aria-invalid': Boolean(errors.description),
                maxLength: 2048,
                placeholder: t('project.edit.descriptionPlaceholder'),
              }}
              value={description}
            />

            <div className="space-y-3">
              <p className="text-sm font-semibold text-primary">
                {t('project.edit.participantsLabel')}
              </p>

              <ProjectAssignmentMembersList
                assignmentByUserId={assignmentByUserId}
                creatorUserId={creatorUserId}
                isLoading={isLoadingMembers}
                members={members}
                onToggleSelection={toggleSelection}
                selectionButtonClassName="rounded-md border p-4 text-left transition-all cursor-pointer"
              />
            </div>
          </div>

          <DialogFooter>
            {showDeleteButton ? (
              <Button
                className="h-10 min-w-32 rounded-md bg-destructive text-sm font-semibold text-white transition-colors hover:brightness-95 disabled:cursor-not-allowed disabled:opacity-70 cursor-pointer"
                disabled={isBusy}
                onClick={handleDeleteRequest}
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
