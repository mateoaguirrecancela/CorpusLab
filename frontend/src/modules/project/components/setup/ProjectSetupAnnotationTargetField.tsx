import { useTranslation } from 'react-i18next';
import { FormFieldControl } from '@/components/common/FormFieldControl';
import { Spinner } from '@/components/ui/spinner';

type ProjectSetupAnnotationTargetFieldProps = Readonly<{
  annotationTargetColumn: string;
  csvHeaderOptions: string[];
  errorMessage: string | undefined;
  isLoadingCsvHeaders: boolean;
  onChange: (nextColumn: string) => void;
  requiresAnnotationTargetColumn: boolean;
}>;

export function ProjectSetupAnnotationTargetField({
  annotationTargetColumn,
  csvHeaderOptions,
  errorMessage,
  isLoadingCsvHeaders,
  onChange,
  requiresAnnotationTargetColumn,
}: ProjectSetupAnnotationTargetFieldProps) {
  const { t } = useTranslation();

  if (!requiresAnnotationTargetColumn) {
    return null;
  }

  if (isLoadingCsvHeaders) {
    return (
      <p className="inline-flex items-center gap-2 text-sm text-muted-foreground">
        <Spinner aria-hidden className="size-4" />
        {t('project.create.annotationTargetColumnLoading')}
      </p>
    );
  }

  if (csvHeaderOptions.length > 0) {
    return (
      <FormFieldControl
        controlType="select"
        id="create-project-annotation-target-column"
        label={t('project.create.annotationTargetColumnLabel')}
        onValueChange={onChange}
        message={errorMessage}
        options={csvHeaderOptions.map((header) => ({
          label: header,
          value: header,
        }))}
        required
        selectProps={{ required: true }}
        value={annotationTargetColumn}
      />
    );
  }

  return (
    <FormFieldControl
      controlType="input"
      id="create-project-annotation-target-column-fallback"
      label={t('project.create.annotationTargetColumnLabel')}
      onValueChange={onChange}
      inputProps={{
        'aria-invalid': Boolean(errorMessage),
        placeholder: t('project.create.annotationTargetColumnPlaceholder'),
        required: true,
      }}
      message={errorMessage}
      required
      value={annotationTargetColumn}
    />
  );
}
