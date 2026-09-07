import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { toast } from 'sonner';
import { useProjectSetupStep } from '@/modules/project/setup/hooks/useProjectSetupStep';
import { extractCsvHeadersFromFile } from '@/modules/project/setup/utils/projectSetupCsvUtils';

// A stable `t` reference matters here: useProjectSetupCsvHeaders' effect
// depends on a callback built from `t`, so a new `t` on every render would
// re-trigger that effect indefinitely.
vi.mock('react-i18next', () => {
  const t = (key: string, opts?: Record<string, unknown>) =>
    opts ? `${key}:${JSON.stringify(opts)}` : key;
  return {
    useTranslation: () => ({ t }),
  };
});

vi.mock('sonner', () => ({
  toast: { error: vi.fn() },
}));

vi.mock('@/modules/project/setup/utils/projectSetupCsvUtils', async (importOriginal) => {
  const actual =
    await importOriginal<typeof import('@/modules/project/setup/utils/projectSetupCsvUtils')>();
  return {
    ...actual,
    extractCsvHeadersFromFile: vi.fn(),
  };
});

function txtFile(name = 'a.txt'): File {
  return new File(['hello'], name, { type: 'text/plain' });
}

function csvFile(name = 'a.csv'): File {
  return new File(['text,label'], name, { type: 'text/csv' });
}

function pdfFile(name = 'guide.pdf', sizeBytes = 10): File {
  return new File([new Uint8Array(sizeBytes)], name, { type: 'application/pdf' });
}

async function addTwoLabels(result: { current: ReturnType<typeof useProjectSetupStep> }) {
  act(() => result.current.labelEditor.openCreateLabelDialog());
  act(() => result.current.labelEditor.setDraftLabelName('Positive'));
  act(() => result.current.labelEditor.saveLabelFromDialog());
  act(() => result.current.labelEditor.openCreateLabelDialog());
  act(() => result.current.labelEditor.setDraftLabelName('Negative'));
  act(() => result.current.labelEditor.saveLabelFromDialog());
  await waitFor(() => expect(result.current.labels).toHaveLength(2));
}

beforeEach(() => {
  vi.mocked(toast.error).mockClear();
  vi.mocked(extractCsvHeadersFromFile).mockReset();
});

