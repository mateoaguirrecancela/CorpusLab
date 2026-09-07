import { afterEach, describe, expect, it } from 'vitest';
import {
  annotationStepKey,
  annotationValueToEditorValue,
  annotationValueToNerEntities,
  annotationValueToNotes,
  buildAnnotationPayload,
  buildNerTextSegments,
  clearTextSelection,
  findCsvColumnValue,
  formatTextSourceContent,
  getLabelStyle,
  getNerEntityStyle,
  getNerSourceText,
  isCsvMimeType,
  isInlineSourceMimeType,
  isJsonSourceMimeType,
  isTextSourceMimeType,
  mergeNerEntity,
  normalizeNerEntities,
  normalizeStepIndex,
  parseCommaSeparatedLabels,
  selectionOffsetsWithinElement,
  type NerAnnotationEntity,
} from '@/modules/project/annotation/utils/annotationPageUtils';
import { type AnnotationStep } from '@/modules/project/shared/types/project';

function entity(overrides: Partial<NerAnnotationEntity> = {}): NerAnnotationEntity {
  return { label: 'PERSON', text: 'Ana', startOffset: 0, endOffset: 3, ...overrides };
}

function step(overrides: Partial<AnnotationStep> = {}): AnnotationStep {
  return {
    datasetItemId: 1,
    datasetItemIndex: 0,
    stepIndex: 2,
    totalStepsForItem: 3,
    sourceName: 'file.txt',
    sourceMimeType: 'text/plain',
    preview: 'preview text',
    rowValues: null,
    completed: false,
    warning: false,
    annotation: null,
    ...overrides,
  };
}

describe('annotationStepKey', () => {
  it('combines dataset item id and step index', () => {
    expect(annotationStepKey(step({ datasetItemId: 7, stepIndex: 2 }))).toBe('7:2');
  });
});

describe('normalizeStepIndex', () => {
  it('clamps to 0 when there are no steps', () => {
    expect(normalizeStepIndex(5, 0)).toBe(0);
  });

  it('clamps below 1 up to 1', () => {
    expect(normalizeStepIndex(-3, 10)).toBe(1);
  });

  it('clamps above the total down to the total', () => {
    expect(normalizeStepIndex(20, 10)).toBe(10);
  });

  it('keeps an in-range index untouched', () => {
    expect(normalizeStepIndex(5, 10)).toBe(5);
  });
});

describe('normalizeNerEntities', () => {
  it('drops entities with invalid offsets or a mismatched text length', () => {
    const entities = normalizeNerEntities([
      entity({ startOffset: -1 }),
      entity({ startOffset: 5, endOffset: 5 }),
      entity({ text: 'AnaX', startOffset: 0, endOffset: 3 }),
    ]);

    expect(entities).toEqual([]);
  });

  it('deduplicates entities with the same offsets and label (case-insensitive)', () => {
    const entities = normalizeNerEntities([
      entity({ label: 'PERSON' }),
      entity({ label: 'person' }),
    ]);

    expect(entities).toHaveLength(1);
  });

  it('sorts by start offset, then end offset, then label', () => {
    const entities = normalizeNerEntities([
      entity({ label: 'LOC', text: 'Vigo', startOffset: 10, endOffset: 14 }),
      entity({ label: 'PERSON', text: 'Ana', startOffset: 0, endOffset: 3 }),
    ]);

    expect(entities.map((e) => e.label)).toEqual(['PERSON', 'LOC']);
  });

  it('breaks a tie on start offset by end offset', () => {
    const entities = normalizeNerEntities([
      entity({ label: 'LOC', text: 'Vigo city', startOffset: 0, endOffset: 9 }),
      entity({ label: 'PERSON', text: 'Vigo', startOffset: 0, endOffset: 4 }),
    ]);

    expect(entities.map((e) => e.endOffset)).toEqual([4, 9]);
  });

  it('breaks a tie on identical offsets by label', () => {
    const entities = normalizeNerEntities([
      entity({ label: 'LOC', text: 'Ana', startOffset: 0, endOffset: 3 }),
      entity({ label: 'PERSON', text: 'Ana', startOffset: 0, endOffset: 3 }),
    ]);

    expect(entities.map((e) => e.label)).toEqual(['LOC', 'PERSON']);
  });
});

