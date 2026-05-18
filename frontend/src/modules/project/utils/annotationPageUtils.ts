import { type CSSProperties } from 'react';
import {
  type AnnotationStep,
  type ProjectSetupLabel,
  type ProjectType,
} from '@/modules/project/types/project';
import { projectTypeI18nKey } from '@/modules/project/utils/projectDisplayUtils';
import { getFileExtension } from '@/modules/project/utils/projectFileUtils';

export const ANNOTATION_PAGE_SIZE = 50;

export type NerAnnotationEntity = {
  label: string;
  text: string;
  startOffset: number;
  endOffset: number;
};

export type AnnotationDraft = {
  value: string;
  notes: string;
  entities: NerAnnotationEntity[];
};

export type AnnotationDraftByStep = Record<string, AnnotationDraft>;

export type PendingPageSelection = 'none' | 'first' | 'last';

export type SourceCacheEntry = {
  url: string | null;
  textContent: string | null;
  mimeType: string;
  fileName: string | null;
};

export type PersistCurrentStepResult = 'saved' | 'skipped' | 'error';

export type NerTextSegment = {
  key: string;
  text: string;
  entities: NerAnnotationEntity[];
};

export type CsvLabelColumnValue = {
  name: string;
  value: string;
};

export { projectTypeI18nKey };

export function annotationStepKey(step: AnnotationStep): string {
  return `${step.datasetItemId}:${step.stepIndex}`;
}

export function normalizeStepIndex(stepIndex: number, totalSteps: number): number {
  if (totalSteps <= 0) {
    return 0;
  }

  return Math.min(Math.max(stepIndex, 1), totalSteps);
}

function normalizeStringArray(value: unknown): string[] {
  if (!Array.isArray(value)) {
    return [];
  }

  return value
    .filter((entry): entry is string => typeof entry === 'string')
    .map((entry) => entry.trim())
    .filter((entry) => entry.length > 0);
}

function parseInteger(value: unknown): number | null {
  if (typeof value === 'number' && Number.isInteger(value)) {
    return value;
  }

  if (typeof value === 'string') {
    const normalizedValue = value.trim();
    if (normalizedValue.length === 0) {
      return null;
    }

    const parsedValue = Number.parseInt(normalizedValue, 10);
    return Number.isInteger(parsedValue) ? parsedValue : null;
  }

  return null;
}

export function normalizeNerEntities(entities: NerAnnotationEntity[]): NerAnnotationEntity[] {
  const uniqueEntities = new Map<string, NerAnnotationEntity>();

  entities.forEach((entity) => {
    if (
      entity.startOffset < 0 ||
      entity.endOffset <= entity.startOffset ||
      entity.text.length !== entity.endOffset - entity.startOffset
    ) {
      return;
    }

    const key = `${entity.startOffset}:${entity.endOffset}:${entity.label.toLowerCase()}`;
    if (!uniqueEntities.has(key)) {
      uniqueEntities.set(key, entity);
    }
  });

  return Array.from(uniqueEntities.values()).sort((left, right) => {
    if (left.startOffset !== right.startOffset) {
      return left.startOffset - right.startOffset;
    }

    if (left.endOffset !== right.endOffset) {
      return left.endOffset - right.endOffset;
    }

    return left.label.localeCompare(right.label);
  });
}

export function annotationValueToNerEntities(annotation: unknown): NerAnnotationEntity[] {
  if (annotation == null || typeof annotation !== 'object' || Array.isArray(annotation)) {
    return [];
  }

  const value = annotation as Record<string, unknown>;
  if (!Array.isArray(value.entities)) {
    return [];
  }

  const entities = value.entities
    .map((entity) => {
      if (entity == null || typeof entity !== 'object' || Array.isArray(entity)) {
        return null;
      }

      const candidate = entity as Record<string, unknown>;
      const label = typeof candidate.label === 'string' ? candidate.label.trim() : '';
      const text = typeof candidate.text === 'string' ? candidate.text : '';
      const startOffset = parseInteger(candidate.startOffset);
      const endOffset = parseInteger(candidate.endOffset);

      if (label.length === 0 || text.length === 0 || startOffset == null || endOffset == null) {
        return null;
      }

      return {
        label,
        text,
        startOffset,
        endOffset,
      } satisfies NerAnnotationEntity;
    })
    .filter((entity): entity is NerAnnotationEntity => entity !== null);

  return normalizeNerEntities(entities);
}

