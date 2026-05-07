import { X } from 'lucide-react';
import { type RefObject } from 'react';
import { useTranslation } from 'react-i18next';
import { type AnnotationStep } from '@/modules/project/types/project';
import {
  getNerEntityStyle,
  type NerAnnotationEntity,
  type NerTextSegment,
} from '@/modules/project/utils/annotationPageUtils';

type AnnotationNerTextSegmentsProps = Readonly<{
  currentStep: AnnotationStep;
  isReviewMode: boolean;
  nerLabelColorMap: Map<string, string | null>;
  nerSourceSelectionRef: RefObject<HTMLDivElement | null>;
  nerTextSegments: NerTextSegment[];
  onRemoveNerEntity: (step: AnnotationStep | null, entityToRemove: NerAnnotationEntity) => void;
}>;

export function AnnotationNerTextSegments({
  currentStep,
  isReviewMode,
  nerLabelColorMap,
  nerSourceSelectionRef,
  nerTextSegments,
  onRemoveNerEntity,
}: AnnotationNerTextSegmentsProps) {
  const { t } = useTranslation();

  return (
    <div
      aria-label={t('project.annotationPage.nerSelectionAreaLabel')}
      className="max-h-130 overflow-auto rounded-lg border border-border/70 bg-background p-3 text-sm leading-relaxed text-foreground/90 whitespace-pre-wrap select-text"
      ref={nerSourceSelectionRef}
    >
      {nerTextSegments.map((segment) => {
        if (segment.entities.length === 0) {
          return <span key={segment.key}>{segment.text}</span>;
        }

        const segEnd = Number.parseInt(segment.key.split('-')[2], 10);

        const nestedMarks = segment.entities.reduce(
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
    </div>
  );
}
