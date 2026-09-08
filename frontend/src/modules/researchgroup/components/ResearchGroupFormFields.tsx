import { type FieldErrors } from 'react-hook-form';
import { FormFieldControl } from '@/components/common/FormFieldControl';
import { type ResearchGroupFormValues } from '@/modules/researchgroup/schemas/researchGroupFormSchemas';

type ResearchGroupFormFieldsProps = Readonly<{
  description: string;
  descriptionId: string;
  descriptionLabel: string;
  descriptionPlaceholder: string;
  errors: FieldErrors<ResearchGroupFormValues>;
  name: string;
  nameId: string;
  nameLabel: string;
  namePlaceholder: string;
  onDescriptionChange: (value: string) => void;
  onNameChange: (value: string) => void;
}>;

export function ResearchGroupFormFields({
  description,
  descriptionId,
  descriptionLabel,
  descriptionPlaceholder,
  errors,
  name,
  nameId,
  nameLabel,
  namePlaceholder,
  onDescriptionChange,
  onNameChange,
}: ResearchGroupFormFieldsProps) {
  return (
    <div className="my-8 space-y-4">
      <FormFieldControl
        id={nameId}
        inputProps={{
          autoFocus: true,
          maxLength: 256,
          placeholder: namePlaceholder,
          required: true,
          'aria-invalid': Boolean(errors.name),
        }}
        label={nameLabel}
        message={errors.name?.message}
        onValueChange={onNameChange}
        required
        value={name}
      />

      <FormFieldControl
        controlType="textarea"
        id={descriptionId}
        label={descriptionLabel}
        message={errors.description?.message}
        onValueChange={onDescriptionChange}
        textareaProps={{
          maxLength: 2048,
          placeholder: descriptionPlaceholder,
          'aria-invalid': Boolean(errors.description),
        }}
        value={description}
      />
    </div>
  );
}
