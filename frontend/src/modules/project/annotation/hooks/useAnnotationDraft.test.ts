import { act, renderHook } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { useAnnotationDraft } from '@/modules/project/annotation/hooks/useAnnotationDraft';
import { type AnnotationStep } from '@/modules/project/shared/types/project';

function step(overrides: Partial<AnnotationStep> = {}): AnnotationStep {
  return {
    datasetItemId: 1,
    datasetItemIndex: 0,
    stepIndex: 1,
    totalStepsForItem: 1,
    sourceName: 'file.txt',
    sourceMimeType: 'text/plain',
    preview: 'preview',
    rowValues: null,
    completed: false,
    warning: false,
    annotation: null,
    ...overrides,
  };
}

describe('useAnnotationDraft', () => {
  it('returns an empty draft when there is no current step', () => {
    const { result } = renderHook(() =>
      useAnnotationDraft(null, {
        annotationProjectType: 'TEXT_CLASSIFICATION_SIMPLE',
        isReviewMode: false,
      }),
    );

    expect(result.current.currentDraft).toEqual({ value: '', notes: '', entities: [] });
    expect(result.current.selectedLabels).toEqual([]);
  });

  it('derives the initial draft from the step annotation', () => {
    const currentStep = step({ annotation: { label: 'positive', notes: 'note' } });
    const { result } = renderHook(() =>
      useAnnotationDraft(currentStep, {
        annotationProjectType: 'TEXT_CLASSIFICATION_SIMPLE',
        isReviewMode: false,
      }),
    );

    expect(result.current.currentDraft).toEqual({
      value: 'positive',
      notes: 'note',
      entities: [],
    });
  });

  it('updateDraft merges the patch into the cached draft and getDraft reflects it', () => {
    const currentStep = step();
    const { result } = renderHook(() =>
      useAnnotationDraft(currentStep, {
        annotationProjectType: 'TEXT_CLASSIFICATION_SIMPLE',
        isReviewMode: false,
      }),
    );

    act(() => result.current.updateDraft(currentStep, { value: 'negative' }));

    expect(result.current.currentDraft.value).toBe('negative');
    expect(result.current.getDraft(currentStep).value).toBe('negative');
  });

  it('updateDraft is a no-op in review mode', () => {
    const currentStep = step();
    const { result } = renderHook(() =>
      useAnnotationDraft(currentStep, {
        annotationProjectType: 'TEXT_CLASSIFICATION_SIMPLE',
        isReviewMode: true,
      }),
    );

    act(() => result.current.updateDraft(currentStep, { value: 'negative' }));

    expect(result.current.currentDraft.value).toBe('');
  });

  it('toggleLabel adds a label not yet present', () => {
    const currentStep = step();
    const { result } = renderHook(() =>
      useAnnotationDraft(currentStep, {
        annotationProjectType: 'TEXT_CLASSIFICATION_MULTILABEL',
        isReviewMode: false,
      }),
    );

    act(() => result.current.toggleLabel(currentStep, 'urgent'));
    expect(result.current.selectedLabels).toEqual(['urgent']);

    act(() => result.current.toggleLabel(currentStep, 'reviewed'));
    expect(result.current.selectedLabels).toEqual(['urgent', 'reviewed']);
  });

  it('toggleLabel removes a label already present', () => {
    const currentStep = step({ annotation: { labels: ['urgent', 'reviewed'] } });
    const { result } = renderHook(() =>
      useAnnotationDraft(currentStep, {
        annotationProjectType: 'TEXT_CLASSIFICATION_MULTILABEL',
        isReviewMode: false,
      }),
    );

    act(() => result.current.toggleLabel(currentStep, 'urgent'));
    expect(result.current.selectedLabels).toEqual(['reviewed']);
  });

  it('resetDrafts clears cached edits back to the step-derived draft', () => {
    const currentStep = step({ annotation: { label: 'positive' } });
    const { result } = renderHook(() =>
      useAnnotationDraft(currentStep, {
        annotationProjectType: 'TEXT_CLASSIFICATION_SIMPLE',
        isReviewMode: false,
      }),
    );

    act(() => result.current.updateDraft(currentStep, { value: 'negative' }));
    expect(result.current.currentDraft.value).toBe('negative');

    act(() => result.current.resetDrafts());
    expect(result.current.currentDraft.value).toBe('positive');
  });

  it('keeps per-step drafts independent', () => {
    const stepOne = step({ datasetItemId: 1, stepIndex: 1 });
    const stepTwo = step({ datasetItemId: 2, stepIndex: 1 });
    const { result } = renderHook(() =>
      useAnnotationDraft(stepOne, {
        annotationProjectType: 'TEXT_CLASSIFICATION_SIMPLE',
        isReviewMode: false,
      }),
    );

    act(() => result.current.updateDraft(stepOne, { value: 'a' }));
    act(() => result.current.updateDraft(stepTwo, { value: 'b' }));

    expect(result.current.getDraft(stepOne).value).toBe('a');
    expect(result.current.getDraft(stepTwo).value).toBe('b');
  });
});
