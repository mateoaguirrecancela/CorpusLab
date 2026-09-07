import { renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useProjectSetupCsvHeaders } from '@/modules/project/setup/hooks/useProjectSetupCsvHeaders';
import {
  extractCsvHeadersFromFile,
  normalizeCsvHeaderSelectionOptions,
} from '@/modules/project/setup/utils/projectSetupCsvUtils';

vi.mock('@/modules/project/setup/utils/projectSetupCsvUtils', () => ({
  extractCsvHeadersFromFile: vi.fn(),
  normalizeCsvHeaderSelectionOptions: vi.fn(),
}));

function csvFile(name = 'data.csv'): File {
  return new File(['text,label'], name, { type: 'text/csv' });
}

function baseParams(overrides: Partial<Parameters<typeof useProjectSetupCsvHeaders>[0]> = {}) {
  return {
    csvDatasetFiles: [csvFile()],
    disableCsvColumnsAsLabels: vi.fn(),
    getAnnotationTargetColumn: vi.fn(() => ''),
    getUseCsvColumnsAsLabels: vi.fn(() => false),
    onHeadersReadError: vi.fn(),
    pruneLabelsMatchingTargetColumn: vi.fn(),
    requiresAnnotationTargetColumn: true,
    setAnnotationTargetColumn: vi.fn(),
    ...overrides,
  };
}

beforeEach(() => {
  vi.mocked(extractCsvHeadersFromFile).mockReset();
  vi.mocked(normalizeCsvHeaderSelectionOptions).mockReset();
});

describe('useProjectSetupCsvHeaders', () => {
  it('clears the target column and disables csv-as-labels when not required', () => {
    const params = baseParams({ requiresAnnotationTargetColumn: false });
    const { result } = renderHook(() => useProjectSetupCsvHeaders(params));

    expect(params.setAnnotationTargetColumn).toHaveBeenCalledWith('');
    expect(params.disableCsvColumnsAsLabels).toHaveBeenCalled();
    expect(extractCsvHeadersFromFile).not.toHaveBeenCalled();
    expect(result.current.csvHeaderOptions).toEqual([]);
  });

  it('loads headers and defaults the target column to the first header', async () => {
    vi.mocked(extractCsvHeadersFromFile).mockResolvedValue(['text', 'label']);
    vi.mocked(normalizeCsvHeaderSelectionOptions).mockReturnValue(['text', 'label']);
    const params = baseParams();

    const { result } = renderHook(() => useProjectSetupCsvHeaders(params));

    await waitFor(() => expect(result.current.csvHeaderOptions).toEqual(['text', 'label']));
    expect(params.setAnnotationTargetColumn).toHaveBeenCalledWith('text');
    expect(result.current.isLoadingCsvHeaders).toBe(false);
  });

  it('keeps the current target column when it is still a valid header', async () => {
    vi.mocked(extractCsvHeadersFromFile).mockResolvedValue(['text', 'label']);
    vi.mocked(normalizeCsvHeaderSelectionOptions).mockReturnValue(['text', 'label']);
    const params = baseParams({ getAnnotationTargetColumn: vi.fn(() => 'label') });

    const { result } = renderHook(() => useProjectSetupCsvHeaders(params));

    await waitFor(() => expect(result.current.csvHeaderOptions).toEqual(['text', 'label']));
    expect(params.setAnnotationTargetColumn).toHaveBeenCalledWith('label');
  });

  it('prunes labels matching the new target column when csv-as-labels is enabled', async () => {
    vi.mocked(extractCsvHeadersFromFile).mockResolvedValue(['text', 'label']);
    vi.mocked(normalizeCsvHeaderSelectionOptions).mockReturnValue(['text', 'label']);
    const params = baseParams({ getUseCsvColumnsAsLabels: vi.fn(() => true) });

    renderHook(() => useProjectSetupCsvHeaders(params));

    await waitFor(() => expect(params.pruneLabelsMatchingTargetColumn).toHaveBeenCalledWith('text'));
  });

  it('reports the read error and clears state when parsing fails', async () => {
    vi.mocked(extractCsvHeadersFromFile).mockRejectedValue(new Error('bad csv'));
    const params = baseParams();

    const { result } = renderHook(() => useProjectSetupCsvHeaders(params));

    await waitFor(() => expect(params.onHeadersReadError).toHaveBeenCalled());
    expect(result.current.csvHeaderOptions).toEqual([]);
    expect(params.setAnnotationTargetColumn).toHaveBeenCalledWith('');
  });
});
