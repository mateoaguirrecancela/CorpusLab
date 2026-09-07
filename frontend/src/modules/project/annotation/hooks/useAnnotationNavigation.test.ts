import { useState } from 'react';
import { renderHook } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { useAnnotationNavigation } from '@/modules/project/annotation/hooks/useAnnotationNavigation';
import {
  ANNOTATION_PAGE_SIZE,
  annotationStepKey,
} from '@/modules/project/annotation/utils/annotationPageUtils';
import {
  type AnnotationStep,
  type ProjectAnnotationWorkspace,
} from '@/modules/project/shared/types/project';

function makeSteps(count: number): AnnotationStep[] {
  return Array.from({ length: count }, (_, index) => ({
    datasetItemId: index + 1,
    datasetItemIndex: index,
    stepIndex: 1,
    totalStepsForItem: 1,
    sourceName: `file-${index + 1}.txt`,
    sourceMimeType: 'text/plain',
    preview: `preview ${index + 1}`,
    rowValues: null,
    completed: false,
    warning: false,
    annotation: null,
  }));
}

function workspace(overrides: Partial<ProjectAnnotationWorkspace> = {}): ProjectAnnotationWorkspace {
  const steps = makeSteps(5);
  return {
    projectId: 1,
    projectType: 'TEXT_CLASSIFICATION_SIMPLE',
    annotationTargetColumn: null,
    labels: [],
    offset: 0,
    limit: ANNOTATION_PAGE_SIZE,
    totalSteps: steps.length,
    completedSteps: 0,
    completionPercentage: 0,
    firstPendingStepIndex: 1,
    steps,
    ...overrides,
  };
}

function useNavigationHarness(initialWorkspace: ProjectAnnotationWorkspace | undefined, isReviewMode = false) {
  const [annotationOffset, setAnnotationOffset] = useState(0);
  const [hasResolvedResumeStep, setHasResolvedResumeStep] = useState(false);
  const [resumeGlobalStepIndex, setResumeGlobalStepIndex] = useState<number | null>(null);

  const navigation = useAnnotationNavigation({
    annotationOffset,
    annotationWorkspace: initialWorkspace,
    hasResolvedResumeStep,
    isInvalidProjectId: false,
    isReviewMode,
    resumeGlobalStepIndex,
    setAnnotationOffset,
    setHasResolvedResumeStep,
    setResumeGlobalStepIndex,
  });

  return { ...navigation, hasResolvedResumeStep };
}

describe('useAnnotationNavigation', () => {
  it('has no current step while the workspace is not loaded', () => {
    const { result } = renderHook(() => useNavigationHarness(undefined));

    expect(result.current.currentStep).toBeNull();
    expect(result.current.totalSteps).toBe(0);
    expect(result.current.steps).toEqual([]);
  });

  it('resolves the resume step to the first pending step on first load', () => {
    const ws = workspace({ completedSteps: 2, completionPercentage: 40, firstPendingStepIndex: 3 });
    const { result } = renderHook(() => useNavigationHarness(ws));

    expect(result.current.hasResolvedResumeStep).toBe(true);
    expect(result.current.currentStep).toEqual(ws.steps[2]);
    expect(result.current.currentGlobalStepIndex).toBe(3);
  });

  it('starts at step 1 for a first-time annotator regardless of firstPendingStepIndex', () => {
    const ws = workspace({ completedSteps: 0, completionPercentage: 0, firstPendingStepIndex: 3 });
    const { result } = renderHook(() => useNavigationHarness(ws));

    expect(result.current.currentGlobalStepIndex).toBe(1);
  });

  it('starts at step 1 when the workspace is already fully completed', () => {
    const ws = workspace({
      completedSteps: 5,
      completionPercentage: 100,
      firstPendingStepIndex: 6,
    });
    const { result } = renderHook(() => useNavigationHarness(ws));

    expect(result.current.currentGlobalStepIndex).toBe(1);
  });

  it('always starts at step 1 in review mode, ignoring progress', () => {
    const ws = workspace({ completedSteps: 2, completionPercentage: 40, firstPendingStepIndex: 3 });
    const { result } = renderHook(() => useNavigationHarness(ws, true));

    expect(result.current.currentGlobalStepIndex).toBe(1);
  });

  it('marks the resume step resolved without picking a step when there are no steps', () => {
    const ws = workspace({ totalSteps: 0, steps: [], firstPendingStepIndex: 0 });
    const { result } = renderHook(() => useNavigationHarness(ws));

    expect(result.current.hasResolvedResumeStep).toBe(true);
    expect(result.current.currentStep).toBeNull();
  });

  it('exposes isFirstStep/isLastStep and gates navigation at the boundaries', () => {
    const ws = workspace({ completedSteps: 0, completionPercentage: 0, firstPendingStepIndex: 1 });
    const { result } = renderHook(() => useNavigationHarness(ws));

    expect(result.current.isFirstStep).toBe(true);
    expect(result.current.isLastStep).toBe(false);
    expect(result.current.canMovePrevious).toBe(false);
    expect(result.current.canMoveNext).toBe(true);
  });

  it('produces a stable per-step key derived from the dataset item and step index', () => {
    const ws = workspace();
    const { result } = renderHook(() => useNavigationHarness(ws));

    expect(annotationStepKey(result.current.currentStep!)).toBe('1:1');
  });
});
