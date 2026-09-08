import { useEffect } from 'react';
import { type Dispatch, type SetStateAction } from 'react';
import { useAnnotationStepCursor } from '@/modules/project/annotation/hooks/useAnnotationStepCursor';
import type {
  AnnotationStep,
  ProjectAnnotationWorkspace,
} from '@/modules/project/shared/types/project';
import { normalizeStepIndex } from '@/modules/project/annotation/utils/annotationPageUtils';

type UseAnnotationNavigationParams = Readonly<{
  annotationOffset: number;
  annotationWorkspace: ProjectAnnotationWorkspace | undefined;
  hasResolvedResumeStep: boolean;
  isInvalidProjectId: boolean;
  isReviewMode: boolean;
  resumeGlobalStepIndex: number | null;
  setAnnotationOffset: Dispatch<SetStateAction<number>>;
  setHasResolvedResumeStep: Dispatch<SetStateAction<boolean>>;
  setResumeGlobalStepIndex: Dispatch<SetStateAction<number | null>>;
}>;

type UseAnnotationNavigationResult = Readonly<{
  canMoveNext: boolean;
  canMovePrevious: boolean;
  currentGlobalStepIndex: number;
  currentStep: AnnotationStep | null;
  goToNextStep: () => void;
  goToPreviousStep: () => void;
  isFirstStep: boolean;
  isLastStep: boolean;
  steps: AnnotationStep[];
  totalSteps: number;
}>;

export function useAnnotationNavigation({
  annotationOffset,
  annotationWorkspace,
  hasResolvedResumeStep,
  isInvalidProjectId,
  isReviewMode,
  resumeGlobalStepIndex,
  setAnnotationOffset,
  setHasResolvedResumeStep,
  setResumeGlobalStepIndex,
}: UseAnnotationNavigationParams): UseAnnotationNavigationResult {
  const steps = annotationWorkspace?.steps ?? [];
  const totalSteps = annotationWorkspace?.totalSteps ?? 0;

  useEffect(() => {
    if (hasResolvedResumeStep || isInvalidProjectId || !annotationWorkspace) {
      return;
    }

    const total = annotationWorkspace.totalSteps;
    if (total <= 0) {
      setHasResolvedResumeStep(true);
      return;
    }

    const isCompleted = annotationWorkspace.completionPercentage >= 100;
    const isFirstTime = annotationWorkspace.completedSteps <= 0;

    const initialStep = normalizeStepIndex(
      isReviewMode || isCompleted || isFirstTime ? 1 : annotationWorkspace.firstPendingStepIndex,
      total,
    );

    setResumeGlobalStepIndex(initialStep);
    setHasResolvedResumeStep(true);
  }, [
    annotationWorkspace,
    hasResolvedResumeStep,
    isInvalidProjectId,
    isReviewMode,
    setHasResolvedResumeStep,
    setResumeGlobalStepIndex,
  ]);

  const {
    activeStepOnPage,
    currentGlobalStepIndex,
    canMovePrevious,
    canMoveNext,
    goToPreviousStep,
    goToNextStep,
  } = useAnnotationStepCursor(
    steps.length,
    totalSteps,
    annotationOffset,
    setAnnotationOffset,
    resumeGlobalStepIndex,
    setResumeGlobalStepIndex,
  );

  const currentStep =
    steps.length === 0 ? null : steps[Math.min(activeStepOnPage, steps.length - 1)];
  const isFirstStep = currentStep != null && currentGlobalStepIndex <= 1;
  const isLastStep = currentStep != null && totalSteps > 0 && currentGlobalStepIndex >= totalSteps;

  return {
    canMoveNext,
    canMovePrevious,
    currentGlobalStepIndex,
    currentStep,
    goToNextStep,
    goToPreviousStep,
    isFirstStep,
    isLastStep,
    steps,
    totalSteps,
  };
}
