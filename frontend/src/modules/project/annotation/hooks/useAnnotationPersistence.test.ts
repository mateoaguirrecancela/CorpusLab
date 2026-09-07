import { act, renderHook } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { toast } from 'sonner';
import { useAnnotationPersistence } from '@/modules/project/annotation/hooks/useAnnotationPersistence';
import { type AnnotationStep } from '@/modules/project/shared/types/project';

const saveMutateAsync = vi.fn();
const toggleWarningMutateAsync = vi.fn();
const resolveWarningMutateAsync = vi.fn();

vi.mock('sonner', () => ({
  toast: { error: vi.fn(), success: vi.fn() },
}));

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

vi.mock('@/modules/project/shared/services/projectService', () => ({
  getProjectAnnotationSaveErrorMessage: () => 'save-error-message',
}));

vi.mock('@/modules/project/shared/hooks/useProjectQueries', () => ({
  useSaveProjectAnnotationStepMutation: () => ({
    mutateAsync: saveMutateAsync,
    isPending: false,
  }),
  useToggleProjectAnnotationWarningMutation: () => ({
    mutateAsync: toggleWarningMutateAsync,
    isPending: false,
  }),
  useResolveOwnProjectAnnotationWarningMutation: () => ({
    mutateAsync: resolveWarningMutateAsync,
    isPending: false,
  }),
}));

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

function renderPersistence(overrides: Partial<Parameters<typeof useAnnotationPersistence>[0]> = {}) {
  return renderHook(() =>
    useAnnotationPersistence({
      annotationOffset: 0,
      annotationProjectType: 'TEXT_CLASSIFICATION_SIMPLE',
      currentStep: step(),
      getDraft: () => ({ value: 'positive', notes: '', entities: [] }),
      isReviewMode: false,
      numericProjectId: 1,
      reviewedParticipantUserId: null,
      ...overrides,
    }),
  );
}

beforeEach(() => {
  saveMutateAsync.mockReset().mockResolvedValue(undefined);
  toggleWarningMutateAsync.mockReset().mockResolvedValue(undefined);
  resolveWarningMutateAsync.mockReset().mockResolvedValue(undefined);
  vi.mocked(toast.error).mockClear();
  vi.mocked(toast.success).mockClear();
});

describe('runAfterPersist', () => {
  it('calls onSuccess immediately in review mode without saving', async () => {
    const { result } = renderPersistence({ isReviewMode: true });
    const onSuccess = vi.fn();

    await act(() => result.current.runAfterPersist(onSuccess));

    expect(saveMutateAsync).not.toHaveBeenCalled();
    expect(onSuccess).toHaveBeenCalled();
  });

  it('saves the current draft and calls onSuccess on success', async () => {
    const currentStep = step({ datasetItemId: 5, stepIndex: 2 });
    const { result } = renderPersistence({ currentStep });
    const onSuccess = vi.fn();

    await act(() => result.current.runAfterPersist(onSuccess));

    expect(saveMutateAsync).toHaveBeenCalledWith({
      projectId: 1,
      datasetItemId: 5,
      stepIndex: 2,
      annotation: { label: 'positive' },
    });
    expect(onSuccess).toHaveBeenCalled();
  });

  it('shows an error toast and skips onSuccess when saving fails', async () => {
    saveMutateAsync.mockRejectedValue(new Error('network error'));
    const { result } = renderPersistence();
    const onSuccess = vi.fn();

    await act(() => result.current.runAfterPersist(onSuccess));

    expect(toast.error).toHaveBeenCalledWith('save-error-message');
    expect(onSuccess).not.toHaveBeenCalled();
  });

  it('calls onSuccess without saving when there is no current step', async () => {
    const { result } = renderPersistence({ currentStep: null });
    const onSuccess = vi.fn();

    await act(() => result.current.runAfterPersist(onSuccess));

    expect(saveMutateAsync).not.toHaveBeenCalled();
    expect(onSuccess).toHaveBeenCalled();
  });
});

