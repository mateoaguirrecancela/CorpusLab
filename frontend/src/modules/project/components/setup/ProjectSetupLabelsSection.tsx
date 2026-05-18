import { Plus } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import { LabelRow } from '@/modules/project/components/LabelRow';
import { type ProjectSetupLabel } from '@/modules/project/types/project';

type ProjectSetupLabelsSectionProps = Readonly<{
  canUseCsvColumnsAsLabels: boolean;
  isNerProjectType: boolean;
  labelErrorMessage: string | undefined;
  labels: ProjectSetupLabel[];
  onAddLabel: () => void;
  onEditLabel: (index: number) => void;
  onRemoveLabel: (index: number) => void;
  onUseCsvColumnsAsLabelsChange: (checked: boolean) => void;
  shouldShowClassificationLabelMinError: boolean;
  useCsvColumnsAsLabels: boolean;
}>;

export function ProjectSetupLabelsSection({
  canUseCsvColumnsAsLabels,
  isNerProjectType,
  labelErrorMessage,
  labels,
  onAddLabel,
  onEditLabel,
  onRemoveLabel,
  onUseCsvColumnsAsLabelsChange,
  shouldShowClassificationLabelMinError,
  useCsvColumnsAsLabels,
}: ProjectSetupLabelsSectionProps) {
  const { t } = useTranslation();
  const visibleErrorMessage = shouldShowClassificationLabelMinError
    ? t('project.create.labelsMinCountError')
    : labelErrorMessage;

  return (
    <section>
      <div className="space-y-3">
        <div className="flex items-end justify-between gap-3">
          <p className="text-xs font-bold uppercase tracking-wider text-muted-foreground">
            {t('project.create.labelsSectionTitle')} *
          </p>

          <Button
            className="h-10 rounded-md bg-primary px-3 text-sm font-semibold text-white hover:bg-primary-strong cursor-pointer"
            onClick={onAddLabel}
            type="button"
          >
            <Plus className="mr-1 size-4" />
            {t('project.create.addLabel')}
          </Button>
        </div>

        {canUseCsvColumnsAsLabels && (
          <label
            className="flex cursor-pointer items-start gap-3 rounded-md border border-border bg-white px-4 py-5 text-sm text-primary"
            htmlFor="create-project-use-csv-columns-as-labels"
          >
            <input
              checked={useCsvColumnsAsLabels}
              className="mt-0.5 size-4 rounded border-border accent-primary"
              id="create-project-use-csv-columns-as-labels"
              onChange={(event) => onUseCsvColumnsAsLabelsChange(event.currentTarget.checked)}
              type="checkbox"
            />
            <span className="block font-semibold">{t('project.create.useCsvColumnsAsLabels')}</span>
          </label>
        )}
      </div>

      <div className="mt-4">
        {labels.length === 0 ? (
          <p className="rounded-lg border border-dashed border-border bg-background px-4 py-5 text-sm text-muted-foreground">
            {t('project.create.labelsEmptyState')}
          </p>
        ) : (
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            {labels.map((label, index) => (
              <LabelRow
                editLabelText={t('project.create.editLabel')}
                isNerProjectType={isNerProjectType}
                key={`${label.name}-${index}`}
                label={label}
                onEdit={() => onEditLabel(index)}
                onRemove={() => onRemoveLabel(index)}
                removeLabelText={t('project.create.removeLabel')}
              />
            ))}
          </div>
        )}
      </div>

      {visibleErrorMessage ? (
        <p className="mt-2 text-sm font-medium text-destructive">{visibleErrorMessage}</p>
      ) : null}
    </section>
  );
}
