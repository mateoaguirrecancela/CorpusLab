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
import { useCreateProjectMutation } from '@/modules/project/hooks/useProjectQueries';
import { getCreateProjectErrorMessage } from '@/modules/project/services/projectService';

type CreateProjectDialogProps = {
  groupId: number;
  trigger: React.ReactNode;
};

export function CreateProjectDialog({ groupId, trigger }: Readonly<CreateProjectDialogProps>) {
  const { t } = useTranslation();
  const createProjectMutation = useCreateProjectMutation();

  const [open, setOpen] = useState(false);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');

  const isSaving = createProjectMutation.isPending;
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
      await createProjectMutation.mutateAsync({
        groupId,
        payload: {
          name: name.trim(),
          description: description.trim() || undefined,
        },
      });
      setOpen(false);
      resetForm();
      toast.success(t('project.create.success'));
    } catch (error) {
      toast.error(getCreateProjectErrorMessage(error));
    }
  };

  return (
    <Dialog onOpenChange={handleOpenChange} open={open}>
      <DialogTrigger render={trigger as React.JSX.Element} />

      <DialogContent className="sm:max-w-xl">
        <DialogHeader>
          <DialogTitle>{t('project.create.title')}</DialogTitle>
        </DialogHeader>

        <div className="my-8 space-y-4">
          <FormFieldControl
            id="create-project-name"
            inputProps={{
              autoFocus: true,
              maxLength: 256,
              placeholder: t('project.create.namePlaceholder'),
              required: true,
            }}
            label={t('project.create.nameLabel')}
            onValueChange={setName}
            required
            value={name}
          />

          <FormFieldControl
            controlType="textarea"
            id="create-project-description"
            label={t('project.create.descriptionLabel')}
            onValueChange={setDescription}
            textareaProps={{
              maxLength: 2048,
              placeholder: t('project.create.descriptionPlaceholder'),
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
              t('project.create.submit')
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
