import { useMemo, useState } from 'react';
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm, useWatch } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';
import { ConfirmDestructiveDialog } from '@/components/common/ConfirmDestructiveDialog';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { DialogActionButton } from '@/modules/researchgroup/components/DialogActionButton';
import { ResearchGroupFormFields } from '@/modules/researchgroup/components/ResearchGroupFormFields';
import {
  useDeleteResearchGroupMutation,
  useUpdateResearchGroupMutation,
} from '@/modules/researchgroup/hooks/useResearchGroupQueries';
import {
  createResearchGroupFormSchema,
  type ResearchGroupFormValues,
} from '@/modules/researchgroup/schemas/researchGroupFormSchemas';
import {
  getDeleteGroupErrorMessage,
  getUpdateGroupErrorMessage,
} from '@/modules/researchgroup/services/researchGroupService';
import {
  buildResearchGroupFormValues,
  canSubmitResearchGroupForm,
  DIRTY_VALIDATED_FIELD_OPTIONS,
  toResearchGroupPayload,
} from '@/modules/researchgroup/utils/researchGroupForm';

type EditResearchGroupDialogProps = {
  groupId: number;
  initialName: string;
  initialDescription: string | null;
  trigger: React.ReactNode;
  showDeleteButton?: boolean;
  onDeleted?: () => void;
};

export function EditResearchGroupDialog({
  groupId,
  initialName,
  initialDescription,
  trigger,
  showDeleteButton = false,
  onDeleted,
}: Readonly<EditResearchGroupDialogProps>) {
  const { t } = useTranslation();
  const updateGroupMutation = useUpdateResearchGroupMutation(groupId);
  const deleteGroupMutation = useDeleteResearchGroupMutation(groupId);
  const researchGroupSchema = useMemo(() => createResearchGroupFormSchema(t), [t]);
  const defaultFormValues = useMemo<ResearchGroupFormValues>(
    () => buildResearchGroupFormValues(initialName, initialDescription),
    [initialDescription, initialName],
  );
  const form = useForm<ResearchGroupFormValues>({
    defaultValues: defaultFormValues,
    mode: 'onChange',
    resolver: zodResolver(researchGroupSchema),
  });
  const {
    control,
    formState: { errors, isValid },
    handleSubmit,
    reset,
    setValue,
  } = form;
  const [open, setOpen] = useState(false);
  const [confirmDeleteOpen, setConfirmDeleteOpen] = useState(false);
  const name = useWatch({ control, name: 'name' });
  const description = useWatch({ control, name: 'description' });
  const isSaving = updateGroupMutation.isPending;
  const isDeleting = deleteGroupMutation.isPending;
  const values = { description, name };
  const canSave = canSubmitResearchGroupForm({
    initialValues: defaultFormValues,
    isPending: isSaving,
    isValid,
    values,
  });

  const resetForm = () => {
    reset(defaultFormValues);
  };

  const handleOpenChange = (nextOpen: boolean) => {
    resetForm();

    if (!nextOpen) {
      setConfirmDeleteOpen(false);
    }

    setOpen(nextOpen);
  };

  const submitForm = async (values: ResearchGroupFormValues) => {
    try {
      await updateGroupMutation.mutateAsync(toResearchGroupPayload(values));
      setOpen(false);
      resetForm();
      toast.success(t('common.actions.saveChanges'));
    } catch (error) {
      toast.error(getUpdateGroupErrorMessage(error));
    }
  };

  const handleDeleteGroup = async () => {
    if (isDeleting) {
      return;
    }

    try {
      await deleteGroupMutation.mutateAsync();
      setConfirmDeleteOpen(false);
      setOpen(false);
      toast.success(t('researchGroup.edit.deleteSuccess'));
      onDeleted?.();
    } catch (error) {
      toast.error(getDeleteGroupErrorMessage(error));
    }
  };

  return (
    <>
      <Dialog open={open} onOpenChange={handleOpenChange}>
        <DialogTrigger render={trigger as React.JSX.Element} />

        <DialogContent className="sm:max-w-xl">
          <DialogHeader>
            <DialogTitle>{t('researchGroup.edit.title')}</DialogTitle>
          </DialogHeader>

          <ResearchGroupFormFields
            description={description}
            descriptionId="edit-group-description"
            descriptionLabel={t('researchGroup.edit.descriptionLabel')}
            descriptionPlaceholder={t('researchGroup.edit.descriptionPlaceholder')}
            errors={errors}
            name={name}
            nameId="edit-group-name"
            nameLabel={t('researchGroup.edit.nameLabel')}
            namePlaceholder={t('researchGroup.edit.namePlaceholder')}
            onDescriptionChange={(nextDescription) =>
              setValue('description', nextDescription, DIRTY_VALIDATED_FIELD_OPTIONS)
            }
            onNameChange={(nextName) => setValue('name', nextName, DIRTY_VALIDATED_FIELD_OPTIONS)}
          />

          <DialogFooter>
            {showDeleteButton ? (
              <Button
                className="h-10 min-w-32 rounded-md bg-destructive text-sm font-semibold text-white transition-colors hover:brightness-95 disabled:cursor-not-allowed disabled:opacity-70 cursor-pointer"
                disabled={isDeleting || isSaving}
                onClick={() => {
                  setOpen(false);
                  setConfirmDeleteOpen(true);
                }}
                type="button"
              >
                {t('researchGroup.edit.delete')}
              </Button>
            ) : undefined}
            <DialogActionButton
              disabled={!canSave}
              isPending={isSaving}
              label={t('researchGroup.edit.submit')}
              loadingLabel={t('common.actions.saving')}
              minWidthClassName="min-w-32"
              onClick={() => void handleSubmit(submitForm)()}
            />
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <ConfirmDestructiveDialog
        confirmLabel={t('researchGroup.edit.delete')}
        confirmingLabel={t('researchGroup.edit.deleting')}
        description={t('researchGroup.edit.deleteConfirm', {
          groupName: name.trim() || initialName,
        })}
        isConfirming={isDeleting}
        onConfirm={handleDeleteGroup}
        onOpenChange={setConfirmDeleteOpen}
        open={confirmDeleteOpen}
        title={t('researchGroup.edit.deleteTitle')}
      />
    </>
  );
}