describe('annotationValueToNerEntities', () => {
  it('returns an empty array for non-object annotations', () => {
    expect(annotationValueToNerEntities(null)).toEqual([]);
    expect(annotationValueToNerEntities('text')).toEqual([]);
    expect(annotationValueToNerEntities([])).toEqual([]);
  });

  it('returns an empty array when entities is missing or not an array', () => {
    expect(annotationValueToNerEntities({})).toEqual([]);
  });

  it('parses valid entities and drops malformed ones', () => {
    const result = annotationValueToNerEntities({
      entities: [
        { label: 'PERSON', text: 'Ana', startOffset: 0, endOffset: 3 },
        { label: '', text: 'Ana', startOffset: 0, endOffset: 3 },
        { label: 'PERSON', text: 'Ana', startOffset: '0', endOffset: '3' },
      ],
    });

    expect(result).toEqual([{ label: 'PERSON', text: 'Ana', startOffset: 0, endOffset: 3 }]);
  });

  it('drops entities whose offsets are blank, non-numeric, or a wrong type', () => {
    const result = annotationValueToNerEntities({
      entities: [
        { label: 'A', text: 'x', startOffset: '  ', endOffset: 3 },
        { label: 'B', text: 'x', startOffset: 'abc', endOffset: 3 },
        { label: 'C', text: 'x', startOffset: true, endOffset: 3 },
      ],
    });

    expect(result).toEqual([]);
  });
});

describe('annotationValueToEditorValue', () => {
  it('returns empty string for null annotation', () => {
    expect(annotationValueToEditorValue(null, 'TEXT_CLASSIFICATION_SIMPLE')).toBe('');
  });

  it('returns a string annotation unchanged', () => {
    expect(annotationValueToEditorValue('label', 'TEXT_CLASSIFICATION_SIMPLE')).toBe('label');
  });

  it('joins array annotations with commas', () => {
    expect(annotationValueToEditorValue(['a', 'b'], 'TEXT_CLASSIFICATION_MULTILABEL')).toBe('a, b');
  });

  it('reads text for SEQ2SEQ objects', () => {
    expect(annotationValueToEditorValue({ text: 'generated' }, 'SEQ2SEQ')).toBe('generated');
  });

  it('is always empty for NER regardless of shape', () => {
    expect(annotationValueToEditorValue({ entities: [] }, 'NER')).toBe('');
  });

  it('reads label for simple classification objects', () => {
    expect(annotationValueToEditorValue({ label: 'positive' }, 'TEXT_CLASSIFICATION_SIMPLE')).toBe(
      'positive',
    );
  });

  it('reads labels array for multilabel objects', () => {
    expect(
      annotationValueToEditorValue({ labels: ['a', 'b'] }, 'TEXT_CLASSIFICATION_MULTILABEL'),
    ).toBe('a, b');
  });

  it('falls back to empty string for unrecognized shapes', () => {
    expect(annotationValueToEditorValue({ foo: 'bar' }, 'TEXT_CLASSIFICATION_SIMPLE')).toBe('');
  });
});

describe('annotationValueToNotes', () => {
  it('reads notes from an object annotation', () => {
    expect(annotationValueToNotes({ notes: 'hello' })).toBe('hello');
  });

  it('returns empty string when notes is missing or annotation is not an object', () => {
    expect(annotationValueToNotes({})).toBe('');
    expect(annotationValueToNotes(null)).toBe('');
    expect(annotationValueToNotes('text')).toBe('');
  });
});