describe('handleBackNavigation', () => {
  it('returns true immediately in review mode', async () => {
    const { result } = renderPersistence({ isReviewMode: true });

    const canNavigateBack = await act(() => result.current.handleBackNavigation());

    expect(canNavigateBack).toBe(true);
    expect(saveMutateAsync).not.toHaveBeenCalled();
  });

  it('saves, shows an autosave toast, and returns true on success', async () => {
    const { result } = renderPersistence();

    const canNavigateBack = await act(() => result.current.handleBackNavigation());

    expect(canNavigateBack).toBe(true);
    expect(toast.success).toHaveBeenCalledWith('project.annotationPage.autoSaved');
  });

  it('returns false and shows an error toast when saving fails', async () => {
    saveMutateAsync.mockRejectedValue(new Error('network error'));
    const { result } = renderPersistence();

    const canNavigateBack = await act(() => result.current.handleBackNavigation());

    expect(canNavigateBack).toBe(false);
    expect(toast.error).toHaveBeenCalledWith('save-error-message');
    expect(toast.success).not.toHaveBeenCalled();
  });
});

describe('handleToggleWarning', () => {
  it('does nothing when there is no current step', async () => {
    const { result } = renderPersistence({ currentStep: null });

    await act(() => result.current.handleToggleWarning());

    expect(toggleWarningMutateAsync).not.toHaveBeenCalled();
    expect(resolveWarningMutateAsync).not.toHaveBeenCalled();
  });

  it('toggles the warning as a reviewer and shows a "marked" toast when activating it', async () => {
    const currentStep = step({ warning: false, datasetItemId: 7, stepIndex: 1 });
    const { result } = renderPersistence({
      currentStep,
      isReviewMode: true,
      reviewedParticipantUserId: 42,
    });

    await act(() => result.current.handleToggleWarning());

    expect(toggleWarningMutateAsync).toHaveBeenCalledWith({
      projectId: 1,
      participantUserId: 42,
      datasetItemId: 7,
      stepIndex: 1,
    });
    expect(toast.success).toHaveBeenCalledWith('project.annotationPage.warningMarked');
  });

  it('shows a "cleared" toast when toggling off an active warning as a reviewer', async () => {
    const currentStep = step({ warning: true });
    const { result } = renderPersistence({
      currentStep,
      isReviewMode: true,
      reviewedParticipantUserId: 42,
    });

    await act(() => result.current.handleToggleWarning());

    expect(toast.success).toHaveBeenCalledWith('project.annotationPage.warningCleared');
  });

  it('resolves its own warning when not in review mode and the step has one', async () => {
    const currentStep = step({ warning: true, datasetItemId: 9, stepIndex: 3 });
    const { result } = renderPersistence({ currentStep });

    await act(() => result.current.handleToggleWarning());

    expect(resolveWarningMutateAsync).toHaveBeenCalledWith({
      projectId: 1,
      datasetItemId: 9,
      stepIndex: 3,
    });
    expect(toast.success).toHaveBeenCalledWith('project.annotationPage.warningResolved');
  });

  it('does not call any mutation when there is no active warning to resolve outside review mode', async () => {
    const currentStep = step({ warning: false });
    const { result } = renderPersistence({ currentStep });

    await act(() => result.current.handleToggleWarning());

    expect(resolveWarningMutateAsync).not.toHaveBeenCalled();
    expect(toggleWarningMutateAsync).not.toHaveBeenCalled();
  });

  it('shows an error toast when the toggle mutation fails', async () => {
    const currentStep = step({ warning: true });
    resolveWarningMutateAsync.mockRejectedValue(new Error('failed'));
    const { result } = renderPersistence({ currentStep });

    await act(() => result.current.handleToggleWarning());

    expect(toast.error).toHaveBeenCalledWith('project.annotationPage.errors.warningToggleFailed');
  });
});