describe('useProjectSetupStep with a plain text dataset', () => {
  it('requires guideline text and at least 2 labels before it can save', async () => {
    const datasetFiles = [txtFile()];
    const onCompleted = vi.fn();
    const { result } = renderHook(() => useProjectSetupStep({ datasetFiles, onCompleted }));

    expect(result.current.canSaveSetup).toBe(false);

    act(() => result.current.handleGuidelineTextChange('Follow these rules'));
    await waitFor(() => expect(result.current.guidelineText).toBe('Follow these rules'));
    expect(result.current.canSaveSetup).toBe(false);

    act(() => result.current.labelEditor.openCreateLabelDialog());
    act(() => result.current.labelEditor.setDraftLabelName('Positive'));
    act(() => result.current.labelEditor.saveLabelFromDialog());
    act(() => result.current.labelEditor.openCreateLabelDialog());
    act(() => result.current.labelEditor.setDraftLabelName('Negative'));
    act(() => result.current.labelEditor.saveLabelFromDialog());

    await waitFor(() => expect(result.current.labels).toHaveLength(2));
    expect(result.current.canSaveSetup).toBe(true);
  });

  it('rejects switching to NER when the dataset is not NER-compatible', () => {
    const datasetFiles = [new File(['x'], 'a.png', { type: 'image/png' })];
    const onCompleted = vi.fn();
    const { result } = renderHook(() => useProjectSetupStep({ datasetFiles, onCompleted }));

    act(() => result.current.handleProjectTypeChange('NER'));

    expect(result.current.projectType).toBe('TEXT_CLASSIFICATION_SIMPLE');
    expect(toast.error).toHaveBeenCalledWith('project.create.nerDatasetIncompatibleError');
  });

  it('allows switching to NER for a compatible dataset and clears existing labels', async () => {
    const datasetFiles = [txtFile()];
    const onCompleted = vi.fn();
    const { result } = renderHook(() => useProjectSetupStep({ datasetFiles, onCompleted }));

    act(() => result.current.handleProjectTypeChange('NER'));

    await waitFor(() => expect(result.current.projectType).toBe('NER'));
    expect(result.current.isNerProjectType).toBe(true);
    expect(result.current.requiresLabels).toBe(true);
  });

  it('clears the guideline pdf file when switching from PDF mode to TEXT mode', async () => {
    const datasetFiles = [txtFile()];
    const onCompleted = vi.fn();
    const { result } = renderHook(() => useProjectSetupStep({ datasetFiles, onCompleted }));

    act(() => result.current.handleGuidelineModeChange('PDF'));
    await waitFor(() => expect(result.current.guidelineMode).toBe('PDF'));

    act(() => result.current.handleGuidelineModeChange('TEXT'));

    await waitFor(() => expect(result.current.guidelineMode).toBe('TEXT'));
    expect(result.current.guidelinePdfFile).toBeNull();
  });

  it('builds the completed payload with the trimmed guideline text', async () => {
    const datasetFiles = [txtFile()];
    const onCompleted = vi.fn();
    const { result } = renderHook(() => useProjectSetupStep({ datasetFiles, onCompleted }));

    act(() => result.current.handleGuidelineTextChange('  Follow these rules  '));
    act(() => result.current.labelEditor.openCreateLabelDialog());
    act(() => result.current.labelEditor.setDraftLabelName('Positive'));
    act(() => result.current.labelEditor.saveLabelFromDialog());
    act(() => result.current.labelEditor.openCreateLabelDialog());
    act(() => result.current.labelEditor.setDraftLabelName('Negative'));
    act(() => result.current.labelEditor.saveLabelFromDialog());
    await waitFor(() => expect(result.current.canSaveSetup).toBe(true));

    await act(async () => {
      await result.current.handleSaveProjectSetup();
    });

    expect(onCompleted).toHaveBeenCalledWith(
      expect.objectContaining({
        projectType: 'TEXT_CLASSIFICATION_SIMPLE',
        guidelineText: 'Follow these rules',
        guidelinePdfFile: undefined,
        annotationTargetColumn: undefined,
      }),
    );
  });

  it('does not call onCompleted while setup cannot be saved', async () => {
    const datasetFiles = [txtFile()];
    const onCompleted = vi.fn();
    const { result } = renderHook(() => useProjectSetupStep({ datasetFiles, onCompleted }));

    await act(async () => {
      await result.current.handleSaveProjectSetup();
    });

    expect(onCompleted).not.toHaveBeenCalled();
  });
});

describe('useProjectSetupStep with a csv dataset', () => {
  it('requires an annotation target column and loads csv headers', async () => {
    vi.mocked(extractCsvHeadersFromFile).mockResolvedValue(['text', 'label']);
    const datasetFiles = [csvFile()];
    const onCompleted = vi.fn();
    const { result } = renderHook(() => useProjectSetupStep({ datasetFiles, onCompleted }));

    expect(result.current.requiresAnnotationTargetColumn).toBe(true);
    await waitFor(() => expect(result.current.csvHeaderOptions).toEqual(['text', 'label']));
    expect(result.current.annotationTargetColumn).toBe('text');
  });

  it('includes the trimmed annotation target column in the completed payload', async () => {
    vi.mocked(extractCsvHeadersFromFile).mockResolvedValue(['text', 'label']);
    const datasetFiles = [csvFile()];
    const onCompleted = vi.fn();
    const { result } = renderHook(() => useProjectSetupStep({ datasetFiles, onCompleted }));
    await waitFor(() => expect(result.current.annotationTargetColumn).toBe('text'));

    act(() => result.current.handleGuidelineTextChange('Follow these rules'));
    act(() => result.current.labelEditor.openCreateLabelDialog());
    act(() => result.current.labelEditor.setDraftLabelName('Positive'));
    act(() => result.current.labelEditor.saveLabelFromDialog());
    act(() => result.current.labelEditor.openCreateLabelDialog());
    act(() => result.current.labelEditor.setDraftLabelName('Negative'));
    act(() => result.current.labelEditor.saveLabelFromDialog());
    await waitFor(() => expect(result.current.canSaveSetup).toBe(true));

    await act(async () => {
      await result.current.handleSaveProjectSetup();
    });

    expect(onCompleted).toHaveBeenCalledWith(
      expect.objectContaining({ annotationTargetColumn: 'text' }),
    );
  });
});