describe('parseCommaSeparatedLabels', () => {
  it('trims, dedupes and drops empty segments', () => {
    expect(parseCommaSeparatedLabels(' a, b ,a,, c')).toEqual(['a', 'b', 'c']);
  });
});

describe('buildAnnotationPayload', () => {
  it('returns null for NER with no valid entities', () => {
    expect(buildAnnotationPayload('NER', '', '', [])).toBeNull();
  });

  it('builds a NER payload with entities and optional notes', () => {
    const payload = buildAnnotationPayload('NER', '', 'note', [entity()]);
    expect(payload).toEqual({
      entities: [{ label: 'PERSON', text: 'Ana', startOffset: 0, endOffset: 3 }],
      notes: 'note',
    });
  });

  it('returns null for a blank value on non-NER types', () => {
    expect(buildAnnotationPayload('TEXT_CLASSIFICATION_SIMPLE', '   ', '')).toBeNull();
  });

  it('builds a SEQ2SEQ payload from the trimmed text', () => {
    expect(buildAnnotationPayload('SEQ2SEQ', ' generated ', '')).toEqual({ text: 'generated' });
  });

  it('includes notes in a SEQ2SEQ payload when present', () => {
    expect(buildAnnotationPayload('SEQ2SEQ', 'generated', 'note')).toEqual({
      text: 'generated',
      notes: 'note',
    });
  });

  it('includes notes in a simple classification payload when present', () => {
    expect(buildAnnotationPayload('TEXT_CLASSIFICATION_SIMPLE', 'positive', 'note')).toEqual({
      label: 'positive',
      notes: 'note',
    });
  });

  it('builds a multilabel payload from comma separated labels', () => {
    expect(buildAnnotationPayload('TEXT_CLASSIFICATION_MULTILABEL', 'a, b', '')).toEqual({
      labels: ['a', 'b'],
    });
  });

  it('returns null for multilabel when no labels parse out', () => {
    expect(buildAnnotationPayload('TEXT_CLASSIFICATION_MULTILABEL', ' , ,', '')).toBeNull();
  });

  it('includes notes in a multilabel payload when present', () => {
    expect(buildAnnotationPayload('TEXT_CLASSIFICATION_MULTILABEL', 'a, b', 'note')).toEqual({
      labels: ['a', 'b'],
      notes: 'note',
    });
  });

  it('builds a simple classification payload with a label', () => {
    expect(buildAnnotationPayload('TEXT_CLASSIFICATION_SIMPLE', 'positive', '')).toEqual({
      label: 'positive',
    });
  });
});

describe('getNerSourceText', () => {
  it('returns empty string when there is no step', () => {
    expect(getNerSourceText(null, null, null)).toBe('');
  });

  it('prefers the cached source text content', () => {
    expect(getNerSourceText(step(), 'cached text', null)).toBe('cached text');
  });

  it('falls back to a csv column value when no cached text is available', () => {
    expect(getNerSourceText(step({ rowValues: { Text: 'row value' } }), null, 'Text')).toBe(
      'row value',
    );
  });

  it('falls back to the step preview when nothing else is available', () => {
    expect(getNerSourceText(step({ preview: 'preview text' }), null, null)).toBe('preview text');
  });
});

describe('mergeNerEntity', () => {
  it('replaces an exact duplicate (same offsets and label)', () => {
    const result = mergeNerEntity([entity({ text: 'Ana' })], entity({ text: 'Bob', endOffset: 3 }));
    expect(result).toHaveLength(1);
    expect(result[0].text).toBe('Bob');
  });

  it('adds a non-duplicate entity alongside existing ones', () => {
    const result = mergeNerEntity(
      [entity()],
      entity({ label: 'LOC', text: 'Vigo', startOffset: 10, endOffset: 14 }),
    );
    expect(result).toHaveLength(2);
  });
});

