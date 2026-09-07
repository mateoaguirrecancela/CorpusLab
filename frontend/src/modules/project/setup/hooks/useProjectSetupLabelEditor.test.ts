import { act, renderHook } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { useProjectSetupLabelEditor } from '@/modules/project/setup/hooks/useProjectSetupLabelEditor';
import { type ProjectSetupLabel } from '@/modules/project/shared/types/project';

function baseParams(overrides: Partial<Parameters<typeof useProjectSetupLabelEditor>[0]> = {}) {
  return {
    annotationTargetColumn: 'text',
    csvHeaderOptions: ['text', 'label', 'sentiment'],
    isCsvLabelModeEnabled: false,
    isNerProjectType: false,
    labels: [] as ProjectSetupLabel[],
    onDuplicateLabel: vi.fn(),
    setLabelsValue: vi.fn(),
    ...overrides,
  };
}

describe('dialog open/close', () => {
  it('opens the create dialog with a blank draft', () => {
    const params = baseParams({ labels: [{ name: 'Positive', color: null }] });
    const { result } = renderHook(() => useProjectSetupLabelEditor(params));

    act(() => result.current.openCreateLabelDialog());

    expect(result.current.isLabelEditorOpen).toBe(true);
    expect(result.current.labelDialogMode).toBe('create');
    expect(result.current.draftLabelName).toBe('');
  });

  it('opens the edit dialog pre-filled with the target label', () => {
    const params = baseParams({ labels: [{ name: 'Positive', color: '#ff0000' }] });
    const { result } = renderHook(() => useProjectSetupLabelEditor(params));

    act(() => result.current.openEditLabelDialog(0));

    expect(result.current.labelDialogMode).toBe('edit');
    expect(result.current.draftLabelName).toBe('Positive');
    expect(result.current.draftLabelColor).toBe('#ff0000');
  });

  it('does nothing when editing an out-of-range index', () => {
    const params = baseParams();
    const { result } = renderHook(() => useProjectSetupLabelEditor(params));

    act(() => result.current.openEditLabelDialog(5));

    expect(result.current.isLabelEditorOpen).toBe(false);
  });

  it('resetLabelEditor clears the draft and closes the dialog', () => {
    const params = baseParams();
    const { result } = renderHook(() => useProjectSetupLabelEditor(params));
    act(() => result.current.openCreateLabelDialog());
    act(() => result.current.setDraftLabelName('draft'));

    act(() => result.current.resetLabelEditor());

    expect(result.current.isLabelEditorOpen).toBe(false);
    expect(result.current.draftLabelName).toBe('');
  });
});

describe('isLabelDialogSaveDisabled', () => {
  it('is disabled with a blank name', () => {
    const params = baseParams();
    const { result } = renderHook(() => useProjectSetupLabelEditor(params));

    expect(result.current.isLabelDialogSaveDisabled).toBe(true);
  });

  it('is enabled with a name for non-NER projects', () => {
    const params = baseParams();
    const { result } = renderHook(() => useProjectSetupLabelEditor(params));
    act(() => result.current.setDraftLabelName('Positive'));

    expect(result.current.isLabelDialogSaveDisabled).toBe(false);
  });

  it('requires a color for NER projects', () => {
    const params = baseParams({ isNerProjectType: true });
    const { result } = renderHook(() => useProjectSetupLabelEditor(params));
    act(() => result.current.setDraftLabelName('Person'));
    act(() => result.current.setDraftLabelColor(''));

    expect(result.current.isLabelDialogSaveDisabled).toBe(true);
  });
});

