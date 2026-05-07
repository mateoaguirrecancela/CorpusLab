import { AlertTriangle, CheckCircle2, CircleDashed } from 'lucide-react';
import { type RefObject, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Spinner } from '@/components/ui/spinner';
import { AnnotationNerTextSegments } from '@/modules/project/components/annotation/AnnotationNerTextSegments';
import { type AnnotationStep, type ProjectType } from '@/modules/project/types/project';
import {
  isCsvMimeType,
  type NerAnnotationEntity,
  type NerTextSegment,
} from '@/modules/project/utils/annotationPageUtils';

type AnnotationSourcePanelProps = Readonly<{
  annotationProjectType: ProjectType;
  csvSourceContent: ReactNode;
  currentGlobalStepIndex: number;
  currentStep: AnnotationStep;
  isReviewMode: boolean;
  isSourceLoading: boolean;
  isWarningUpdating: boolean;
  nerLabelColorMap: Map<string, string | null>;
  nerSourceSelectionRef: RefObject<HTMLDivElement | null>;
  nerSourceText: string;
  nerTextSegments: NerTextSegment[];
  sourceFileName: string | null;
  sourceLoadError: string;
  sourceMimeType: string;
  sourceTextContent: string | null;
  sourceUrl: string | null;
  totalSteps: number;
  onRemoveNerEntity: (step: AnnotationStep | null, entityToRemove: NerAnnotationEntity) => void;
  onToggleWarning: () => void;
}>;

export function AnnotationSourcePanel({
  annotationProjectType,
  csvSourceContent,
  currentGlobalStepIndex,
  currentStep,
  isReviewMode,
  isSourceLoading,
  isWarningUpdating,
  nerLabelColorMap,
  nerSourceSelectionRef,
  nerSourceText,
  nerTextSegments,
  sourceFileName,
  sourceLoadError,
  sourceMimeType,
  sourceTextContent,
  sourceUrl,
  totalSteps,
  onRemoveNerEntity,
  onToggleWarning,
}: AnnotationSourcePanelProps) {
  const { t } = useTranslation();

  return (
    <section className="min-w-0 rounded-xl border border-border bg-surface-base p-5 sm:p-6">
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-border pb-3">
        <div>
          <h2 className="text-sm font-bold tracking-[0.12em] text-muted-foreground uppercase">
            {t('project.annotationPage.sourceTitle')}
          </h2>
          <p className="mt-1 text-sm font-medium text-primary">
            {sourceFileName ?? currentStep.sourceName}
          </p>
        </div>
        <div className="flex items-center gap-2">
          {currentStep.completed && (isReviewMode || currentStep.warning) && (
            <button
              aria-label={t('project.annotationPage.warningToggleLabel')}
              className={[
                'inline-flex items-center justify-center transition-colors',
                currentStep.warning ? 'text-red-500' : 'text-muted-foreground/40',
                isReviewMode ? 'cursor-pointer hover:text-red-400' : 'cursor-default',
              ].join(' ')}
              disabled={!isReviewMode || isWarningUpdating}
              onClick={onToggleWarning}
              title={t('project.annotationPage.warningTooltip')}
              type="button"
            >
              <AlertTriangle aria-hidden className="size-5" />
            </button>
          )}
          <span className="inline-flex items-center px-2 py-1 text-xs font-semibold text-muted-foreground">
            {currentGlobalStepIndex}/{totalSteps}
          </span>
          <span
            className={[
              'inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs font-semibold',
              currentStep.completed
                ? 'bg-emerald-100 text-emerald-700'
                : 'bg-amber-100 text-amber-700',
            ].join(' ')}
          >
            {currentStep.completed ? (
              <CheckCircle2 aria-hidden className="size-3.5" />
            ) : (
              <CircleDashed aria-hidden className="size-3.5" />
            )}
            {currentStep.completed
              ? t('project.annotationPage.status.completed')
              : t('project.annotationPage.status.pending')}
          </span>
        </div>
      </div>

      <div className="mt-4 xl:min-h-64 min-w-0">
        {isSourceLoading && (
          <div className="flex h-95 items-center justify-center text-sm text-muted-foreground">
            <span className="inline-flex items-center gap-2">
              <Spinner aria-hidden className="size-4" />
              {t('project.annotationPage.sourceLoading')}
            </span>
          </div>
        )}

        {!isSourceLoading && sourceLoadError.length > 0 && (
          <p className="p-3 text-sm text-destructive">{sourceLoadError}</p>
        )}

        {!isSourceLoading &&
          sourceLoadError.length === 0 &&
          isCsvMimeType(currentStep.sourceMimeType) && (
            <div className="max-h-130 w-full overflow-y-auto">{csvSourceContent}</div>
          )}

        {!isSourceLoading &&
          sourceLoadError.length === 0 &&
          sourceTextContent != null &&
          annotationProjectType !== 'NER' && (
            <pre className="max-h-130 overflow-auto whitespace-pre-wrap text-sm leading-relaxed text-foreground/90">
              {sourceTextContent}
            </pre>
          )}

        {!isSourceLoading &&
          sourceLoadError.length === 0 &&
          sourceUrl != null &&
          sourceMimeType.toLowerCase().startsWith('image/') && (
            <div className="flex h-full min-h-95 items-center justify-center overflow-auto">
              <img
                alt={t('project.annotationPage.sourceImageAlt')}
                className="max-h-130 w-auto rounded-md"
                src={sourceUrl}
              />
            </div>
          )}

        {!isSourceLoading &&
          sourceLoadError.length === 0 &&
          sourceUrl != null &&
          sourceMimeType.toLowerCase().includes('pdf') && (
            <iframe
              className="h-140 w-full rounded-md border border-border bg-background"
              src={sourceUrl}
              title={sourceFileName ?? currentStep.sourceName}
            />
          )}

        {!isSourceLoading &&
          sourceLoadError.length === 0 &&
          !isCsvMimeType(currentStep.sourceMimeType) &&
          annotationProjectType === 'NER' &&
          nerSourceText.length > 0 && (
            <div className="mt-4 space-y-2">
              <AnnotationNerTextSegments
                currentStep={currentStep}
                isReviewMode={isReviewMode}
                nerLabelColorMap={nerLabelColorMap}
                nerSourceSelectionRef={nerSourceSelectionRef}
                nerTextSegments={nerTextSegments}
                onRemoveNerEntity={onRemoveNerEntity}
              />
            </div>
          )}

        {!isSourceLoading &&
          sourceLoadError.length === 0 &&
          sourceUrl == null &&
          sourceTextContent == null &&
          annotationProjectType !== 'NER' &&
          !isCsvMimeType(currentStep.sourceMimeType) && (
            <div className="space-y-2 p-3">
              <p className="text-sm text-muted-foreground">
                {t('project.annotationPage.sourceNotSupported')}
              </p>
              <p className="rounded-md border border-border bg-background p-3 text-sm text-foreground/90 wrap-break-word">
                {currentStep.preview}
              </p>
            </div>
          )}
      </div>
    </section>
  );
}