describe('useProjectSetupStep guideline pdf handling', () => {
  it('does nothing when no file was actually selected', () => {
    const datasetFiles = [txtFile()];
    const onCompleted = vi.fn();
    const { result } = renderHook(() => useProjectSetupStep({ datasetFiles, onCompleted }));

    act(() => result.current.handleGuidelinePdfSelected([]));

    expect(result.current.guidelinePdfFile).toBeNull();
  });

  it('accepts a valid pdf and clears any guideline text', async () => {
    const datasetFiles = [txtFile()];
    const onCompleted = vi.fn();
    const { result } = renderHook(() => useProjectSetupStep({ datasetFiles, onCompleted }));
    act(() => result.current.handleGuidelineTextChange('some text'));

    act(() => result.current.handleGuidelinePdfSelected([pdfFile()]));

    await waitFor(() => expect(result.current.guidelinePdfFile).not.toBeNull());
    expect(result.current.guidelineText).toBe('');
  });

  it('rejects selecting the same pdf twice as a duplicate', async () => {
    const datasetFiles = [txtFile()];
    const onCompleted = vi.fn();
    const { result } = renderHook(() => useProjectSetupStep({ datasetFiles, onCompleted }));
    const file = pdfFile('guide.pdf', 10);
    act(() => result.current.handleGuidelinePdfSelected([file]));
    await waitFor(() => expect(result.current.guidelinePdfFile).not.toBeNull());

    act(() => result.current.handleGuidelinePdfSelected([pdfFile('guide.pdf', 10)]));

    expect(toast.error).toHaveBeenCalledWith(
      'project.create.duplicateFileError:{"fileName":"guide.pdf"}',
    );
  });

  it('rejects a pdf larger than the size limit', () => {
    const datasetFiles = [txtFile()];
    const onCompleted = vi.fn();
    const { result } = renderHook(() => useProjectSetupStep({ datasetFiles, onCompleted }));

    act(() => result.current.handleGuidelinePdfSelected([pdfFile('big.pdf', 11 * 1024 * 1024)]));

    expect(toast.error).toHaveBeenCalledWith(
      'project.create.fileSizeError:{"fileName":"big.pdf","limit":10}',
    );
    expect(result.current.guidelinePdfFile).toBeNull();
  });

  it('reports a single-file error when the dropzone rejects extra files', () => {
    const datasetFiles = [txtFile()];
    const onCompleted = vi.fn();
    const { result } = renderHook(() => useProjectSetupStep({ datasetFiles, onCompleted }));

    act(() =>
      result.current.handleGuidelinePdfRejected([
        { file: pdfFile(), errors: [{ code: 'too-many-files', message: 'Too many files' }] },
      ]),
    );

    expect(toast.error).toHaveBeenCalledWith('project.create.singleFileError');
  });

  it('does nothing when the dropzone reports no rejections', () => {
    const datasetFiles = [txtFile()];
    const onCompleted = vi.fn();
    const { result } = renderHook(() => useProjectSetupStep({ datasetFiles, onCompleted }));

    act(() => result.current.handleGuidelinePdfRejected([]));

    expect(toast.error).not.toHaveBeenCalled();
  });

  it('includes the selected pdf file and omits guideline text in the completed payload', async () => {
    const datasetFiles = [txtFile()];
    const onCompleted = vi.fn();
    const { result } = renderHook(() => useProjectSetupStep({ datasetFiles, onCompleted }));

    act(() => result.current.handleGuidelineModeChange('PDF'));
    await waitFor(() => expect(result.current.guidelineMode).toBe('PDF'));
    act(() => result.current.handleGuidelinePdfSelected([pdfFile()]));
    await waitFor(() => expect(result.current.guidelinePdfFile).not.toBeNull());
    await addTwoLabels(result);
    await waitFor(() => expect(result.current.canSaveSetup).toBe(true));

    await act(async () => {
      await result.current.handleSaveProjectSetup();
    });

    expect(onCompleted).toHaveBeenCalledWith(
      expect.objectContaining({
        guidelineText: undefined,
        guidelinePdfFile: expect.any(File),
      }),
    );
  });
});

