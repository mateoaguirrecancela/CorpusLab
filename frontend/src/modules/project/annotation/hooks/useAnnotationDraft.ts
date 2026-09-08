import { useCallback, useMemo, useState } from 'react';
import type { AnnotationStep, ProjectType } from '@/modules/project/shared/types/project';
import {
  annotationStepKey,
  annotationValueToEditorValue,
  annotationValueToNerEntities,
  annotationValueToNotes,
  parseCommaSeparatedLabels,
  type AnnotationDraft,
  type AnnotationDraftByStep,
} from '@/modules/project/annotation/utils/annotationPageUtils';

const EMPTY_DRAFT: AnnotationDraft = {
  value: '',
  notes: '',
  entities: [],
};

type UseAnnotationDraftParams = Readonly<{
  annotationProjectType: ProjectType;
  isReviewMode: boolean;
}>;

type UseAnnotationDraftResult = Readonly<{
  currentDraft: AnnotationDraft;
  getDraft: (step: AnnotationStep) => AnnotationDraft;
  resetDrafts: () => void;
  selectedLabels: string[];
  toggleLabel: (step: AnnotationStep, labelName: string) => void;
  updateDraft: (step: AnnotationStep, patch: Partial<AnnotationDraft>) => void;
}>;

export function useAnnotationDraft(
  currentStep: AnnotationStep | null,
  { annotationProjectType, isReviewMode }: UseAnnotationDraftParams,
): UseAnnotationDraftResult {
  const [draftByStep, setDraftByStep] = useState<AnnotationDraftByStep>({});

  const getDraft = useCallback(
    (step: AnnotationStep): AnnotationDraft => {
      const stepId = annotationStepKey(step);
      const draft = draftByStep[stepId];
      if (draft) {
        return draft;
      }

      return {
        value: annotationValueToEditorValue(step.annotation, annotationProjectType),
        notes: annotationValueToNotes(step.annotation),
        entities: annotationValueToNerEntities(step.annotation),
      };
    },
    [annotationProjectType, draftByStep],
  );

  const updateDraft = useCallback(
    (step: AnnotationStep, patch: Partial<AnnotationDraft>) => {
      if (isReviewMode) {
        return;
      }

      const stepId = annotationStepKey(step);
      const currentDraft = getDraft(step);

      setDraftByStep((previous) => ({
        ...previous,
        [stepId]: {
          ...currentDraft,
          ...patch,
        },
      }));
    },
    [getDraft, isReviewMode],
  );

  const toggleLabel = useCallback(
    (step: AnnotationStep, labelName: string) => {
      const currentDraft = getDraft(step);
      const labels = parseCommaSeparatedLabels(currentDraft.value);
      const hasLabel = labels.includes(labelName);

      const nextLabels = hasLabel
        ? labels.filter((existingLabel) => existingLabel !== labelName)
        : [...labels, labelName];

      updateDraft(step, { value: nextLabels.join(', ') });
    },
    [getDraft, updateDraft],
  );

  const currentDraft = currentStep ? getDraft(currentStep) : EMPTY_DRAFT;
  const selectedLabels = useMemo(
    () => parseCommaSeparatedLabels(currentDraft.value),
    [currentDraft.value],
  );
  const resetDrafts = useCallback(() => setDraftByStep({}), []);

  return {
    currentDraft,
    getDraft,
    resetDrafts,
    selectedLabels,
    toggleLabel,
    updateDraft,
  };
}

export type { AnnotationDraft };
export type { NerAnnotationEntity } from '@/modules/project/annotation/utils/annotationPageUtils';