describe('saveLabelFromDialog', () => {
  it('adds a new label and clears the draft name', () => {
    const params = baseParams();
    const { result } = renderHook(() => useProjectSetupLabelEditor(params));
    act(() => result.current.openCreateLabelDialog());
    act(() => result.current.setDraftLabelName('Positive'));

    act(() => result.current.saveLabelFromDialog());

    expect(params.setLabelsValue).toHaveBeenCalledWith(expect.any(Function));
    const updater = vi.mocked(params.setLabelsValue).mock.calls[0][0] as (
      l: ProjectSetupLabel[],
    ) => ProjectSetupLabel[];
    expect(updater([])).toEqual([{ color: null, name: 'Positive' }]);
    expect(result.current.isLabelEditorOpen).toBe(false);
  });

  it('does nothing when the draft name is blank', () => {
    const params = baseParams();
    const { result } = renderHook(() => useProjectSetupLabelEditor(params));

    act(() => result.current.saveLabelFromDialog());

    expect(params.setLabelsValue).not.toHaveBeenCalled();
  });

  it('reports a duplicate name instead of saving', () => {
    const params = baseParams({ labels: [{ name: 'Positive', color: null }] });
    const { result } = renderHook(() => useProjectSetupLabelEditor(params));
    act(() => result.current.openCreateLabelDialog());
    act(() => result.current.setDraftLabelName('positive'));

    act(() => result.current.saveLabelFromDialog());

    expect(params.onDuplicateLabel).toHaveBeenCalled();
    expect(params.setLabelsValue).not.toHaveBeenCalled();
  });

  it('rejects a name outside the available csv headers in csv label mode', () => {
    const params = baseParams({ isCsvLabelModeEnabled: true });
    const { result } = renderHook(() => useProjectSetupLabelEditor(params));
    act(() => result.current.openCreateLabelDialog());
    act(() => result.current.setDraftLabelName('not-a-header'));

    act(() => result.current.saveLabelFromDialog());

    expect(params.setLabelsValue).not.toHaveBeenCalled();
  });

  it('replaces the label at the editing index when saving an edit', () => {
    const params = baseParams({ labels: [{ name: 'Positive', color: null }, { name: 'Negative', color: null }] });
    const { result } = renderHook(() => useProjectSetupLabelEditor(params));
    act(() => result.current.openEditLabelDialog(0));
    act(() => result.current.setDraftLabelName('Great'));

    act(() => result.current.saveLabelFromDialog());

    const updater = vi.mocked(params.setLabelsValue).mock.calls[0][0] as (
      l: ProjectSetupLabel[],
    ) => ProjectSetupLabel[];
    expect(updater(params.labels)).toEqual([
      { color: null, name: 'Great' },
      { name: 'Negative', color: null },
    ]);
  });
});

describe('removeLabelAt', () => {
  it('removes the label at the given index', () => {
    const params = baseParams({ labels: [{ name: 'A', color: null }, { name: 'B', color: null }] });
    const { result } = renderHook(() => useProjectSetupLabelEditor(params));

    act(() => result.current.removeLabelAt(0));

    const updater = vi.mocked(params.setLabelsValue).mock.calls[0][0] as (
      l: ProjectSetupLabel[],
    ) => ProjectSetupLabel[];
    expect(updater(params.labels)).toEqual([{ name: 'B', color: null }]);
  });
});

describe('availableCsvLabelOptions', () => {
  it('excludes the target column and already-used labels', () => {
    const params = baseParams({ labels: [{ name: 'label', color: null }] });
    const { result } = renderHook(() => useProjectSetupLabelEditor(params));

    expect(result.current.availableCsvLabelOptions).toEqual([
      { label: 'sentiment', value: 'sentiment' },
    ]);
  });

  it('exposes csvLabelNameOptions only when csv label mode is enabled', () => {
    const enabled = renderHook(() => useProjectSetupLabelEditor(baseParams({ isCsvLabelModeEnabled: true })));
    expect(enabled.result.current.csvLabelNameOptions).toBeDefined();

    const disabled = renderHook(() => useProjectSetupLabelEditor(baseParams({ isCsvLabelModeEnabled: false })));
    expect(disabled.result.current.csvLabelNameOptions).toBeUndefined();
  });
});
