import { useState } from 'react';
import { act, renderHook } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { useAnnotationStepCursor } from '@/modules/project/annotation/hooks/useAnnotationStepCursor';
import { ANNOTATION_PAGE_SIZE } from '@/modules/project/annotation/utils/annotationPageUtils';

function useCursorHarness(
  stepsLength: number,
  totalSteps: number,
  initialOffset = 0,
  initialResume: number | null = null,
) {
  const [annotationOffset, setAnnotationOffset] = useState(initialOffset);
  const [resumeGlobalStepIndex, setResumeGlobalStepIndex] = useState<number | null>(
    initialResume,
  );

  const cursor = useAnnotationStepCursor(
    stepsLength,
    totalSteps,
    annotationOffset,
    setAnnotationOffset,
    resumeGlobalStepIndex,
    setResumeGlobalStepIndex,
  );

  return { ...cursor, annotationOffset, resumeGlobalStepIndex };
}

describe('useAnnotationStepCursor', () => {
  it('reports no current step and disabled navigation when there are no steps', () => {
    const { result } = renderHook(() => useCursorHarness(0, 0));

    expect(result.current.currentGlobalStepIndex).toBe(0);
    expect(result.current.canMovePrevious).toBe(false);
    expect(result.current.canMoveNext).toBe(false);
  });

  it('starts at global step 1 with previous disabled and next enabled', () => {
    const { result } = renderHook(() => useCursorHarness(5, 10));

    expect(result.current.currentGlobalStepIndex).toBe(1);
    expect(result.current.canMovePrevious).toBe(false);
    expect(result.current.canMoveNext).toBe(true);
  });

  it('moves forward within the current page', () => {
    const { result } = renderHook(() => useCursorHarness(3, 3));

    act(() => result.current.goToNextStep());
    expect(result.current.activeStepOnPage).toBe(1);
    expect(result.current.currentGlobalStepIndex).toBe(2);
    expect(result.current.annotationOffset).toBe(0);
  });

  it('does nothing when trying to move past the last step of the last page', () => {
    const { result } = renderHook(() => useCursorHarness(3, 3, 0));

    act(() => result.current.goToNextStep());
    act(() => result.current.goToNextStep());
    expect(result.current.currentGlobalStepIndex).toBe(3);
    expect(result.current.canMoveNext).toBe(false);

    act(() => result.current.goToNextStep());
    expect(result.current.currentGlobalStepIndex).toBe(3);
    expect(result.current.annotationOffset).toBe(0);
  });

  it('moves back within the current page', () => {
    const { result } = renderHook(() => useCursorHarness(3, 3));
    act(() => result.current.goToNextStep());
    act(() => result.current.goToNextStep());
    expect(result.current.currentGlobalStepIndex).toBe(3);

    act(() => result.current.goToPreviousStep());

    expect(result.current.currentGlobalStepIndex).toBe(2);
    expect(result.current.annotationOffset).toBe(0);
  });

  it('does nothing when navigating with no current step at all', () => {
    const { result } = renderHook(() => useCursorHarness(0, 0));

    act(() => result.current.goToNextStep());
    act(() => result.current.goToPreviousStep());

    expect(result.current.currentGlobalStepIndex).toBe(0);
    expect(result.current.annotationOffset).toBe(0);
  });

  it('advances to the next page by exactly ANNOTATION_PAGE_SIZE and lands on the first step', () => {
    const totalSteps = ANNOTATION_PAGE_SIZE + 5;
    const { result } = renderHook(() => useCursorHarness(ANNOTATION_PAGE_SIZE, totalSteps, 0));

    act(() => {
      for (let i = 0; i < ANNOTATION_PAGE_SIZE - 1; i += 1) {
        result.current.goToNextStep();
      }
    });
    expect(result.current.currentGlobalStepIndex).toBe(ANNOTATION_PAGE_SIZE);

    act(() => result.current.goToNextStep());
    expect(result.current.annotationOffset).toBe(ANNOTATION_PAGE_SIZE);
    expect(result.current.activeStepOnPage).toBe(0);
    expect(result.current.currentGlobalStepIndex).toBe(ANNOTATION_PAGE_SIZE + 1);
  });

  it('moves back to the previous page and lands on its last step', () => {
    const { result } = renderHook(() =>
      useCursorHarness(ANNOTATION_PAGE_SIZE, ANNOTATION_PAGE_SIZE + 5, ANNOTATION_PAGE_SIZE),
    );

    act(() => result.current.goToPreviousStep());
    expect(result.current.annotationOffset).toBe(0);
    expect(result.current.activeStepOnPage).toBe(ANNOTATION_PAGE_SIZE - 1);
    expect(result.current.currentGlobalStepIndex).toBe(ANNOTATION_PAGE_SIZE);
  });

  it('resolves a pending resume index into the right page and step, then clears it', () => {
    // Step 55 (1-indexed) with a page size of ANNOTATION_PAGE_SIZE lives on the second page.
    const resumeIndex = ANNOTATION_PAGE_SIZE + 5;
    const totalSteps = ANNOTATION_PAGE_SIZE + 10;
    const { result } = renderHook(() =>
      useCursorHarness(ANNOTATION_PAGE_SIZE, totalSteps, 0, resumeIndex),
    );

    expect(result.current.annotationOffset).toBe(ANNOTATION_PAGE_SIZE);
    expect(result.current.currentGlobalStepIndex).toBe(resumeIndex);
    expect(result.current.resumeGlobalStepIndex).toBeNull();
  });
});