describe('buildNerTextSegments', () => {
  it('returns a single full-text segment when there are no entities', () => {
    const segments = buildNerTextSegments('Hello world', []);
    expect(segments).toEqual([{ key: 'text-full', text: 'Hello world', entities: [] }]);
  });

  it('splits text into segments at entity boundaries', () => {
    const segments = buildNerTextSegments('Ana went to Vigo', [
      entity({ label: 'PERSON', text: 'Ana', startOffset: 0, endOffset: 3 }),
      entity({ label: 'LOC', text: 'Vigo', startOffset: 12, endOffset: 16 }),
    ]);

    expect(segments.map((s) => s.text)).toEqual(['Ana', ' went to ', 'Vigo']);
    expect(segments[0].entities).toHaveLength(1);
    expect(segments[1].entities).toHaveLength(0);
  });

  it('ignores entities that extend past the end of the text', () => {
    const segments = buildNerTextSegments('short', [
      entity({ startOffset: 0, endOffset: 999, text: 'x'.repeat(999) }),
    ]);

    expect(segments).toEqual([{ key: 'text-full', text: 'short', entities: [] }]);
  });
});

describe('getNerEntityStyle', () => {
  it('returns undefined when there is no color for the label', () => {
    expect(getNerEntityStyle('PERSON', new Map())).toBeUndefined();
    expect(getNerEntityStyle('PERSON', new Map([['PERSON', null]]))).toBeUndefined();
  });

  it('builds a translucent background and solid border color', () => {
    expect(getNerEntityStyle('PERSON', new Map([['PERSON', '#ff0000']]))).toEqual({
      backgroundColor: '#ff000030',
      borderBottom: '2px solid #ff0000',
    });
  });
});

describe('mime type helpers', () => {
  it('isCsvMimeType detects csv regardless of case', () => {
    expect(isCsvMimeType('text/CSV')).toBe(true);
    expect(isCsvMimeType('text/plain')).toBe(false);
  });

  it('isInlineSourceMimeType detects images and pdf', () => {
    expect(isInlineSourceMimeType('image/png')).toBe(true);
    expect(isInlineSourceMimeType('application/pdf')).toBe(true);
    expect(isInlineSourceMimeType('text/plain')).toBe(false);
  });

  it('isJsonSourceMimeType detects json mime types and .json extension', () => {
    expect(isJsonSourceMimeType('application/json', null)).toBe(true);
    expect(isJsonSourceMimeType('application/vnd.api+json', null)).toBe(true);
    expect(isJsonSourceMimeType('text/plain', 'data.json')).toBe(true);
    expect(isJsonSourceMimeType('text/plain', 'data.txt')).toBe(false);
  });

  it('isTextSourceMimeType covers text/plain, json and .txt', () => {
    expect(isTextSourceMimeType('text/plain', null)).toBe(true);
    expect(isTextSourceMimeType('application/json', null)).toBe(true);
    expect(isTextSourceMimeType('application/octet-stream', 'notes.txt')).toBe(true);
    expect(isTextSourceMimeType('application/octet-stream', 'image.png')).toBe(false);
  });
});

describe('formatTextSourceContent', () => {
  it('pretty-prints valid JSON content', () => {
    const formatted = formatTextSourceContent('application/json', null, '{"a":1}');
    expect(formatted).toBe('{\n  "a": 1\n}');
  });

  it('returns the raw content when JSON parsing fails', () => {
    expect(formatTextSourceContent('application/json', null, 'not json')).toBe('not json');
  });

  it('leaves non-json content untouched', () => {
    expect(formatTextSourceContent('text/plain', null, 'hello')).toBe('hello');
  });
});

describe('findCsvColumnValue', () => {
  it('finds a column case-insensitively', () => {
    expect(findCsvColumnValue('Text', { text: 'value' })).toBe('value');
  });

  it('returns null when rowValues is null or the column is blank', () => {
    expect(findCsvColumnValue('Text', null)).toBeNull();
    expect(findCsvColumnValue('  ', { text: 'value' })).toBeNull();
  });

  it('returns null when the column is not present', () => {
    expect(findCsvColumnValue('missing', { text: 'value' })).toBeNull();
  });

  it('returns an empty string rather than a falsy raw value for an empty cell', () => {
    expect(findCsvColumnValue('text', { text: '' })).toBe('');
  });
});

