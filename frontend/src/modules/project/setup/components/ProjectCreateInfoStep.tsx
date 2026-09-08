import { useTranslation } from 'react-i18next';
import { FormFieldControl } from '@/components/common/FormFieldControl';
import { Button } from '@/components/ui/button';

type GroupOption = Readonly<{
  id: number;
  name: string;
}>;

type ProjectCreateInfoStepProps = Readonly<{
  groups: GroupOption[];
  isGroupLocked: boolean;
  selectedGroupId: string;
  name: string;
  description: string;
  canContinue: boolean;
  descriptionErrorMessage?: string;
  nameErrorMessage?: string;
  selectedGroupErrorMessage?: string;
  onGroupChange: (groupId: string) => void;
  onNameChange: (name: string) => void;
  onDescriptionChange: (description: string) => void;
  onContinue: () => void;
}>;

export function ProjectCreateInfoStep({
  groups,
  isGroupLocked,
  selectedGroupId,
  name,
  description,
  canContinue,
  descriptionErrorMessage,
  nameErrorMessage,
  selectedGroupErrorMessage,
  onGroupChange,
  onNameChange,
  onDescriptionChange,
  onContinue,
}: ProjectCreateInfoStepProps) {
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
        selectProps={{
          'aria-invalid': Boolean(selectedGroupErrorMessage),
          disabled: isGroupLocked,
          required: true,
        }}
        message={selectedGroupErrorMessage}
        value={selectedGroupId}
      />

      <FormFieldControl
        id="create-project-name-page"
        inputProps={{
          maxLength: 256,
          placeholder: t('project.create.namePlaceholder'),
          required: true,
          'aria-invalid': Boolean(nameErrorMessage),
        }}
        label={t('project.create.nameLabel')}
        message={nameErrorMessage}
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
          'aria-invalid': Boolean(descriptionErrorMessage),
        }}
        value={description}
        message={descriptionErrorMessage}
      />

      <div className="flex justify-end gap-3">
        <Button
          disabled={!canContinue}
          onClick={onContinue}
          size="action-wide"
          type="button"
          variant="primaryAction"
        >
          {t('project.create.nextStepSimple')}
        </Button>
      </div>
    </div>
  );
}
