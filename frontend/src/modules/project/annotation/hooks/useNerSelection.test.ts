import { act, renderHook } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { toast } from 'sonner';
import { useNerSelection } from '@/modules/project/annotation/hooks/useNerSelection';
import {
  clearTextSelection,
  selectionOffsetsWithinElement,
} from '@/modules/project/annotation/utils/annotationPageUtils';
import { type AnnotationDraft, type NerAnnotationEntity } from '@/modules/project/annotation/utils/annotationPageUtils';
import { type AnnotationStep, type ProjectSetupLabel } from '@/modules/project/shared/types/project';

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

vi.mock('sonner', () => ({
  toast: { error: vi.fn() },
}));

vi.mock('@/modules/project/annotation/utils/annotationPageUtils', async (importOriginal) => {
  const actual =
    await importOriginal<typeof import('@/modules/project/annotation/utils/annotationPageUtils')>();
  return {
    ...actual,
    clearTextSelection: vi.fn(),
    selectionOffsetsWithinElement: vi.fn(),
  };
});

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

const labels: ProjectSetupLabel[] = [{ name: 'PERSON', color: '#111' }];

function draft(entities: NerAnnotationEntity[] = []): AnnotationDraft {
  return { value: '', notes: '', entities };
}

function setup(overrides: Partial<Parameters<typeof useNerSelection>[0]> = {}) {
  const updateDraft = vi.fn();
  const setActiveNerLabel = vi.fn();
  const currentStep = step();
  const getDraft = vi.fn(() => draft());

  const hook = renderHook(() =>
    useNerSelection({
      activeNerLabel: '',
      annotationProjectType: 'NER',
      annotationTargetColumn: null,
      currentDraft: draft(),
      currentStep,
      getDraft,
      isReviewMode: false,
      labels,
      setActiveNerLabel,
      sourceTextContent: null,
      updateDraft,
      ...overrides,
    }),
  );

  return { ...hook, updateDraft, setActiveNerLabel, getDraft, currentStep };
}

beforeEach(() => {
  vi.mocked(toast.error).mockClear();
  vi.mocked(clearTextSelection).mockClear();
  vi.mocked(selectionOffsetsWithinElement).mockReset();
});

describe('useNerSelection active label sync', () => {
  it('clears the active label when the project type is not NER', () => {
    const { setActiveNerLabel } = setup({
      activeNerLabel: 'PERSON',
      annotationProjectType: 'TEXT_CLASSIFICATION_SIMPLE',
    });

    expect(setActiveNerLabel).toHaveBeenCalledWith('');
  });

  it('clears the active label when it no longer exists among the labels', () => {
    const { setActiveNerLabel } = setup({ activeNerLabel: 'GHOST' });

    expect(setActiveNerLabel).toHaveBeenCalledWith('');
  });

  it('keeps a valid active label untouched', () => {
    const { setActiveNerLabel } = setup({ activeNerLabel: 'PERSON' });

    expect(setActiveNerLabel).not.toHaveBeenCalled();
  });
});

describe('useNerSelection mouse selection handling', () => {
  function fireMouseUp(refElement: HTMLElement) {
    act(() => {
      refElement.dispatchEvent(new MouseEvent('mouseup', { bubbles: true }));
    });
  }

  it('does nothing when there is no text selection', () => {
    vi.mocked(selectionOffsetsWithinElement).mockReturnValue(null);
    const { result, updateDraft } = setup({ activeNerLabel: 'PERSON' });
    const element = document.createElement('div');
    document.body.append(element);
    result.current.nerSourceSelectionRef.current = element;

    fireMouseUp(element);

    expect(updateDraft).not.toHaveBeenCalled();
    element.remove();
  });

  it('shows an error and clears the selection when no label is active', () => {
    vi.mocked(selectionOffsetsWithinElement).mockReturnValue({
      selectedText: 'Ana',
      startOffset: 0,
      endOffset: 3,
    });
    const { result, updateDraft } = setup({ activeNerLabel: '' });
    const element = document.createElement('div');
    document.body.append(element);
    result.current.nerSourceSelectionRef.current = element;

    fireMouseUp(element);

    expect(toast.error).toHaveBeenCalledWith('project.annotationPage.errors.nerLabelRequired');
    expect(clearTextSelection).toHaveBeenCalled();
    expect(updateDraft).not.toHaveBeenCalled();
    element.remove();
  });

  it('merges the new entity into the draft when a label is active', () => {
    vi.mocked(selectionOffsetsWithinElement).mockReturnValue({
      selectedText: 'Ana',
      startOffset: 0,
      endOffset: 3,
    });
    const { result, updateDraft, currentStep } = setup({ activeNerLabel: 'PERSON' });
    const element = document.createElement('div');
    document.body.append(element);
    result.current.nerSourceSelectionRef.current = element;

    fireMouseUp(element);

    expect(updateDraft).toHaveBeenCalledWith(currentStep, {
      entities: [{ label: 'PERSON', text: 'Ana', startOffset: 0, endOffset: 3 }],
    });
    expect(clearTextSelection).toHaveBeenCalled();
    element.remove();
  });

  it('does not react to selection while in review mode', () => {
    vi.mocked(selectionOffsetsWithinElement).mockReturnValue({
      selectedText: 'Ana',
      startOffset: 0,
      endOffset: 3,
    });
    const { result, updateDraft } = setup({ activeNerLabel: 'PERSON', isReviewMode: true });
    const element = document.createElement('div');
    document.body.append(element);
    result.current.nerSourceSelectionRef.current = element;

    fireMouseUp(element);

    expect(updateDraft).not.toHaveBeenCalled();
    element.remove();
  });
});

describe('useNerSelection derived data', () => {
  it('builds the ner source text and segments only for NER projects', () => {
    const { result } = setup({
      annotationProjectType: 'NER',
      sourceTextContent: 'Ana went to Vigo',
      currentDraft: draft([{ label: 'PERSON', text: 'Ana', startOffset: 0, endOffset: 3 }]),
    });

    expect(result.current.nerSourceText).toBe('Ana went to Vigo');
    expect(result.current.nerTextSegments.length).toBeGreaterThan(1);
  });

  it('returns empty source text and segments for non-NER project types', () => {
    const { result } = setup({
      annotationProjectType: 'TEXT_CLASSIFICATION_SIMPLE',
      sourceTextContent: 'Ana went to Vigo',
    });

    expect(result.current.nerSourceText).toBe('');
    expect(result.current.nerTextSegments).toEqual([]);
  });

  it('maps every label to its color', () => {
    const { result } = setup();

    expect(result.current.nerLabelColorMap.get('PERSON')).toBe('#111');
  });
});

describe('removeNerEntity', () => {
  it('removes the matching entity from the step draft', () => {
    const entity: NerAnnotationEntity = { label: 'PERSON', text: 'Ana', startOffset: 0, endOffset: 3 };
    const other: NerAnnotationEntity = { label: 'LOC', text: 'Vigo', startOffset: 10, endOffset: 14 };
    const { result, updateDraft, getDraft, currentStep } = setup();
    getDraft.mockReturnValue(draft([entity, other]));

    result.current.removeNerEntity(currentStep, entity);

    expect(updateDraft).toHaveBeenCalledWith(currentStep, { entities: [other] });
  });

  it('does nothing when in review mode or there is no step', () => {
    const entity: NerAnnotationEntity = { label: 'PERSON', text: 'Ana', startOffset: 0, endOffset: 3 };
    const { result, updateDraft } = setup({ isReviewMode: true });

    result.current.removeNerEntity(null, entity);
    expect(updateDraft).not.toHaveBeenCalled();
  });
});
