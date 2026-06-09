import { X } from 'lucide-react';
import { type ReactNode, type RefObject } from 'react';
import { useTranslation } from 'react-i18next';
import { type AnnotationStep, type ProjectType } from '@/modules/project/shared/types/project';
import {
  getNerEntityStyle,
  type CsvLabelColumnValue,
  type NerAnnotationEntity,
  type NerTextSegment,
} from '@/modules/project/annotation/utils/annotationPageUtils';

type AnnotationCsvSourceContentProps = Readonly<{
  annotationProjectType: ProjectType;
  annotationTargetColumn: string | null;
  csvLabelColumnValues: CsvLabelColumnValue[];
  csvTargetColumnValue: string | null;
  currentStep: AnnotationStep;
  isReviewMode: boolean;
  nerLabelColorMap: Map<string, string | null>;
  nerSourceSelectionRef: RefObject<HTMLDivElement | null>;
  nerTextSegments: NerTextSegment[];
  onRemoveNerEntity: (step: AnnotationStep | null, entityToRemove: NerAnnotationEntity) => void;
}>;

type CsvLabelColumnContentProps = Readonly<{
  csvLabelColumnValues: CsvLabelColumnValue[];
}>;

function CsvLabelColumnContent({ csvLabelColumnValues }: CsvLabelColumnContentProps) {
  const { t } = useTranslation();

  if (csvLabelColumnValues.length === 0) {
    return null;
  }

  return (
    <div className="mt-4 border-t border-border pt-6">
      <h3 className="text-sm font-bold uppercase tracking-wider text-muted-foreground">
        {t('project.annotationPage.csvLabelOptionsTitle')}
      </h3>
      <div className="mt-3 space-y-4">
        {csvLabelColumnValues.map((columnValue) => (
          <div key={columnValue.name}>
            <p className="truncate text-xs font-bold uppercase tracking-wider text-muted-foreground">
              {columnValue.name}
            </p>
            <p className="mt-2 whitespace-pre-wrap text-sm leading-relaxed text-foreground/90">
              {columnValue.value}
            </p>
          </div>
        ))}
      </div>
    </div>
  );
}

type CsvNerTextSegmentsProps = Readonly<{
  currentStep: AnnotationStep;
  isReviewMode: boolean;
  nerLabelColorMap: Map<string, string | null>;
  nerTextSegments: NerTextSegment[];
  onRemoveNerEntity: (step: AnnotationStep | null, entityToRemove: NerAnnotationEntity) => void;
}>;

function CsvNerTextSegments({
  currentStep,
  isReviewMode,
  nerLabelColorMap,
  nerTextSegments,
  onRemoveNerEntity,
}: CsvNerTextSegmentsProps) {
  const { t } = useTranslation();

  return (
    <>
      {nerTextSegments.map((segment) => {
        if (segment.entities.length === 0) {
          return <span key={segment.key}>{segment.text}</span>;
        }

        const segEnd = Number.parseInt(segment.key.split('-')[2], 10);
        const nestedMarks = segment.entities.reduce<ReactNode>(
          (acc, entity) => (
            <mark
              className="rounded-xs px-px text-current"
              key={`${entity.label}-${entity.startOffset}`}
              style={getNerEntityStyle(entity.label, nerLabelColorMap)}
              title={entity.label}
            >
              {acc}
            </mark>
          ),
          <>{segment.text}</>,
        );

        return (
          <span className="group/ner relative inline" key={segment.key}>
            {nestedMarks}
            {!isReviewMode &&
              segment.entities
                .filter((entity) => entity.endOffset === segEnd)
                .map((entity, index) => (
                  <button
                    aria-label={t('project.annotationPage.removeEntity')}
                    className="absolute -top-2 -right-2 inline-flex size-4 items-center justify-center rounded-full border border-border bg-background text-muted-foreground opacity-0 shadow-sm transition-opacity hover:text-destructive group-hover/ner:opacity-100"
                    key={`${entity.label}-${entity.startOffset}`}
                    onClick={() => onRemoveNerEntity(currentStep, entity)}
                    onMouseDown={(event) => {
                      event.preventDefault();
                      event.stopPropagation();
                    }}
                    style={{
                      transform: index > 0 ? `translateX(${index * 1}rem)` : undefined,
                    }}
                    type="button"
                  >
                    <X aria-hidden className="size-3" />
                  </button>
                ))}
          </span>
        );
      })}
    </>
  );
}

export function AnnotationCsvSourceContent({
  annotationProjectType,
  annotationTargetColumn,
  csvLabelColumnValues,
  csvTargetColumnValue,
  currentStep,
  isReviewMode,
  nerLabelColorMap,
  nerSourceSelectionRef,
  nerTextSegments,
  onRemoveNerEntity,
}: AnnotationCsvSourceContentProps) {
  const { t } = useTranslation();

  if (csvTargetColumnValue == null) {
    return (
      <pre className="whitespace-pre-wrap px-3 py-2 text-sm leading-relaxed text-foreground/90">
        {currentStep.preview}
      </pre>
    );
  }

  const columnHeader = annotationTargetColumn ? (
    <h3 className="text-sm font-bold uppercase tracking-wider text-muted-foreground">
      {annotationTargetColumn}
    </h3>
  ) : null;

  if (annotationProjectType === 'NER') {
    return (
      <div>
        {columnHeader}
        <div
          aria-label={t('project.annotationPage.nerSelectionAreaLabel')}
          className="mt-2 whitespace-pre-wrap text-sm leading-relaxed text-foreground/90 select-text"
          ref={nerSourceSelectionRef}
        >
          <CsvNerTextSegments
            currentStep={currentStep}
            isReviewMode={isReviewMode}
            nerLabelColorMap={nerLabelColorMap}
            nerTextSegments={nerTextSegments}
            onRemoveNerEntity={onRemoveNerEntity}
          />
        </div>
        <CsvLabelColumnContent csvLabelColumnValues={csvLabelColumnValues} />
      </div>
    );
  }

  return (
    <div>
      {columnHeader}
      <p className="mt-2 whitespace-pre-wrap text-sm leading-relaxed text-foreground/90">
        {csvTargetColumnValue}
      </p>
      <CsvLabelColumnContent csvLabelColumnValues={csvLabelColumnValues} />
    </div>
  );
}
