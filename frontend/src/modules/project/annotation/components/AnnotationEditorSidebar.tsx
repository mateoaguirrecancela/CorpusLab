import { CheckCircle2, Tag } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import {
  type AnnotationDraft,
  getLabelStyle,
  projectTypeI18nKey,
} from '@/modules/project/annotation/utils/annotationPageUtils';
import type {
  AnnotationStep,
  ProjectDetail,
  ProjectSetupLabel,
  ProjectType,
} from '@/modules/project/shared/types/project';

type AnnotationEditorSidebarProps = Readonly<{
  activeNerLabel: string;
  annotationProjectType: ProjectType;
  classificationHeading: string;
  currentDraft: AnnotationDraft;
  currentStep: AnnotationStep;
  isReviewMode: boolean;
  labels: ProjectSetupLabel[];
  project: ProjectDetail | undefined;
  selectedLabels: string[];
  onActiveNerLabelChange: (labelName: string) => void;
  onToggleLabel: (step: AnnotationStep, labelName: string) => void;
  onUpdateDraft: (step: AnnotationStep, patch: Partial<AnnotationDraft>) => void;
}>;

export function AnnotationEditorSidebar({
  activeNerLabel,
  annotationProjectType,
  classificationHeading,
  currentDraft,
  currentStep,
  isReviewMode,
  labels,
  project,
  selectedLabels,
  onActiveNerLabelChange,
  onToggleLabel,
  onUpdateDraft,
}: AnnotationEditorSidebarProps) {
  const { t } = useTranslation();

  return (
    <aside className="space-y-4 rounded-xl border border-border bg-surface-base p-5 sm:p-6">
      <section>
        {project && (
          <h1 className="mb-4 text-md font-bold tracking-wider text-muted-foreground uppercase">
            {t(projectTypeI18nKey(project.projectType))}
          </h1>
        )}
        <h3 className="flex items-center gap-2 text-sm font-bold tracking-[0.12em] text-muted-foreground uppercase">
          <Tag className="size-4" />
          {classificationHeading}
        </h3>

        <div className="mt-4 space-y-3">
          {annotationProjectType === 'TEXT_CLASSIFICATION_SIMPLE' && labels.length > 0 && (
            <select
              className="h-10 w-full rounded-md border border-input bg-surface-base px-3 text-sm"
              disabled={isReviewMode}
              onChange={(event) => onUpdateDraft(currentStep, { value: event.currentTarget.value })}
              value={currentDraft.value}
            >
              <option value="">{t('project.annotationPage.fields.selectLabelPlaceholder')}</option>
              {labels.map((label) => (
                <option key={label.name} value={label.name}>
                  {label.name}
                </option>
              ))}
            </select>
          )}

          {annotationProjectType === 'TEXT_CLASSIFICATION_MULTILABEL' && labels.length > 0 && (
            <div className="grid gap-2">
              {labels.map((label) => {
                const selected = selectedLabels.includes(label.name);

                return (
                  <button
                    className={[
                      'flex w-full items-center justify-between rounded-md border px-3 py-2 text-left text-sm font-medium transition',
                      selected
                        ? 'bg-primary/8 shadow-[inset_0_0_0_1px_rgba(0,0,0,0.03)]'
                        : 'hover:bg-background',
                      isReviewMode ? 'cursor-default opacity-80' : 'cursor-pointer',
                    ].join(' ')}
                    disabled={isReviewMode}
                    key={label.name}
                    onClick={() => onToggleLabel(currentStep, label.name)}
                    style={getLabelStyle(label, selected)}
                    type="button"
                  >
                    <span className="truncate">{label.name}</span>
                    {selected && <CheckCircle2 aria-hidden className="size-4" />}
                  </button>
                );
              })}
            </div>
          )}

          {annotationProjectType === 'NER' && labels.length > 0 && (
            <div className="space-y-3">
              <div className="grid gap-2">
                {labels.map((label) => {
                  const selected = activeNerLabel === label.name;

                  return (
                    <button
                      className={[
                        'flex w-full items-center justify-between rounded-md border px-3 py-2 text-left text-sm font-medium transition',
                        selected
                          ? 'bg-primary/8 shadow-[inset_0_0_0_1px_rgba(0,0,0,0.03)]'
                          : 'hover:bg-transparent',
                        isReviewMode ? 'cursor-default opacity-80' : 'cursor-pointer',
                      ].join(' ')}
                      disabled={isReviewMode}
                      key={label.name}
                      onClick={() => onActiveNerLabelChange(label.name)}
                      style={getLabelStyle(label, selected)}
                      type="button"
                    >
                      <span className="truncate">{label.name}</span>
                      {selected && <CheckCircle2 aria-hidden className="size-4" />}
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          {annotationProjectType === 'SEQ2SEQ' && (
            <Textarea
              disabled={isReviewMode}
              onChange={(event) => onUpdateDraft(currentStep, { value: event.currentTarget.value })}
              placeholder={t('project.annotationPage.fields.annotationPlaceholder')}
              readOnly={isReviewMode}
              rows={5}
              value={currentDraft.value}
            />
          )}

          {annotationProjectType === 'TEXT_CLASSIFICATION_SIMPLE' && labels.length === 0 && (
            <Input
              disabled={isReviewMode}
              onChange={(event) => onUpdateDraft(currentStep, { value: event.currentTarget.value })}
              placeholder={t('project.annotationPage.fields.selectLabelPlaceholder')}
              readOnly={isReviewMode}
              value={currentDraft.value}
            />
          )}

          {annotationProjectType === 'TEXT_CLASSIFICATION_MULTILABEL' && labels.length === 0 && (
            <Input
              disabled={isReviewMode}
              onChange={(event) => onUpdateDraft(currentStep, { value: event.currentTarget.value })}
              placeholder={t('project.annotationPage.fields.labelsPlaceholder')}
              readOnly={isReviewMode}
              value={currentDraft.value}
            />
          )}

          {annotationProjectType === 'NER' && labels.length === 0 && (
            <p className="rounded-md border border-dashed border-border bg-background px-3 py-2 text-sm text-muted-foreground">
              {t('project.annotationPage.nerNoLabels')}
            </p>
          )}
        </div>
      </section>

      <section>
        <h3 className="text-sm font-bold tracking-[0.12em] text-muted-foreground uppercase">
          {t('project.annotationPage.notesTitle')}
        </h3>
        <Textarea
          className="mt-3"
          disabled={isReviewMode}
          onChange={(event) => onUpdateDraft(currentStep, { notes: event.currentTarget.value })}
          placeholder={t('project.annotationPage.fields.notesPlaceholder')}
          readOnly={isReviewMode}
          rows={6}
          value={currentDraft.notes}
        />
      </section>
    </aside>
  );
}
