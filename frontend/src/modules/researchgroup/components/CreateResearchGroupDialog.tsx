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
import { useCreateResearchGroupMutation } from '@/modules/researchgroup/hooks/useResearchGroupQueries';
import { getCreateGroupErrorMessage } from '@/modules/researchgroup/services/researchGroupService';

type CreateResearchGroupDialogProps = {
  trigger: React.ReactNode;
};

export function CreateResearchGroupDialog({ trigger }: Readonly<CreateResearchGroupDialogProps>) {
  const { t } = useTranslation();
  const createGroupMutation = useCreateResearchGroupMutation();
  const [open, setOpen] = useState(false);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const isSaving = createGroupMutation.isPending;

  const canSave = name.trim().length > 0 && !isSaving;

  const resetForm = () => {
    setName('');
    setDescription('');
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
      await createGroupMutation.mutateAsync({
        name: name.trim(),
        description: description.trim() || undefined,
      });

      setOpen(false);
      resetForm();
      toast.success(t('researchGroup.create.submit'));
    } catch (error) {
      toast.error(getCreateGroupErrorMessage(error));
    }
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger render={trigger as React.JSX.Element} />

      <DialogContent className="sm:max-w-xl">
        <DialogHeader>
          <DialogTitle>{t('researchGroup.create.title')}</DialogTitle>
        </DialogHeader>

        <div className="my-8 space-y-4">
          <FormFieldControl
            id="create-group-name"
            inputProps={{
              autoFocus: true,
              maxLength: 256,
              placeholder: t('researchGroup.create.namePlaceholder'),
              required: true,
            }}
            label={t('researchGroup.create.nameLabel')}
            onValueChange={setName}
            required
            value={name}
          />

          <FormFieldControl
            controlType="textarea"
            id="create-group-description"
            label={t('researchGroup.create.descriptionLabel')}
            onValueChange={setDescription}
            textareaProps={{
              maxLength: 2048,
              placeholder: t('researchGroup.create.descriptionPlaceholder'),
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
              t('researchGroup.create.submit')
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
