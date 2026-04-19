import { useTranslation } from 'react-i18next';
import { FormFieldControl } from '@/components/common/FormFieldControl';
import { Button } from '@/components/ui/button';

type GroupOption = Readonly<{
  id: number;
  name: string;
}>;

type CreateProjectInfoStepProps = Readonly<{
  groups: GroupOption[];
  isGroupLocked: boolean;
  selectedGroupId: string;
  name: string;
  description: string;
  canContinue: boolean;
  onGroupChange: (groupId: string) => void;
  onNameChange: (name: string) => void;
  onDescriptionChange: (description: string) => void;
  onContinue: () => void;
}>;

export function CreateProjectInfoStep({
  groups,
  isGroupLocked,
  selectedGroupId,
  name,
  description,
  canContinue,
  onGroupChange,
  onNameChange,
  onDescriptionChange,
  onContinue,
}: CreateProjectInfoStepProps) {
  const { t } = useTranslation();

  return (
    <div className="mt-8 space-y-4">
      <FormFieldControl
        controlType="select"
        id="create-project-group"
        label={t('project.create.groupLabel')}
        onValueChange={onGroupChange}
        options={groups.map((group) => ({
          label: group.name,
          value: String(group.id),
        }))}
        required
        selectProps={{ disabled: isGroupLocked, required: true }}
        value={selectedGroupId}
      />

      <FormFieldControl
        id="create-project-name-page"
        inputProps={{
          maxLength: 256,
          placeholder: t('project.create.namePlaceholder'),
          required: true,
        }}
        label={t('project.create.nameLabel')}
        onValueChange={onNameChange}
        required
        value={name}
      />

      <FormFieldControl
        controlType="textarea"
        id="create-project-description-page"
        label={t('project.create.descriptionLabel')}
        onValueChange={onDescriptionChange}
        textareaProps={{
          maxLength: 2048,
          placeholder: t('project.create.descriptionPlaceholder'),
        }}
        value={description}
      />

      <div className="flex justify-end gap-3">
        <Button
          className="h-10 min-w-36 rounded-md bg-primary text-sm font-semibold text-white transition-colors hover:bg-primary-strong disabled:bg-secondary cursor-pointer"
          disabled={!canContinue}
          onClick={onContinue}
          type="button"
        >
          {t('project.create.nextStepSimple')}
        </Button>
      </div>
    </div>
  );
}