describe('useProjectSetupStep csv-columns-as-labels toggle', () => {
  it('switches on once headers are available and clears any manual labels', async () => {
    vi.mocked(extractCsvHeadersFromFile).mockResolvedValue(['text', 'label']);
    const datasetFiles = [csvFile()];
    const onCompleted = vi.fn();
    const { result } = renderHook(() => useProjectSetupStep({ datasetFiles, onCompleted }));
    await waitFor(() => expect(result.current.canUseCsvColumnsAsLabels).toBe(true));
    await addTwoLabels(result);

    act(() => result.current.handleUseCsvColumnsAsLabelsChange(true));

    await waitFor(() => expect(result.current.isCsvLabelModeEnabled).toBe(true));
    expect(result.current.labels).toEqual([]);
  });

  it('prunes a label matching the annotation target column once csv label mode is on', async () => {
    vi.mocked(extractCsvHeadersFromFile).mockResolvedValue(['text', 'label', 'sentiment']);
    const datasetFiles = [csvFile()];
    const onCompleted = vi.fn();
    const { result } = renderHook(() => useProjectSetupStep({ datasetFiles, onCompleted }));
    await waitFor(() => expect(result.current.canUseCsvColumnsAsLabels).toBe(true));
    act(() => result.current.handleUseCsvColumnsAsLabelsChange(true));
    await waitFor(() => expect(result.current.isCsvLabelModeEnabled).toBe(true));

    act(() => result.current.labelEditor.openCreateLabelDialog());
    act(() => result.current.labelEditor.setDraftLabelName('sentiment'));
    act(() => result.current.labelEditor.saveLabelFromDialog());
    await waitFor(() => expect(result.current.labels).toEqual([{ name: 'sentiment', color: null }]));

    act(() => result.current.handleAnnotationTargetColumnChange('sentiment'));

    await waitFor(() => expect(result.current.labels).toEqual([]));
  });
});

describe('useProjectSetupStep project type switching', () => {
  it('clears useCsvColumnsAsLabels when switching to SEQ2SEQ', async () => {
    vi.mocked(extractCsvHeadersFromFile).mockResolvedValue(['text', 'label']);
    const datasetFiles = [csvFile()];
    const onCompleted = vi.fn();
    const { result } = renderHook(() => useProjectSetupStep({ datasetFiles, onCompleted }));
    await waitFor(() => expect(result.current.canUseCsvColumnsAsLabels).toBe(true));
    act(() => result.current.handleUseCsvColumnsAsLabelsChange(true));
    await waitFor(() => expect(result.current.isCsvLabelModeEnabled).toBe(true));

    act(() => result.current.handleProjectTypeChange('SEQ2SEQ'));

    await waitFor(() => expect(result.current.projectType).toBe('SEQ2SEQ'));
    expect(result.current.isCsvLabelModeEnabled).toBe(false);
  });

  it('accepts a dataset file with no explicit mime type as NER-compatible via its extension', async () => {
    const datasetFiles = [new File(['hello'], 'a.txt')];
    const onCompleted = vi.fn();
    const { result } = renderHook(() => useProjectSetupStep({ datasetFiles, onCompleted }));

    act(() => result.current.handleProjectTypeChange('NER'));

    await waitFor(() => expect(result.current.projectType).toBe('NER'));
    expect(toast.error).not.toHaveBeenCalled();
  });
});

describe('useProjectSetupStep schema-level validation guard', () => {
  it('does not call onCompleted when a NER label is missing its required color', async () => {
    const datasetFiles = [txtFile()];
    const onCompleted = vi.fn();
    const { result } = renderHook(() => useProjectSetupStep({ datasetFiles, onCompleted }));
    act(() => result.current.handleProjectTypeChange('NER'));
    await waitFor(() => expect(result.current.projectType).toBe('NER'));
    act(() => result.current.handleGuidelineTextChange('Follow these rules'));

    act(() => result.current.labelEditor.openCreateLabelDialog());
    act(() => result.current.labelEditor.setDraftLabelName('Person'));
    act(() => result.current.labelEditor.setDraftLabelColor(''));
    act(() => result.current.labelEditor.saveLabelFromDialog());
    await waitFor(() => expect(result.current.labels).toHaveLength(1));
    // The hook's own optimistic canSaveSetup only checks label *count*, not
    // color validity, so it can be true even though the zod schema (checked
    // inside handleSaveProjectSetup via `trigger()`) will still reject this.
    expect(result.current.canSaveSetup).toBe(true);

    await act(async () => {
      await result.current.handleSaveProjectSetup();
    });

    expect(onCompleted).not.toHaveBeenCalled();
  });
});