export function annotationValueToEditorValue(
  annotation: unknown,
  projectType: ProjectType,
): string {
  if (annotation == null) {
    return '';
  }

  if (typeof annotation === 'string') {
    return annotation;
  }

  if (Array.isArray(annotation)) {
    return normalizeStringArray(annotation).join(', ');
  }

  if (typeof annotation === 'object') {
    const value = annotation as Record<string, unknown>;

    if (projectType === 'SEQ2SEQ' && typeof value.text === 'string') {
      return value.text;
    }

    if (projectType === 'NER') {
      return '';
    }

    if (typeof value.label === 'string') {
      return value.label;
    }

    if (Array.isArray(value.labels)) {
      return normalizeStringArray(value.labels).join(', ');
    }
  }

  return '';
}

export function annotationValueToNotes(annotation: unknown): string {
  if (annotation == null || typeof annotation !== 'object' || Array.isArray(annotation)) {
    return '';
  }

  const value = annotation as Record<string, unknown>;
  return typeof value.notes === 'string' ? value.notes : '';
}

export function parseCommaSeparatedLabels(rawValue: string): string[] {
  return Array.from(
    new Set(
      rawValue
        .split(',')
        .map((segment) => segment.trim())
        .filter((segment) => segment.length > 0),
    ),
  );
}

export function buildAnnotationPayload(
  projectType: ProjectType,
  rawValue: string,
  rawNotes: string,
  nerEntities: NerAnnotationEntity[] = [],
): Record<string, unknown> | null {
  const notes = rawNotes.trim();

  if (projectType === 'NER') {
    const entities = normalizeNerEntities(nerEntities);
    if (entities.length === 0) {
      return null;
    }

    const payload: Record<string, unknown> = {
      entities: entities.map((entity) => ({
        label: entity.label,
        text: entity.text,
        startOffset: entity.startOffset,
        endOffset: entity.endOffset,
      })),
    };

    if (notes.length > 0) {
      payload.notes = notes;
    }

    return payload;
  }

  const trimmedValue = rawValue.trim();
  if (trimmedValue.length === 0) {
    return null;
  }

  if (projectType === 'SEQ2SEQ') {
    const payload: Record<string, unknown> = { text: trimmedValue };
    if (notes.length > 0) {
      payload.notes = notes;
    }

    return payload;
  }

  if (projectType === 'TEXT_CLASSIFICATION_MULTILABEL') {
    const labels = parseCommaSeparatedLabels(trimmedValue);
    if (labels.length === 0) {
      return null;
    }

    const payload: Record<string, unknown> = { labels };
    if (notes.length > 0) {
      payload.notes = notes;
    }

    return payload;
  }

  const payload: Record<string, unknown> = { label: trimmedValue };
  if (notes.length > 0) {
    payload.notes = notes;
  }

  return payload;
}

export function getNerSourceText(
  step: AnnotationStep | null,
  sourceTextContent: string | null,
  annotationTargetColumn: string | null,
): string {
  if (!step) {
    return '';
  }

  if (sourceTextContent != null) {
    return sourceTextContent;
  }

  if (annotationTargetColumn) {
    const columnValue = findCsvColumnValue(annotationTargetColumn, step.rowValues);
    if (columnValue != null) {
      return columnValue;
    }
  }

  return step.preview;
}

export function mergeNerEntity(
  existingEntities: NerAnnotationEntity[],
  nextEntity: NerAnnotationEntity,
): NerAnnotationEntity[] {
  const entitiesWithoutExactDuplicate = existingEntities.filter(
    (entity) =>
      !(
        entity.startOffset === nextEntity.startOffset &&
        entity.endOffset === nextEntity.endOffset &&
        entity.label === nextEntity.label
      ),
  );

  return normalizeNerEntities([...entitiesWithoutExactDuplicate, nextEntity]);
}

export function buildNerTextSegments(
  sourceText: string,
  entities: NerAnnotationEntity[],
): NerTextSegment[] {
  const normalizedEntities = normalizeNerEntities(entities).filter(
    (entity) => entity.endOffset <= sourceText.length,
  );

  if (normalizedEntities.length === 0) {
    return [
      {
        key: 'text-full',
        text: sourceText,
        entities: [],
      },
    ];
  }

  const boundarySet = new Set<number>();
  boundarySet.add(0);
  boundarySet.add(sourceText.length);

  normalizedEntities.forEach((entity) => {
    boundarySet.add(entity.startOffset);
    boundarySet.add(entity.endOffset);
  });

  const boundaries = Array.from(boundarySet).sort((a, b) => a - b);
  const segments: NerTextSegment[] = [];

  for (let i = 0; i < boundaries.length - 1; i += 1) {
    const segStart = boundaries[i];
    const segEnd = boundaries[i + 1];

    if (segStart >= segEnd) {
      continue;
    }

    const coveringEntities = normalizedEntities.filter(
      (entity) => entity.startOffset <= segStart && entity.endOffset >= segEnd,
    );

    segments.push({
      key: `seg-${segStart}-${segEnd}`,
      text: sourceText.slice(segStart, segEnd),
      entities: coveringEntities,
    });
  }

  return segments;
}

