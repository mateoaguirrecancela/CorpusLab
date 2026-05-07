import { useEffect, useState, type Dispatch, type SetStateAction } from 'react';
import {
  ANNOTATION_PAGE_SIZE,
  normalizeStepIndex,
  type PendingPageSelection,
} from '@/modules/project/utils/annotationPageUtils';

type StepCursorState = {
  activeStepOnPage: number;
  currentGlobalStepIndex: number;
  canMovePrevious: boolean;
  canMoveNext: boolean;
  goToPreviousStep: () => void;
  goToNextStep: () => void;
};

export function useAnnotationStepCursor(
  stepsLength: number,
  totalSteps: number,
  annotationOffset: number,
  setAnnotationOffset: Dispatch<SetStateAction<number>>,
  resumeGlobalStepIndex: number | null,
  setResumeGlobalStepIndex: Dispatch<SetStateAction<number | null>>,
): StepCursorState {
  const [activeStepOnPage, setActiveStepOnPage] = useState(0);
  const [pendingPageSelection, setPendingPageSelection] = useState<PendingPageSelection>('none');

  useEffect(() => {
    if (stepsLength === 0) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setActiveStepOnPage(0);
      return;
    }

    if (pendingPageSelection === 'first') {
      setActiveStepOnPage(0);
      setPendingPageSelection('none');
      return;
    }

    if (pendingPageSelection === 'last') {
      setActiveStepOnPage(stepsLength - 1);
      setPendingPageSelection('none');
      return;
    }

    setActiveStepOnPage((previous) => Math.min(previous, stepsLength - 1));
  }, [pendingPageSelection, stepsLength]);

  useEffect(() => {
    if (resumeGlobalStepIndex == null || totalSteps <= 0) {
      return;
    }

    const normalizedStepIndex = normalizeStepIndex(resumeGlobalStepIndex, totalSteps);
    const targetOffset =
      Math.floor((normalizedStepIndex - 1) / ANNOTATION_PAGE_SIZE) * ANNOTATION_PAGE_SIZE;

    if (annotationOffset !== targetOffset) {
      setAnnotationOffset(targetOffset);
      return;
    }

    const targetStepOnPage = normalizedStepIndex - 1 - annotationOffset;
    if (targetStepOnPage >= 0 && targetStepOnPage < stepsLength) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setActiveStepOnPage(targetStepOnPage);
      setResumeGlobalStepIndex(null);
    }
  }, [
    annotationOffset,
    resumeGlobalStepIndex,
    setAnnotationOffset,
    setResumeGlobalStepIndex,
    stepsLength,
    totalSteps,
  ]);

  const hasCurrentStep = stepsLength > 0;

  const currentGlobalStepIndex = hasCurrentStep
    ? annotationOffset + Math.min(activeStepOnPage, stepsLength - 1) + 1
    : 0;

  const canMovePrevious = hasCurrentStep && currentGlobalStepIndex > 1;
  const canMoveNext = hasCurrentStep && currentGlobalStepIndex < totalSteps;

  const goToPreviousStep = () => {
    if (!hasCurrentStep) {
      return;
    }

    if (activeStepOnPage > 0) {
      setActiveStepOnPage((previous) => previous - 1);
      return;
    }

    if (annotationOffset > 0) {
      setPendingPageSelection('last');
      setAnnotationOffset((previous) => Math.max(previous - ANNOTATION_PAGE_SIZE, 0));
    }
  };

  const goToNextStep = () => {
    if (!hasCurrentStep) {
      return;
    }

    if (activeStepOnPage < stepsLength - 1) {
      setActiveStepOnPage((previous) => previous + 1);
      return;
    }

    const hasMoreSteps = annotationOffset + stepsLength < totalSteps;
    if (hasMoreSteps) {
      setPendingPageSelection('first');
      setAnnotationOffset((previous) => previous + ANNOTATION_PAGE_SIZE);
    }
  };

  return {
    activeStepOnPage,
    currentGlobalStepIndex,
    canMovePrevious,
    canMoveNext,
    goToPreviousStep,
    goToNextStep,
  };
}
