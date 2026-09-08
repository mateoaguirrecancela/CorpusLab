import { act, renderHook, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useGuidelinePdf } from '@/modules/project/annotation/hooks/useGuidelinePdf';
import {
  getProjectGuidelinePdf,
  getProjectGuidelinePdfErrorMessage,
} from '@/modules/project/shared/services/projectService';
import { type ProjectDetail } from '@/modules/project/shared/types/project';

vi.mock('@/modules/project/shared/services/projectService', () => ({
  getProjectGuidelinePdf: vi.fn(),
  getProjectGuidelinePdfErrorMessage: vi.fn(() => 'guideline-pdf-error'),
}));

function project(overrides: Partial<ProjectDetail> = {}): ProjectDetail {
  return {
    id: 1,
    guidelinePdfAvailable: true,
    ...overrides,
  } as unknown as ProjectDetail;
}

beforeEach(() => {
  vi.mocked(getProjectGuidelinePdf).mockReset();
  vi.stubGlobal('URL', {
    ...URL,
    createObjectURL: vi.fn(() => 'blob:mock-url'),
    revokeObjectURL: vi.fn(),
  });
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('useGuidelinePdf', () => {
  it('does nothing without a project', () => {
    const { result } = renderHook(() => useGuidelinePdf(undefined));

    expect(result.current).toEqual({ guidelinePdfError: '', guidelinePdfUrl: null });
    expect(getProjectGuidelinePdf).not.toHaveBeenCalled();
  });

  it('does nothing when the project has no guideline pdf', () => {
    const { result } = renderHook(() => useGuidelinePdf(project({ guidelinePdfAvailable: false })));

    expect(result.current).toEqual({ guidelinePdfError: '', guidelinePdfUrl: null });
    expect(getProjectGuidelinePdf).not.toHaveBeenCalled();
  });

  it('loads the pdf and exposes an object url', async () => {
    const blob = new Blob(['pdf'], { type: 'application/pdf' });
    vi.mocked(getProjectGuidelinePdf).mockResolvedValue({
      blob,
      mimeType: 'application/pdf',
      fileName: 'guideline.pdf',
    });

    const { result } = renderHook(() => useGuidelinePdf(project()));

    await waitFor(() => expect(result.current.guidelinePdfUrl).toBe('blob:mock-url'));
    expect(getProjectGuidelinePdf).toHaveBeenCalledWith(1);
    expect(URL.createObjectURL).toHaveBeenCalledWith(blob);
    expect(result.current.guidelinePdfError).toBe('');
  });

  it('exposes the mapped error message when loading fails', async () => {
    vi.mocked(getProjectGuidelinePdf).mockRejectedValue(new Error('boom'));

    const { result } = renderHook(() => useGuidelinePdf(project()));

    await waitFor(() => expect(result.current.guidelinePdfError).toBe('guideline-pdf-error'));
    expect(getProjectGuidelinePdfErrorMessage).toHaveBeenCalled();
    expect(result.current.guidelinePdfUrl).toBeNull();
  });

  it('revokes the object url when the project changes', async () => {
    const blob = new Blob(['pdf'], { type: 'application/pdf' });
    vi.mocked(getProjectGuidelinePdf).mockResolvedValue({
      blob,
      mimeType: 'application/pdf',
      fileName: 'guideline.pdf',
    });

    const { rerender, result } = renderHook(({ p }: { p: ProjectDetail }) => useGuidelinePdf(p), {
      initialProps: { p: project({ id: 1 }) },
    });
    await waitFor(() => expect(result.current.guidelinePdfUrl).toBe('blob:mock-url'));

    act(() => rerender({ p: project({ id: 2, guidelinePdfAvailable: false }) }));

    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:mock-url');
    expect(result.current.guidelinePdfUrl).toBeNull();
  });

  it('ignores a load that resolves after the project changed again', async () => {
    let resolveFirstLoad: (value: { blob: Blob; mimeType: string; fileName: string }) => void =
      () => {};
    vi.mocked(getProjectGuidelinePdf).mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          resolveFirstLoad = resolve;
        }),
    );

    const { rerender, result } = renderHook(({ p }: { p: ProjectDetail }) => useGuidelinePdf(p), {
      initialProps: { p: project({ id: 1 }) },
    });

    act(() => rerender({ p: project({ id: 2, guidelinePdfAvailable: false }) }));
    resolveFirstLoad({
      blob: new Blob(['pdf'], { type: 'application/pdf' }),
      mimeType: 'application/pdf',
      fileName: 'guideline.pdf',
    });
    await Promise.resolve();
    await Promise.resolve();

    expect(result.current.guidelinePdfUrl).toBeNull();
  });
});