const SELECTION_TRIM_PATTERN = /^(?:[\s]|(?![()[\]{}])\p{P})+|(?:[\s]|(?![()[\]{}])\p{P})+$/gu;

function cleanSelectedText(
  rawText: string,
  rawStartOffset: number,
): { text: string; startOffset: number; endOffset: number } | null {
  const cleanedText = rawText.replace(SELECTION_TRIM_PATTERN, '');
  if (cleanedText.length === 0) {
    return null;
  }

  const leadingStripped = rawText.indexOf(cleanedText);
  const adjustedStart = rawStartOffset + leadingStripped;

  return {
    text: cleanedText,
    startOffset: adjustedStart,
    endOffset: adjustedStart + cleanedText.length,
  };
}

export function selectionOffsetsWithinElement(
  element: HTMLElement,
): { selectedText: string; startOffset: number; endOffset: number } | null {
  const selection = globalThis.window.getSelection();

  if (!selection || selection.rangeCount === 0 || selection.isCollapsed) {
    return null;
  }

  const range = selection.getRangeAt(0);
  if (!element.contains(range.commonAncestorContainer)) {
    return null;
  }

  const rawSelectedText = range.toString();
  if (rawSelectedText.trim().length === 0) {
    return null;
  }

  const anchorRange = range.cloneRange();
  anchorRange.selectNodeContents(element);
  anchorRange.setEnd(range.startContainer, range.startOffset);
  const rawStartOffset = anchorRange.toString().length;

  const cleaned = cleanSelectedText(rawSelectedText, rawStartOffset);
  if (!cleaned) {
    return null;
  }

  return {
    selectedText: cleaned.text,
    startOffset: cleaned.startOffset,
    endOffset: cleaned.endOffset,
  };
}

export function clearTextSelection(): void {
  const selection = globalThis.window.getSelection();
  selection?.removeAllRanges();
}

export function getNerEntityStyle(
  entityLabel: string,
  colorByLabel: Map<string, string | null>,
): CSSProperties | undefined {
  const color = colorByLabel.get(entityLabel);
  if (!color) {
    return undefined;
  }

  return {
    backgroundColor: `${color}30`,
    borderBottom: `2px solid ${color}`,
  };
}

export function isCsvMimeType(mimeType: string): boolean {
  return mimeType.toLowerCase().includes('csv');
}

export function isInlineSourceMimeType(mimeType: string): boolean {
  const normalizedMimeType = mimeType.toLowerCase();
  return normalizedMimeType.startsWith('image/') || normalizedMimeType.includes('pdf');
}

export function isJsonSourceMimeType(mimeType: string, fileName: string | null): boolean {
  const normalizedMimeType = mimeType.toLowerCase();
  return (
    normalizedMimeType.includes('application/json') ||
    normalizedMimeType.includes('+json') ||
    getFileExtension(fileName) === 'json'
  );
}

export function isTextSourceMimeType(mimeType: string, fileName: string | null): boolean {
  const normalizedMimeType = mimeType.toLowerCase();
  return (
    normalizedMimeType.includes('text/plain') ||
    isJsonSourceMimeType(normalizedMimeType, fileName) ||
    getFileExtension(fileName) === 'txt'
  );
}

export function formatTextSourceContent(
  mimeType: string,
  fileName: string | null,
  content: string,
): string {
  if (!isJsonSourceMimeType(mimeType, fileName)) {
    return content;
  }

  try {
    return JSON.stringify(JSON.parse(content), null, 2);
  } catch {
    return content;
  }
}

export function findCsvColumnValue(
  columnName: string,
  rowValues: Record<string, string> | null,
): string | null {
  const normalizedColumnName = columnName.trim().toLowerCase();
  if (normalizedColumnName.length === 0) {
    return null;
  }

  if (rowValues) {
    for (const [key, value] of Object.entries(rowValues)) {
      if (key.trim().toLowerCase() === normalizedColumnName) {
        return value || '';
      }
    }
  }

  return null;
}

export function getLabelStyle(
  label: ProjectSetupLabel,
  selected: boolean,
): CSSProperties | undefined {
  if (!label.color) {
    return undefined;
  }

  if (selected) {
    return {
      borderColor: label.color,
      backgroundColor: `${label.color}1A`,
      color: label.color,
    };
  }

  return {
    borderColor: `${label.color}66`,
    color: label.color,
  };
}
