import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { toast } from 'sonner';
import { useParams } from 'react-router';
import { useProjectDetailPage } from '@/modules/project/core/hooks/useProjectDetailPage';
import {
  useArchiveProjectMutation,
  useProjectDetailQuery,
  useUnarchiveProjectMutation,
} from '@/modules/project/shared/hooks/useProjectQueries';
import {
  exportProjectAnnotationResultsCsv,
  getArchiveProjectErrorMessage,
  getProjectAnnotationExportErrorMessage,
  getProjectDatasetItemContent,
  getProjectDatasetItemContentErrorMessage,
  getProjectDetailLoadErrorMessage,
  getProjectGuidelinePdf,
  getProjectGuidelinePdfErrorMessage,
  getUnarchiveProjectErrorMessage,
} from '@/modules/project/shared/services/projectService';
import { triggerBlobDownload } from '@/modules/project/shared/utils/fileDownloadUtils';
import { type ProjectDetail } from '@/modules/project/shared/types/project';

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

vi.mock('react-router', () => ({
  useParams: vi.fn(),
}));

vi.mock('sonner', () => ({
  toast: { error: vi.fn(), success: vi.fn() },
}));

vi.mock('@/modules/project/shared/hooks/useProjectQueries', () => ({
  useArchiveProjectMutation: vi.fn(),
  useProjectDetailQuery: vi.fn(),
  useUnarchiveProjectMutation: vi.fn(),
}));

vi.mock('@/modules/project/shared/services/projectService', () => ({
  exportProjectAnnotationResultsCsv: vi.fn(),
  getArchiveProjectErrorMessage: vi.fn(() => 'archive-error'),
  getProjectAnnotationExportErrorMessage: vi.fn(() => 'export-error'),
  getProjectDatasetItemContent: vi.fn(),
  getProjectDatasetItemContentErrorMessage: vi.fn(() => 'dataset-item-error'),
  getProjectDetailLoadErrorMessage: vi.fn(() => 'detail-load-error'),
  getProjectGuidelinePdf: vi.fn(),
  getProjectGuidelinePdfErrorMessage: vi.fn(() => 'guideline-pdf-error'),
  getUnarchiveProjectErrorMessage: vi.fn(() => 'unarchive-error'),
}));

vi.mock('@/modules/project/shared/utils/fileDownloadUtils', () => ({
  triggerBlobDownload: vi.fn(),
}));

const archiveMutateAsync = vi.fn();
const unarchiveMutateAsync = vi.fn();

function project(overrides: Partial<ProjectDetail> = {}): ProjectDetail {
  return {
    id: 1,
    name: 'Project',
    completionPercentage: 40,
    archived: false,
    canArchiveProject: true,
    canExportAnnotations: true,
    guidelinePdfAvailable: false,
    guidelinePdfMimeType: null,
    guidelinePdfSizeBytes: 0,
    ...overrides,
  } as unknown as ProjectDetail;
}

function mockDetailQuery(overrides: Record<string, unknown> = {}) {
  vi.mocked(useProjectDetailQuery).mockReturnValue({
    data: project(),
    isLoading: false,
    isError: false,
    error: null,
    ...overrides,
  } as never);
}

