import { useMemo, useState } from 'react';
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm, useWatch } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';
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
import { useCreateResearchGroupMutation } from '@/modules/researchgroup/hooks/useResearchGroupQueries';
import {
  createResearchGroupFormSchema,
  type ResearchGroupFormValues,
} from '@/modules/researchgroup/schemas/researchGroupFormSchemas';
import { getCreateGroupErrorMessage } from '@/modules/researchgroup/services/researchGroupService';
import {
  canSubmitResearchGroupForm,
  DIRTY_VALIDATED_FIELD_OPTIONS,
  EMPTY_RESEARCH_GROUP_FORM,
  toResearchGroupPayload,
} from '@/modules/researchgroup/utils/researchGroupForm';

type CreateResearchGroupDialogProps = {
  trigger: React.ReactNode;
};

export function CreateResearchGroupDialog({ trigger }: Readonly<CreateResearchGroupDialogProps>) {
  const { t } = useTranslation();
  const createGroupMutation = useCreateResearchGroupMutation();
  const researchGroupSchema = useMemo(() => createResearchGroupFormSchema(t), [t]);
  const form = useForm<ResearchGroupFormValues>({
    defaultValues: EMPTY_RESEARCH_GROUP_FORM,
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
  const name = useWatch({ control, name: 'name' });
  const description = useWatch({ control, name: 'description' });
  const isSaving = createGroupMutation.isPending;
  const values = { description, name };
  const canSave = canSubmitResearchGroupForm({
    isPending: isSaving,
    isValid,
    values,
  });

  const resetForm = () => {
    reset(EMPTY_RESEARCH_GROUP_FORM);
  };

  const handleOpenChange = (nextOpen: boolean) => {
    if (!nextOpen) {
      resetForm();
    }
    setOpen(nextOpen);
  };

  const submitForm = async (values: ResearchGroupFormValues) => {
    try {
      await createGroupMutation.mutateAsync(toResearchGroupPayload(values));

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

        <ResearchGroupFormFields
          description={description}
          descriptionId="create-group-description"
          descriptionLabel={t('researchGroup.create.descriptionLabel')}
          descriptionPlaceholder={t('researchGroup.create.descriptionPlaceholder')}
          errors={errors}
          name={name}
          nameId="create-group-name"
          nameLabel={t('researchGroup.create.nameLabel')}
          namePlaceholder={t('researchGroup.create.namePlaceholder')}
          onDescriptionChange={(nextDescription) =>
            setValue('description', nextDescription, DIRTY_VALIDATED_FIELD_OPTIONS)
          }
          onNameChange={(nextName) => setValue('name', nextName, DIRTY_VALIDATED_FIELD_OPTIONS)}
        />

        <DialogFooter>
          <DialogActionButton
            disabled={!canSave}
            isPending={isSaving}
            label={t('researchGroup.create.submit')}
            loadingLabel={t('common.actions.saving')}
            onClick={() => void handleSubmit(submitForm)()}
          />
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
