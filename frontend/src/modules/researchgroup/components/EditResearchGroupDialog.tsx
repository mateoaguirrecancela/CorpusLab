import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';
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
import { useUpdateResearchGroupMutation } from '@/modules/researchgroup/hooks/useResearchGroupQueries';
import { getUpdateGroupErrorMessage } from '@/modules/researchgroup/services/researchGroupService';

type EditResearchGroupDialogProps = {
  groupId: number;
  initialName: string;
  initialDescription: string | null;
  trigger: React.ReactNode;
};

export function EditResearchGroupDialog({
  groupId,
  initialName,
  initialDescription,
  trigger,
}: Readonly<EditResearchGroupDialogProps>) {
  const { t } = useTranslation();
  const updateGroupMutation = useUpdateResearchGroupMutation(groupId);
  const [open, setOpen] = useState(false);
  const [name, setName] = useState(initialName);
  const [description, setDescription] = useState(initialDescription ?? '');
  const isSaving = updateGroupMutation.isPending;

  const hasChanges =
    name.trim() !== initialName.trim() || description.trim() !== (initialDescription ?? '').trim();
  const canSave = name.trim().length > 0 && hasChanges && !isSaving;

  const resetForm = () => {
    setName(initialName);
    setDescription(initialDescription ?? '');
  };

  const handleOpenChange = (nextOpen: boolean) => {
    if (!nextOpen) {
      resetForm();
    }
    setOpen(nextOpen);
  };

  const handleSubmit = async () => {
    if (!canSave) {
      return;
    }

    try {
      await updateGroupMutation.mutateAsync({
        name: name.trim(),
        description: description.trim() || undefined,
      });
      setOpen(false);
      resetForm();
      toast.success(t('common.actions.saveChanges'));
    } catch (error) {
      toast.error(getUpdateGroupErrorMessage(error));
    }
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger render={trigger as React.JSX.Element} />

      <DialogContent className="sm:max-w-xl">
        <DialogHeader>
          <DialogTitle>{t('researchGroup.edit.title')}</DialogTitle>
        </DialogHeader>

        <div className="my-8 space-y-4">
          <FormFieldControl
            id="edit-group-name"
            inputProps={{
              autoFocus: true,
              maxLength: 256,
              placeholder: t('researchGroup.edit.namePlaceholder'),
              required: true,
            }}
            label={t('researchGroup.edit.nameLabel')}
            onValueChange={setName}
            required
            value={name}
          />

          <FormFieldControl
            controlType="textarea"
            id="edit-group-description"
            label={t('researchGroup.edit.descriptionLabel')}
            onValueChange={setDescription}
            textareaProps={{
              maxLength: 2048,
              placeholder: t('researchGroup.edit.descriptionPlaceholder'),
            }}
            value={description}
          />
        </div>

        <DialogFooter>
          <Button
            className="h-10 min-w-28 rounded-md bg-primary text-sm font-semibold text-white transition-colors hover:bg-primary-strong disabled:bg-secondary cursor-pointer"
            disabled={!canSave}
            onClick={() => void handleSubmit()}
            type="button"
          >
            {isSaving ? (
              <span className="inline-flex items-center gap-2">
                <Spinner aria-hidden className="size-4" />
                {t('common.actions.saving')}
              </span>
            ) : (
              t('researchGroup.edit.submit')
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