beforeEach(() => {
  archiveMutateAsync.mockReset().mockResolvedValue(undefined);
  unarchiveMutateAsync.mockReset().mockResolvedValue(undefined);
  vi.mocked(toast.error).mockClear();
  vi.mocked(toast.success).mockClear();
  vi.mocked(triggerBlobDownload).mockClear();
  vi.mocked(useArchiveProjectMutation).mockReturnValue({
    mutateAsync: archiveMutateAsync,
    isPending: false,
  } as never);
  vi.mocked(useUnarchiveProjectMutation).mockReturnValue({
    mutateAsync: unarchiveMutateAsync,
    isPending: false,
  } as never);
  vi.stubGlobal('open', vi.fn());
  vi.stubGlobal('URL', { ...URL, createObjectURL: vi.fn(() => 'blob:mock-url'), revokeObjectURL: vi.fn() });
  vi.mocked(useParams).mockReturnValue({ projectId: '1' } as never);
  mockDetailQuery();
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('guideline pdf metadata', () => {
  it('is empty when no guideline pdf is available', () => {
    mockDetailQuery({ data: project({ guidelinePdfAvailable: false }) });
    const { result } = renderHook(() => useProjectDetailPage());
    expect(result.current.guidelinePdfMetadata).toBe('');
  });

  it('shows the mime type and formatted size when available', () => {
    mockDetailQuery({
      data: project({
        guidelinePdfAvailable: true,
        guidelinePdfMimeType: 'application/pdf',
        guidelinePdfSizeBytes: 2048,
      }),
    });
    const { result } = renderHook(() => useProjectDetailPage());
    expect(result.current.guidelinePdfMetadata).toBe('application/pdf - 2.0 KB');
  });

  it('shows just the mime type when the size is unknown', () => {
    mockDetailQuery({
      data: project({ guidelinePdfAvailable: true, guidelinePdfMimeType: 'application/pdf', guidelinePdfSizeBytes: 0 }),
    });
    const { result } = renderHook(() => useProjectDetailPage());
    expect(result.current.guidelinePdfMetadata).toBe('application/pdf');
  });
});

describe('detail error message', () => {
  it('reports an invalid id without calling the detail error mapper', () => {
    vi.mocked(useParams).mockReturnValue({ projectId: 'not-a-number' } as never);
    mockDetailQuery({ data: undefined });

    const { result } = renderHook(() => useProjectDetailPage());

    expect(result.current.detailErrorMessage).toBe('project.detail.invalidId');
    expect(getProjectDetailLoadErrorMessage).not.toHaveBeenCalled();
    expect(toast.error).toHaveBeenCalledWith('project.detail.invalidId', {
      id: 'project-detail-load-error',
    });
  });

  it('maps a query error to the project detail error message', () => {
    mockDetailQuery({ data: undefined, isError: true, error: new Error('boom') });

    const { result } = renderHook(() => useProjectDetailPage());

    expect(getProjectDetailLoadErrorMessage).toHaveBeenCalled();
    expect(result.current.detailErrorMessage).toBe('detail-load-error');
  });
});

describe('archiving', () => {
  it('archives an active project and shows a success toast', async () => {
    mockDetailQuery({ data: project({ archived: false }) });
    const { result } = renderHook(() => useProjectDetailPage());

    await act(async () => {
      await result.current.handleArchiveAction();
    });

    expect(archiveMutateAsync).toHaveBeenCalledWith({ projectId: 1 });
    expect(toast.success).toHaveBeenCalledWith('project.detail.archiveSuccess');
    expect(result.current.projectActionsOpen).toBe(false);
  });

  it('unarchives an archived project', async () => {
    mockDetailQuery({ data: project({ archived: true }) });
    const { result } = renderHook(() => useProjectDetailPage());

    await act(async () => {
      await result.current.handleArchiveAction();
    });

    expect(unarchiveMutateAsync).toHaveBeenCalledWith({ projectId: 1 });
    expect(toast.success).toHaveBeenCalledWith('project.detail.unarchiveSuccess');
  });

  it('does nothing when the project cannot be archived', async () => {
    mockDetailQuery({ data: project({ canArchiveProject: false }) });
    const { result } = renderHook(() => useProjectDetailPage());

    await act(async () => {
      await result.current.handleArchiveAction();
    });

    expect(archiveMutateAsync).not.toHaveBeenCalled();
    expect(unarchiveMutateAsync).not.toHaveBeenCalled();
  });

  it('shows the archive-specific error message on failure', async () => {
    archiveMutateAsync.mockRejectedValue(new Error('boom'));
    mockDetailQuery({ data: project({ archived: false }) });
    const { result } = renderHook(() => useProjectDetailPage());

    await act(async () => {
      await result.current.handleArchiveAction();
    });

    expect(getArchiveProjectErrorMessage).toHaveBeenCalled();
    expect(getUnarchiveProjectErrorMessage).not.toHaveBeenCalled();
    expect(toast.error).toHaveBeenCalledWith('archive-error');
  });
});

describe('guideline pdf actions', () => {
  it('does nothing when there is no guideline pdf', async () => {
    mockDetailQuery({ data: project({ guidelinePdfAvailable: false }) });
    const { result } = renderHook(() => useProjectDetailPage());

    await act(async () => {
      await result.current.openGuidelinePdf();
    });

    expect(getProjectGuidelinePdf).not.toHaveBeenCalled();
  });

  it('opens the pdf in a new tab', async () => {
    const blob = new Blob(['pdf']);
    vi.mocked(getProjectGuidelinePdf).mockResolvedValue({ blob, mimeType: 'application/pdf', fileName: null });
    mockDetailQuery({ data: project({ guidelinePdfAvailable: true }) });
    const { result } = renderHook(() => useProjectDetailPage());

    await act(async () => {
      await result.current.openGuidelinePdf();
    });

    expect(globalThis.open).toHaveBeenCalledWith('blob:mock-url', '_blank', 'noopener,noreferrer');
  });

  it('downloads the pdf with a fallback file name', async () => {
    const blob = new Blob(['pdf']);
    vi.mocked(getProjectGuidelinePdf).mockResolvedValue({ blob, mimeType: 'application/pdf', fileName: null });
    mockDetailQuery({ data: project({ guidelinePdfAvailable: true }) });
    const { result } = renderHook(() => useProjectDetailPage());

    await act(async () => {
      await result.current.downloadGuidelinePdf();
    });

    expect(triggerBlobDownload).toHaveBeenCalledWith(blob, 'guideline.pdf');
  });

  it('shows an error toast when loading the pdf fails', async () => {
    vi.mocked(getProjectGuidelinePdf).mockRejectedValue(new Error('boom'));
    mockDetailQuery({ data: project({ guidelinePdfAvailable: true }) });
    const { result } = renderHook(() => useProjectDetailPage());

    await act(async () => {
      await result.current.downloadGuidelinePdf();
    });

    expect(getProjectGuidelinePdfErrorMessage).toHaveBeenCalled();
    expect(toast.error).toHaveBeenCalledWith('guideline-pdf-error');
  });
});

describe('dataset file actions', () => {
  it('downloads a dataset file, falling back to a generated name', async () => {
    const blob = new Blob(['data']);
    vi.mocked(getProjectDatasetItemContent).mockResolvedValue({ blob, mimeType: 'text/csv', fileName: null });
    const { result } = renderHook(() => useProjectDetailPage());

    await act(async () => {
      await result.current.downloadDatasetFile(3, '  ');
    });

    expect(triggerBlobDownload).toHaveBeenCalledWith(blob, 'dataset-item-3');
  });

  it('shows an error toast when the dataset file fails to load', async () => {
    vi.mocked(getProjectDatasetItemContent).mockRejectedValue(new Error('boom'));
    const { result } = renderHook(() => useProjectDetailPage());

    await act(async () => {
      await result.current.openDatasetFile(3);
    });

    expect(getProjectDatasetItemContentErrorMessage).toHaveBeenCalled();
    expect(toast.error).toHaveBeenCalledWith('dataset-item-error');
  });
});

describe('csv export', () => {
  it('is gated by canExportAnnotations', async () => {
    mockDetailQuery({ data: project({ canExportAnnotations: false }) });
    const { result } = renderHook(() => useProjectDetailPage());

    await act(async () => {
      await result.current.downloadAnnotationResultsCsv();
    });

    expect(exportProjectAnnotationResultsCsv).not.toHaveBeenCalled();
  });

  it('exports and downloads the csv with a generated file name', async () => {
    const blob = new Blob(['csv']);
    vi.mocked(exportProjectAnnotationResultsCsv).mockResolvedValue({ blob, mimeType: 'text/csv', fileName: null });
    const { result } = renderHook(() => useProjectDetailPage());

    await act(async () => {
      await result.current.downloadAnnotationResultsCsv();
    });

    expect(triggerBlobDownload).toHaveBeenCalledWith(blob, 'project-1-annotations.csv');
  });

  it('shows an error toast when export fails', async () => {
    vi.mocked(exportProjectAnnotationResultsCsv).mockRejectedValue(new Error('boom'));
    const { result } = renderHook(() => useProjectDetailPage());

    await act(async () => {
      await result.current.downloadAnnotationResultsCsv();
    });

    expect(getProjectAnnotationExportErrorMessage).toHaveBeenCalled();
    expect(toast.error).toHaveBeenCalledWith('export-error');
  });
});