describe('getLabelStyle', () => {
  it('returns undefined when the label has no color', () => {
    expect(getLabelStyle({ name: 'a', color: null }, true)).toBeUndefined();
  });

  it('builds a filled style when selected', () => {
    expect(getLabelStyle({ name: 'a', color: '#00ff00' }, true)).toEqual({
      borderColor: '#00ff00',
      backgroundColor: '#00ff001A',
      color: '#00ff00',
    });
  });

  it('builds a subtle border style when not selected', () => {
    expect(getLabelStyle({ name: 'a', color: '#00ff00' }, false)).toEqual({
      borderColor: '#00ff0066',
      color: '#00ff00',
    });
  });
});

function selectTextInElement(element: HTMLElement, start: number, end: number): void {
  const textNode = element.firstChild;
  if (!textNode) {
    throw new Error('expected the element to contain a text node');
  }

  const range = document.createRange();
  range.setStart(textNode, start);
  range.setEnd(textNode, end);

  const selection = window.getSelection();
  selection?.removeAllRanges();
  selection?.addRange(range);
}

describe('selectionOffsetsWithinElement', () => {
  afterEach(() => {
    window.getSelection()?.removeAllRanges();
    document.body.replaceChildren();
  });

  it('returns null when nothing is selected', () => {
    const container = document.createElement('div');
    container.textContent = 'Hello world';
    document.body.append(container);

    expect(selectionOffsetsWithinElement(container)).toBeNull();
  });

  it('returns null when the selection lies outside the given element', () => {
    const container = document.createElement('div');
    container.textContent = 'Hello world';
    const other = document.createElement('div');
    other.textContent = 'Other text';
    document.body.append(container, other);

    selectTextInElement(other, 0, 5);

    expect(selectionOffsetsWithinElement(container)).toBeNull();
  });

  it('returns null when the selection is only whitespace', () => {
    const container = document.createElement('div');
    container.textContent = 'Hello   world';
    document.body.append(container);

    selectTextInElement(container, 5, 8);

    expect(selectionOffsetsWithinElement(container)).toBeNull();
  });

  it('returns the selected text and its offsets within the element', () => {
    const container = document.createElement('div');
    container.textContent = 'Ana went to Vigo';
    document.body.append(container);

    selectTextInElement(container, 12, 16);

    expect(selectionOffsetsWithinElement(container)).toEqual({
      selectedText: 'Vigo',
      startOffset: 12,
      endOffset: 16,
    });
  });

  it('trims leading/trailing whitespace and punctuation, adjusting the offsets', () => {
    const container = document.createElement('div');
    container.textContent = 'Say "hello world" now';
    document.body.append(container);

    // Selects `"hello world"` (including the surrounding quotes).
    selectTextInElement(container, 4, 17);

    expect(selectionOffsetsWithinElement(container)).toEqual({
      selectedText: 'hello world',
      startOffset: 5,
      endOffset: 16,
    });
  });

  it('returns null when the selection is only punctuation with nothing left after trimming', () => {
    const container = document.createElement('div');
    container.textContent = 'Say "..." now';
    document.body.append(container);

    selectTextInElement(container, 4, 9);

    expect(selectionOffsetsWithinElement(container)).toBeNull();
  });
});

describe('clearTextSelection', () => {
  it('removes every range from the current selection', () => {
    const container = document.createElement('div');
    container.textContent = 'Hello world';
    document.body.append(container);
    selectTextInElement(container, 0, 5);
    expect(window.getSelection()?.rangeCount).toBe(1);

    clearTextSelection();

    expect(window.getSelection()?.rangeCount).toBe(0);
    document.body.replaceChildren();
  });
});
