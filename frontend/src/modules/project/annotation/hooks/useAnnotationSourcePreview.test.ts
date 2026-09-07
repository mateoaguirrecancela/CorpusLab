import { act, renderHook, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useAnnotationSourcePreview } from '@/modules/project/annotation/hooks/useAnnotationSourcePreview';
import {
  getProjectDatasetItemContent,
  getProjectDatasetItemContentErrorMessage,
} from '@/modules/project/shared/services/projectService';
import { type AnnotationStep, type ProjectDatasetItemContent } from '@/modules/project/shared/types/project';

vi.mock('@/modules/project/shared/services/projectService', () => ({
  getProjectDatasetItemContent: vi.fn(),
  getProjectDatasetItemContentErrorMessage: vi.fn(() => 'dataset-item-error'),
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

function textContent(text: string, fileName = 'file.txt'): ProjectDatasetItemContent {
  return {
    blob: new Blob([text], { type: 'text/plain' }),
    mimeType: 'text/plain',
    fileName,
  };
}

beforeEach(() => {
  vi.mocked(getProjectDatasetItemContent).mockReset();
  vi.stubGlobal('URL', {
    ...URL,
    createObjectURL: vi.fn(() => 'blob:mock-url'),
    revokeObjectURL: vi.fn(),
  });
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('useAnnotationSourcePreview', () => {
  it('is empty when there is no current step', () => {
    const { result } = renderHook(() => useAnnotationSourcePreview(null, 1));

    expect(result.current.isSourceLoading).toBe(false);
    expect(result.current.sourceUrl).toBeNull();
    expect(getProjectDatasetItemContent).not.toHaveBeenCalled();
  });

  it('skips fetching for a csv step, using the step data directly', () => {
    const csvStep = step({ sourceMimeType: 'text/csv' });
    const { result } = renderHook(() => useAnnotationSourcePreview(csvStep, 1));

    expect(getProjectDatasetItemContent).not.toHaveBeenCalled();
    expect(result.current.sourceFileName).toBe('file.txt');
    expect(result.current.isSourceLoading).toBe(false);
  });

  it('loads and caches a text source, exposing its content', async () => {
    vi.mocked(getProjectDatasetItemContent).mockResolvedValue(textContent('hello world'));
    const currentStep = step();
    const { result } = renderHook(() => useAnnotationSourcePreview(currentStep, 5));

    expect(result.current.isSourceLoading).toBe(true);

    await waitFor(() => expect(result.current.isSourceLoading).toBe(false));
    expect(result.current.sourceTextContent).toBe('hello world');
    expect(getProjectDatasetItemContent).toHaveBeenCalledWith(5, 1);
  });

  it('reuses the cached source on a second visit to the same dataset item', async () => {
    vi.mocked(getProjectDatasetItemContent).mockResolvedValue(textContent('hello world'));
    const { result, rerender } = renderHook(
      ({ s }: { s: AnnotationStep | null }) => useAnnotationSourcePreview(s, 5),
      { initialProps: { s: step() } },
    );
    await waitFor(() => expect(result.current.isSourceLoading).toBe(false));
    expect(getProjectDatasetItemContent).toHaveBeenCalledTimes(1);

    // Navigate to an unrelated (non-fetched) step and back to the same dataset item.
    act(() =>
      rerender({ s: step({ datasetItemId: 2, sourceMimeType: 'text/csv', sourceName: 'other.csv' }) }),
    );
    expect(result.current.sourceFileName).toBe('other.csv');
    act(() => rerender({ s: step() }));

    expect(result.current.sourceTextContent).toBe('hello world');
    expect(getProjectDatasetItemContent).toHaveBeenCalledTimes(1);
  });

  it('exposes the mapped error message when loading fails, without crashing', async () => {
    vi.mocked(getProjectDatasetItemContent).mockRejectedValue(new Error('boom'));
    const currentStep = step();
    const { result } = renderHook(() => useAnnotationSourcePreview(currentStep, 5));

    await waitFor(() => expect(result.current.sourceLoadError).toBe('dataset-item-error'));
    expect(getProjectDatasetItemContentErrorMessage).toHaveBeenCalled();
    expect(result.current.isSourceLoading).toBe(false);
  });

  it('ignores a response that resolves after the step changed again', async () => {
    vi.mocked(getProjectDatasetItemContent).mockResolvedValue(
      textContent('second item content', 'file2.txt'),
    );
    let resolveFirstLoad: (value: ProjectDatasetItemContent) => void = () => {};
    vi.mocked(getProjectDatasetItemContent).mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          resolveFirstLoad = resolve;
        }),
    );
    const { result, rerender } = renderHook(
      ({ s }: { s: AnnotationStep }) => useAnnotationSourcePreview(s, 5),
      { initialProps: { s: step({ datasetItemId: 1 }) } },
    );

    act(() => rerender({ s: step({ datasetItemId: 2, stepIndex: 1 }) }));
    await waitFor(() => expect(result.current.sourceTextContent).toBe('second item content'));

    resolveFirstLoad(textContent('stale content'));
    await Promise.resolve();
    await Promise.resolve();
    await Promise.resolve();

    expect(result.current.sourceTextContent).toBe('second item content');
  });

  it('revokes every cached object url on unmount', async () => {
    vi.mocked(getProjectDatasetItemContent).mockResolvedValue({
      blob: new Blob(['binary'], { type: 'image/png' }),
      mimeType: 'image/png',
      fileName: 'photo.png',
    });
    const imageStep = step({ sourceMimeType: 'image/png', sourceName: 'photo.png' });
    const { result, unmount } = renderHook(() => useAnnotationSourcePreview(imageStep, 5));

    await waitFor(() => expect(result.current.sourceUrl).toBe('blob:mock-url'));

    unmount();

    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:mock-url');
  });
});
