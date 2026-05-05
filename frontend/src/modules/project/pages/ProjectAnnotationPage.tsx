import {
  useEffect,
  useMemo,
  useRef,
  useState,
  type CSSProperties,
  type Dispatch,
  type ReactNode,
  type SetStateAction,
} from 'react';
import {
  ArrowLeft,
  ArrowRight,
  CheckCircle2,
  ChevronDown,
  ChevronUp,
  CircleDashed,
  Tag,
  X,
  AlertTriangle,
} from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams, useSearchParams } from 'react-router';
import { toast } from 'sonner';
import { BackButton } from '@/components/common/BackButton';
import { PageContainer } from '@/components/common/PageContainer';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Spinner } from '@/components/ui/spinner';

import { Textarea } from '@/components/ui/textarea';
import {
  useProjectParticipantAnnotationWorkspaceQuery,
  useProjectAnnotationWorkspaceQuery,
  useProjectDetailQuery,
  useSaveProjectAnnotationStepMutation,
  useToggleProjectAnnotationWarningMutation,
} from '@/modules/project/hooks/useProjectQueries';
import {
  getProjectAnnotationLoadErrorMessage,
  getProjectAnnotationSaveErrorMessage,
  getProjectDatasetItemContent,
  getProjectDatasetItemContentErrorMessage,
  getProjectDetailLoadErrorMessage,
} from '@/modules/project/services/projectService';
import type {
  AnnotationStep,
  ProjectSetupLabel,
  ProjectType,
} from '@/modules/project/types/project';
import {
  completionColor,
  normalizeCompletionPercentage,
} from '@/modules/project/utils/projectUtils';

const ANNOTATION_PAGE_SIZE = 50;

type AnnotationDraft = {
  value: string;
  notes: string;
  entities: NerAnnotationEntity[];
};

type AnnotationDraftByStep = Record<string, AnnotationDraft>;

type NerAnnotationEntity = {
  label: string;
  text: string;
  startOffset: number;
  endOffset: number;
};

type PendingPageSelection = 'none' | 'first' | 'last';

type SourceCacheEntry = {
  url: string | null;
  textContent: string | null;
  mimeType: string;
  fileName: string | null;
};

type PersistCurrentStepResult = 'saved' | 'skipped' | 'error';

function annotationStepKey(step: AnnotationStep): string {
  return `${step.datasetItemId}:${step.stepIndex}`;
}

function normalizeStepIndex(stepIndex: number, totalSteps: number): number {
  if (totalSteps <= 0) {
    return 0;
  }

  return Math.min(Math.max(stepIndex, 1), totalSteps);
}

function projectTypeI18nKey(type: ProjectType): string {
  const map: Record<ProjectType, string> = {
    TEXT_CLASSIFICATION_SIMPLE: 'project.create.projectTypes.textClassificationSimple',
    TEXT_CLASSIFICATION_MULTILABEL: 'project.create.projectTypes.textClassificationMultiLabel',
    NER: 'project.create.projectTypes.ner',
    SEQ2SEQ: 'project.create.projectTypes.seq2seq',
  };
  return map[type];
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

function normalizeNerEntities(entities: NerAnnotationEntity[]): NerAnnotationEntity[] {
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

function annotationValueToNerEntities(annotation: unknown): NerAnnotationEntity[] {
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

function annotationValueToEditorValue(annotation: unknown, projectType: ProjectType): string {
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

function annotationValueToNotes(annotation: unknown): string {
  if (annotation == null || typeof annotation !== 'object' || Array.isArray(annotation)) {
    return '';
  }

  const value = annotation as Record<string, unknown>;
  return typeof value.notes === 'string' ? value.notes : '';
}

function parseCommaSeparatedLabels(rawValue: string): string[] {
  return Array.from(
    new Set(
      rawValue
        .split(',')
        .map((segment) => segment.trim())
        .filter((segment) => segment.length > 0),
    ),
  );
}

function buildAnnotationPayload(
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

function getNerSourceText(
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

  if (step.rowValues && annotationTargetColumn) {
    const normalizedTargetColumn = annotationTargetColumn.trim().toLowerCase();

    for (const [key, value] of Object.entries(step.rowValues)) {
      if (key.trim().toLowerCase() === normalizedTargetColumn) {
        return value || '';
      }
    }
  }

  return step.preview;
}

function mergeNerEntity(
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

type NerTextSegment = {
  key: string;
  text: string;
  entities: NerAnnotationEntity[];
};

function buildNerTextSegments(
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

  // Collect all boundary points from entity start/end offsets
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

    // Find all entities that cover this sub-range
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

/**
 * Regex matching leading/trailing whitespace and punctuation characters
 * to strip from annotation text selections.
 */
const SELECTION_TRIM_PATTERN = /^(?:[\s]|(?![()\[\]{}])\p{P})+|(?:[\s]|(?![()\[\]{}])\p{P})+$/gu;

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

function selectionOffsetsWithinElement(
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

function clearTextSelection(): void {
  const selection = globalThis.window.getSelection();
  selection?.removeAllRanges();
}

function getNerEntityStyle(
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

function isCsvMimeType(mimeType: string): boolean {
  return mimeType.toLowerCase().includes('csv');
}

function isInlineSourceMimeType(mimeType: string): boolean {
  const normalizedMimeType = mimeType.toLowerCase();
  return normalizedMimeType.startsWith('image/') || normalizedMimeType.includes('pdf');
}

function getFileExtension(fileName: string | null): string {
  if (!fileName) {
    return '';
  }

  const normalizedName = fileName.trim().toLowerCase();
  const lastDotIndex = normalizedName.lastIndexOf('.');
  if (lastDotIndex < 0 || lastDotIndex === normalizedName.length - 1) {
    return '';
  }

  return normalizedName.slice(lastDotIndex + 1);
}

function isJsonSourceMimeType(mimeType: string, fileName: string | null): boolean {
  const normalizedMimeType = mimeType.toLowerCase();
  return (
    normalizedMimeType.includes('application/json') ||
    normalizedMimeType.includes('+json') ||
    getFileExtension(fileName) === 'json'
  );
}

function isTextSourceMimeType(mimeType: string, fileName: string | null): boolean {
  const normalizedMimeType = mimeType.toLowerCase();
  return (
    normalizedMimeType.includes('text/plain') ||
    isJsonSourceMimeType(normalizedMimeType, fileName) ||
    getFileExtension(fileName) === 'txt'
  );
}

function formatTextSourceContent(
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

type CsvStepPreview = {
  headerColumns: string[];
  rowColumns: string[];
};

type CsvLabelColumnValue = {
  name: string;
  value: string;
};

function parseCsvLine(line: string): string[] {
  const values: string[] = [];
  let currentValue = '';
  let insideQuotes = false;

  for (let index = 0; index < line.length; index += 1) {
    const char = line[index];

    if (char === '"') {
      if (insideQuotes && line[index + 1] === '"') {
        currentValue += '"';
        index += 1;
      } else {
        insideQuotes = !insideQuotes;
      }

      continue;
    }

    if (char === ',' && !insideQuotes) {
      values.push(currentValue.trim());
      currentValue = '';
      continue;
    }

    currentValue += char;
  }

  values.push(currentValue.trim());

  return values;
}

function parseCsvStepPreview(preview: string): CsvStepPreview | null {
  const lines = preview
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line.length > 0);

  if (lines.length < 2) {
    return null;
  }

  const headerColumns = parseCsvLine(lines[0]);
  const rowColumns = parseCsvLine(lines[1]);

  if (headerColumns.length === 0 || rowColumns.length === 0) {
    return null;
  }

  return {
    headerColumns,
    rowColumns,
  };
}

function findCsvColumnValue(
  columnName: string,
  rowValues: Record<string, string> | null,
  csvStepPreview: CsvStepPreview | null,
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

  if (csvStepPreview) {
    const columnIndex = csvStepPreview.headerColumns.findIndex(
      (header) => header.trim().toLowerCase() === normalizedColumnName,
    );

    if (columnIndex >= 0) {
      return csvStepPreview.rowColumns[columnIndex] ?? '';
    }
  }

  return null;
}

function getLabelStyle(label: ProjectSetupLabel, selected: boolean): CSSProperties | undefined {
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

type StepCursorState = {
  activeStepOnPage: number;
  currentGlobalStepIndex: number;
  canMovePrevious: boolean;
  canMoveNext: boolean;
  goToPreviousStep: () => void;
  goToNextStep: () => void;
};

function useAnnotationStepCursor(
  stepsLength: number,
  totalSteps: number,
  annotationOffset: number,
  setAnnotationOffset: Dispatch<SetStateAction<number>>,
  resumeGlobalStepIndex: number | null,
  setResumeGlobalStepIndex: Dispatch<SetStateAction<number | null>>,
): StepCursorState {
  const [activeStepOnPage, setActiveStepOnPage] = useState(0);
  const [pendingPageSelection, setPendingPageSelection] = useState<PendingPageSelection>('none');

  useEffect(() => {
    if (stepsLength === 0) {
      setActiveStepOnPage(0);
      return;
    }

    if (pendingPageSelection === 'first') {
      setActiveStepOnPage(0);
      setPendingPageSelection('none');
      return;
    }

    if (pendingPageSelection === 'last') {
      setActiveStepOnPage(stepsLength - 1);
      setPendingPageSelection('none');
      return;
    }

    setActiveStepOnPage((previous) => Math.min(previous, stepsLength - 1));
  }, [pendingPageSelection, stepsLength]);

  useEffect(() => {
    if (resumeGlobalStepIndex == null || totalSteps <= 0) {
      return;
    }

    const normalizedStepIndex = normalizeStepIndex(resumeGlobalStepIndex, totalSteps);
    const targetOffset =
      Math.floor((normalizedStepIndex - 1) / ANNOTATION_PAGE_SIZE) * ANNOTATION_PAGE_SIZE;

    if (annotationOffset !== targetOffset) {
      setAnnotationOffset(targetOffset);
      return;
    }

    const targetStepOnPage = normalizedStepIndex - 1 - annotationOffset;
    if (targetStepOnPage >= 0 && targetStepOnPage < stepsLength) {
      setActiveStepOnPage(targetStepOnPage);
      setResumeGlobalStepIndex(null);
    }
  }, [
    annotationOffset,
    resumeGlobalStepIndex,
    setAnnotationOffset,
    setResumeGlobalStepIndex,
    stepsLength,
    totalSteps,
  ]);

  const hasCurrentStep = stepsLength > 0;

  const currentGlobalStepIndex = hasCurrentStep
    ? annotationOffset + Math.min(activeStepOnPage, stepsLength - 1) + 1
    : 0;

  const canMovePrevious = hasCurrentStep && currentGlobalStepIndex > 1;
  const canMoveNext = hasCurrentStep && currentGlobalStepIndex < totalSteps;

  const goToPreviousStep = () => {
    if (!hasCurrentStep) {
      return;
    }

    if (activeStepOnPage > 0) {
      setActiveStepOnPage((previous) => previous - 1);
      return;
    }

    if (annotationOffset > 0) {
      setPendingPageSelection('last');
      setAnnotationOffset((previous) => Math.max(previous - ANNOTATION_PAGE_SIZE, 0));
    }
  };

  const goToNextStep = () => {
    if (!hasCurrentStep) {
      return;
    }

    if (activeStepOnPage < stepsLength - 1) {
      setActiveStepOnPage((previous) => previous + 1);
      return;
    }

    const hasMoreSteps = annotationOffset + stepsLength < totalSteps;
    if (hasMoreSteps) {
      setPendingPageSelection('first');
      setAnnotationOffset((previous) => previous + ANNOTATION_PAGE_SIZE);
    }
  };

  return {
    activeStepOnPage,
    currentGlobalStepIndex,
    canMovePrevious,
    canMoveNext,
    goToPreviousStep,
    goToNextStep,
  };
}

type SourcePreviewState = {
  isSourceLoading: boolean;
  sourceUrl: string | null;
  sourceTextContent: string | null;
  sourceMimeType: string;
  sourceFileName: string | null;
  sourceLoadError: string;
};

function useAnnotationSourcePreview(
  currentStep: AnnotationStep | null,
  numericProjectId: number,
): SourcePreviewState {
  const [isSourceLoading, setIsSourceLoading] = useState(false);
  const [sourceUrl, setSourceUrl] = useState<string | null>(null);
  const [sourceTextContent, setSourceTextContent] = useState<string | null>(null);
  const [sourceMimeType, setSourceMimeType] = useState('');
  const [sourceFileName, setSourceFileName] = useState<string | null>(null);
  const [sourceLoadError, setSourceLoadError] = useState('');

  const sourceCacheRef = useRef<Map<number, SourceCacheEntry>>(new Map());

  useEffect(() => {
    setSourceLoadError('');

    if (!currentStep) {
      setSourceUrl(null);
      setSourceTextContent(null);
      setSourceMimeType('');
      setSourceFileName(null);
      setIsSourceLoading(false);
      return;
    }

    setSourceMimeType(currentStep.sourceMimeType);
    setSourceFileName(currentStep.sourceName);

    if (isCsvMimeType(currentStep.sourceMimeType)) {
      setSourceUrl(null);
      setSourceTextContent(null);
      setIsSourceLoading(false);
      return;
    }

    const shouldLoadBinarySource = isInlineSourceMimeType(currentStep.sourceMimeType);
    const shouldLoadTextSource = isTextSourceMimeType(
      currentStep.sourceMimeType,
      currentStep.sourceName,
    );

    if (!shouldLoadBinarySource && !shouldLoadTextSource) {
      setSourceUrl(null);
      setSourceTextContent(null);
      setIsSourceLoading(false);
      return;
    }

    const cachedSource = sourceCacheRef.current.get(currentStep.datasetItemId);
    if (cachedSource) {
      setSourceUrl(cachedSource.url);
      setSourceTextContent(cachedSource.textContent);
      setSourceMimeType(cachedSource.mimeType);
      setSourceFileName(cachedSource.fileName ?? currentStep.sourceName);
      setIsSourceLoading(false);
      return;
    }

    let cancelled = false;
    setSourceUrl(null);
    setSourceTextContent(null);
    setIsSourceLoading(true);

    getProjectDatasetItemContent(numericProjectId, currentStep.datasetItemId)
      .then(async (content) => {
        const resolvedMimeType = content.mimeType;
        const resolvedFileName = content.fileName ?? currentStep.sourceName;
        const shouldRenderAsText = isTextSourceMimeType(resolvedMimeType, resolvedFileName);

        let nextSourceUrl: string | null = null;
        let nextTextContent: string | null = null;

        if (shouldRenderAsText) {
          const rawTextContent = await content.blob.text();
          nextTextContent = formatTextSourceContent(
            resolvedMimeType,
            resolvedFileName,
            rawTextContent,
          );
        } else {
          nextSourceUrl = URL.createObjectURL(content.blob);
        }

        if (cancelled) {
          if (nextSourceUrl != null) {
            URL.revokeObjectURL(nextSourceUrl);
          }

          return;
        }

        sourceCacheRef.current.set(currentStep.datasetItemId, {
          url: nextSourceUrl,
          textContent: nextTextContent,
          mimeType: resolvedMimeType,
          fileName: resolvedFileName,
        });

        setSourceUrl(nextSourceUrl);
        setSourceTextContent(nextTextContent);
        setSourceMimeType(resolvedMimeType);
        setSourceFileName(resolvedFileName);
      })
      .catch((error) => {
        if (cancelled) {
          return;
        }

        setSourceUrl(null);
        setSourceTextContent(null);
        setSourceLoadError(getProjectDatasetItemContentErrorMessage(error));
      })
      .finally(() => {
        if (!cancelled) {
          setIsSourceLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [currentStep, numericProjectId]);

  useEffect(() => {
    return () => {
      sourceCacheRef.current.forEach((source) => {
        if (source.url != null) {
          URL.revokeObjectURL(source.url);
        }
      });
      sourceCacheRef.current.clear();
    };
  }, []);

  return {
    isSourceLoading,
    sourceUrl,
    sourceTextContent,
    sourceMimeType,
    sourceFileName,
    sourceLoadError,
  };
}

// eslint-disable-next-line sonarjs/cognitive-complexity
export default function ProjectAnnotationPage() {
  // NOSONAR - Flujo completo de anotacion en una sola pantalla.
  const { t } = useTranslation(); // NOSONAR
  const navigate = useNavigate();
  const { projectId } = useParams();
  const [searchParams] = useSearchParams();

  const numericProjectId = useMemo(() => Number(projectId), [projectId]);
  const isInvalidProjectId = !Number.isFinite(numericProjectId) || numericProjectId <= 0;
  const reviewedParticipantUserId = useMemo(() => {
    const rawParticipantUserId = searchParams.get('participantUserId');
    if (!rawParticipantUserId) {
      return null;
    }

    const parsedParticipantUserId = Number(rawParticipantUserId);
    if (!Number.isInteger(parsedParticipantUserId) || parsedParticipantUserId <= 0) {
      return null;
    }

    return parsedParticipantUserId;
  }, [searchParams]);
  const isReviewMode = reviewedParticipantUserId != null;

  const [annotationOffset, setAnnotationOffset] = useState(0);
  const [resumeGlobalStepIndex, setResumeGlobalStepIndex] = useState<number | null>(null);
  const [hasResolvedResumeStep, setHasResolvedResumeStep] = useState(false);
  const [draftByStep, setDraftByStep] = useState<AnnotationDraftByStep>({});
  const [savingStepId, setSavingStepId] = useState<string | null>(null);
  const [guidelinePdfUrl, setGuidelinePdfUrl] = useState<string | null>(null);
  const [guidelinePdfError, setGuidelinePdfError] = useState('');
  const [isGuidelineCollapsed, setIsGuidelineCollapsed] = useState(true);
  const [activeNerLabel, setActiveNerLabel] = useState('');
  const nerSourceSelectionRef = useRef<HTMLDivElement | null>(null);

  const {
    data: project,
    isLoading: isProjectLoading,
    isError: isProjectError,
    error: projectError,
  } = useProjectDetailQuery(numericProjectId);

  const ownAnnotationWorkspaceQuery = useProjectAnnotationWorkspaceQuery(
    numericProjectId,
    annotationOffset,
    ANNOTATION_PAGE_SIZE,
    { enabled: !isReviewMode },
  );

  const participantAnnotationWorkspaceQuery = useProjectParticipantAnnotationWorkspaceQuery(
    numericProjectId,
    reviewedParticipantUserId ?? 0,
    annotationOffset,
    ANNOTATION_PAGE_SIZE,
    { enabled: isReviewMode },
  );

  const annotationWorkspace = isReviewMode
    ? participantAnnotationWorkspaceQuery.data
    : ownAnnotationWorkspaceQuery.data;
  const isAnnotationWorkspaceLoading = isReviewMode
    ? participantAnnotationWorkspaceQuery.isLoading
    : ownAnnotationWorkspaceQuery.isLoading;
  const isAnnotationWorkspaceError = isReviewMode
    ? participantAnnotationWorkspaceQuery.isError
    : ownAnnotationWorkspaceQuery.isError;
  const annotationWorkspaceError = isReviewMode
    ? participantAnnotationWorkspaceQuery.error
    : ownAnnotationWorkspaceQuery.error;

  const saveProjectAnnotationStepMutation = useSaveProjectAnnotationStepMutation(
    annotationOffset,
    ANNOTATION_PAGE_SIZE,
  );

  const toggleProjectAnnotationWarningMutation = useToggleProjectAnnotationWarningMutation(
    annotationOffset,
    ANNOTATION_PAGE_SIZE,
  );

  const reviewedParticipant = useMemo(() => {
    if (!project || reviewedParticipantUserId == null) {
      return null;
    }

    return (
      project.participants.find(
        (participant) => participant.userId === reviewedParticipantUserId,
      ) ?? null
    );
  }, [project, reviewedParticipantUserId]);

  const detailErrorMessage = useMemo(() => {
    if (isInvalidProjectId) {
      return t('project.annotationPage.errors.invalidId');
    }

    if (isProjectError) {
      return getProjectDetailLoadErrorMessage(projectError);
    }

    return '';
  }, [isInvalidProjectId, isProjectError, projectError, t]);

  const annotationWorkspaceErrorMessage = useMemo(() => {
    if (isInvalidProjectId || !isAnnotationWorkspaceError) {
      return '';
    }

    return getProjectAnnotationLoadErrorMessage(annotationWorkspaceError);
  }, [annotationWorkspaceError, isAnnotationWorkspaceError, isInvalidProjectId]);

  useEffect(() => {
    if (detailErrorMessage.length > 0) {
      toast.error(detailErrorMessage, { id: 'project-annotation-detail-load-error' });
    }
  }, [detailErrorMessage]);

  useEffect(() => {
    if (annotationWorkspaceErrorMessage.length > 0) {
      toast.error(annotationWorkspaceErrorMessage, {
        id: 'project-annotation-workspace-load-error',
      });
    }
  }, [annotationWorkspaceErrorMessage]);

  useEffect(() => {
    let createdGuidelineUrl: string | null = null;
    setGuidelinePdfUrl(null);
    setGuidelinePdfError('');

    const guidelinePdfBase64 = project?.guidelinePdfBase64;
    if (!guidelinePdfBase64) {
      return;
    }

    try {
      const normalizedGuidelinePdfBase64 = guidelinePdfBase64.includes(',')
        ? (guidelinePdfBase64.split(',').pop() ?? guidelinePdfBase64)
        : guidelinePdfBase64;

      const binary = globalThis.atob(normalizedGuidelinePdfBase64);
      const bytes = new Uint8Array(binary.length);

      for (let index = 0; index < binary.length; index += 1) {
        bytes[index] = binary.codePointAt(index) ?? 0;
      }

      createdGuidelineUrl = URL.createObjectURL(new Blob([bytes], { type: 'application/pdf' }));
      setGuidelinePdfUrl(createdGuidelineUrl);
    } catch {
      setGuidelinePdfError(t('project.detail.openGuidelinePdfError'));
    }

    return () => {
      if (createdGuidelineUrl != null) {
        URL.revokeObjectURL(createdGuidelineUrl);
      }
    };
  }, [project?.guidelinePdfBase64, t]);

  const steps = annotationWorkspace?.steps ?? [];
  const totalSteps = annotationWorkspace?.totalSteps ?? 0;

  useEffect(() => {
    setResumeGlobalStepIndex(null);
    setHasResolvedResumeStep(false);
  }, [numericProjectId]);

  useEffect(() => {
    if (hasResolvedResumeStep || isInvalidProjectId || !annotationWorkspace) {
      return;
    }

    const total = annotationWorkspace.totalSteps;
    if (total <= 0) {
      setHasResolvedResumeStep(true);
      return;
    }

    const isCompleted = annotationWorkspace.completionPercentage >= 100;
    const isFirstTime = annotationWorkspace.completedSteps <= 0;

    const initialStep = normalizeStepIndex(
      isCompleted || isFirstTime ? 1 : annotationWorkspace.firstPendingStepIndex,
      total,
    );

    setResumeGlobalStepIndex(initialStep);
    setHasResolvedResumeStep(true);
  }, [annotationWorkspace, hasResolvedResumeStep, isInvalidProjectId]);

  const {
    activeStepOnPage,
    currentGlobalStepIndex,
    canMovePrevious,
    canMoveNext,
    goToPreviousStep,
    goToNextStep,
  } = useAnnotationStepCursor(
    steps.length,
    totalSteps,
    annotationOffset,
    setAnnotationOffset,
    resumeGlobalStepIndex,
    setResumeGlobalStepIndex,
  );

  const currentStep =
    steps.length === 0 ? null : steps[Math.min(activeStepOnPage, steps.length - 1)];

  const {
    isSourceLoading,
    sourceUrl,
    sourceTextContent,
    sourceMimeType,
    sourceFileName,
    sourceLoadError,
  } = useAnnotationSourcePreview(currentStep, numericProjectId);

  const annotationProjectType =
    annotationWorkspace?.projectType ?? project?.projectType ?? 'TEXT_CLASSIFICATION_SIMPLE';
  const annotationTargetColumn =
    annotationWorkspace?.annotationTargetColumn ?? project?.annotationTargetColumn ?? null;
  const labels = annotationWorkspace?.labels ?? project?.labels ?? [];

  useEffect(() => {
    if (annotationProjectType !== 'NER') {
      if (activeNerLabel.length > 0) {
        setActiveNerLabel('');
      }
      return;
    }

    if (activeNerLabel.length === 0) {
      return;
    }

    if (labels.some((label) => label.name === activeNerLabel)) {
      return;
    }

    setActiveNerLabel('');
  }, [activeNerLabel, annotationProjectType, labels]);

  const getDraft = (step: AnnotationStep): AnnotationDraft => {
    const stepId = annotationStepKey(step);
    const draft = draftByStep[stepId];
    if (draft) {
      return draft;
    }

    return {
      value: annotationValueToEditorValue(step.annotation, annotationProjectType),
      notes: annotationValueToNotes(step.annotation),
      entities: annotationValueToNerEntities(step.annotation),
    };
  };

  const updateDraft = (step: AnnotationStep, patch: Partial<AnnotationDraft>) => {
    if (isReviewMode) {
      return;
    }

    const stepId = annotationStepKey(step);
    const currentDraft = getDraft(step);

    setDraftByStep((previous) => ({
      ...previous,
      [stepId]: {
        ...currentDraft,
        ...patch,
      },
    }));
  };

  const toggleLabel = (step: AnnotationStep, labelName: string) => {
    const currentDraft = getDraft(step);
    const selectedLabels = parseCommaSeparatedLabels(currentDraft.value);
    const hasLabel = selectedLabels.includes(labelName);

    const nextLabels = hasLabel
      ? selectedLabels.filter((existingLabel) => existingLabel !== labelName)
      : [...selectedLabels, labelName];

    updateDraft(step, { value: nextLabels.join(', ') });
  };

  const handleNerSourceSelection = () => {
    if (isReviewMode || !currentStep || annotationProjectType !== 'NER') {
      return;
    }

    const sourceElement = nerSourceSelectionRef.current;
    if (!sourceElement) {
      return;
    }

    const selection = selectionOffsetsWithinElement(sourceElement);
    if (!selection) {
      return;
    }

    if (activeNerLabel.length === 0) {
      toast.error(t('project.annotationPage.errors.nerLabelRequired'));
      clearTextSelection();
      return;
    }

    const nextEntity: NerAnnotationEntity = {
      label: activeNerLabel,
      text: selection.selectedText,
      startOffset: selection.startOffset,
      endOffset: selection.endOffset,
    };

    const mergedEntities = mergeNerEntity(getDraft(currentStep).entities, nextEntity);
    updateDraft(currentStep, { entities: mergedEntities });
    clearTextSelection();
  };

  const removeNerEntity = (step: AnnotationStep | null, entityToRemove: NerAnnotationEntity) => {
    if (isReviewMode || !step) {
      return;
    }

    const currentEntities = getDraft(step).entities;
    updateDraft(step, {
      entities: currentEntities.filter(
        (entity) =>
          !(
            entity.label === entityToRemove.label &&
            entity.startOffset === entityToRemove.startOffset &&
            entity.endOffset === entityToRemove.endOffset
          ),
      ),
    });
  };

  useEffect(() => {
    if (annotationProjectType !== 'NER' || isReviewMode) {
      return;
    }

    const handleMouseUp = () => {
      handleNerSourceSelection();
    };

    globalThis.document.addEventListener('mouseup', handleMouseUp);

    return () => {
      globalThis.document.removeEventListener('mouseup', handleMouseUp);
    };
  }, [annotationProjectType, handleNerSourceSelection, isReviewMode]);

  const persistCurrentStep = async (): Promise<PersistCurrentStepResult> => {
    if (isReviewMode) {
      return 'skipped';
    }

    if (!currentStep) {
      return 'skipped';
    }

    const draft = getDraft(currentStep);
    const payload = buildAnnotationPayload(
      annotationProjectType,
      draft.value,
      draft.notes,
      draft.entities,
    );

    const stepId = annotationStepKey(currentStep);
    setSavingStepId(stepId);

    try {
      await saveProjectAnnotationStepMutation.mutateAsync({
        projectId: numericProjectId,
        datasetItemId: currentStep.datasetItemId,
        stepIndex: currentStep.stepIndex,
        annotation: payload,
      });

      return 'saved';
    } catch (error) {
      toast.error(getProjectAnnotationSaveErrorMessage(error));
      return 'error';
    } finally {
      setSavingStepId(null);
    }
  };

  const runAfterPersist = async (onSuccess: () => void) => {
    if (isReviewMode) {
      onSuccess();
      return;
    }

    const result = await persistCurrentStep();
    if (result !== 'error') {
      onSuccess();
    }
  };

  const handleBackNavigation = async () => {
    if (isReviewMode) {
      return true;
    }

    const result = await persistCurrentStep();
    if (result === 'error') {
      return false;
    }

    if (result === 'saved') {
      toast.success(t('project.annotationPage.autoSaved'));
    }

    return true;
  };

  const handlePreviousAction = async () => {
    await runAfterPersist(goToPreviousStep);
  };

  const currentStepId = currentStep ? annotationStepKey(currentStep) : null;
  const currentDraft = currentStep
    ? getDraft(currentStep)
    : { value: '', notes: '', entities: [] as NerAnnotationEntity[] };
  const handleNextAction = async () => {
    await runAfterPersist(goToNextStep);
  };

  const handleToggleWarning = async () => {
    if (!currentStep || !isReviewMode || reviewedParticipantUserId == null) {
      return;
    }

    try {
      await toggleProjectAnnotationWarningMutation.mutateAsync({
        projectId: numericProjectId,
        participantUserId: reviewedParticipantUserId,
        datasetItemId: currentStep.datasetItemId,
        stepIndex: currentStep.stepIndex,
      });
    } catch (error) {
      toast.error(t('project.annotationPage.errors.warningToggleFailed'));
    }
  };

  const handleFinishAction = async () => {
    await runAfterPersist(() => navigate(`/home/projects/${numericProjectId}`));
  };

  const completionPercentage = normalizeCompletionPercentage(
    annotationWorkspace?.completionPercentage ?? 0,
  );
  const completionColors = completionColor(completionPercentage);

  const csvStepPreview = useMemo(() => {
    if (!currentStep || !isCsvMimeType(currentStep.sourceMimeType)) {
      return null;
    }

    return parseCsvStepPreview(currentStep.preview);
  }, [currentStep]);

  const csvTargetColumnValue = useMemo(() => {
    if (!currentStep || !isCsvMimeType(currentStep.sourceMimeType)) {
      return null;
    }

    const normalizedTarget = annotationTargetColumn?.trim().toLowerCase() ?? '';

    if (normalizedTarget.length > 0) {
      const targetValue = findCsvColumnValue(
        normalizedTarget,
        currentStep.rowValues,
        csvStepPreview,
      );

      if (targetValue != null) {
        return targetValue;
      }
    }

    return currentStep.preview;
  }, [annotationTargetColumn, csvStepPreview, currentStep]);

  const csvLabelColumnValues = useMemo<CsvLabelColumnValue[]>(() => {
    if (!currentStep || !isCsvMimeType(currentStep.sourceMimeType)) {
      return [];
    }

    const normalizedTarget = annotationTargetColumn?.trim().toLowerCase() ?? '';

    return labels
      .map((label) => ({
        name: label.name,
        value: findCsvColumnValue(label.name, currentStep.rowValues, csvStepPreview),
      }))
      .filter((entry): entry is CsvLabelColumnValue => {
        const normalizedName = entry.name.trim().toLowerCase();

        return (
          normalizedName.length > 0 && normalizedName !== normalizedTarget && entry.value !== null
        );
      });
  }, [annotationTargetColumn, csvStepPreview, currentStep, labels]);

  const selectedLabels = useMemo(() => {
    return parseCommaSeparatedLabels(currentDraft.value);
  }, [currentDraft.value]);

  const nerSourceText = useMemo(() => {
    if (annotationProjectType !== 'NER') {
      return '';
    }

    return getNerSourceText(currentStep, sourceTextContent, annotationTargetColumn);
  }, [annotationProjectType, currentStep, sourceTextContent, annotationTargetColumn]);

  const nerTextSegments = useMemo(() => {
    if (annotationProjectType !== 'NER' || nerSourceText.length === 0) {
      return [];
    }

    return buildNerTextSegments(nerSourceText, currentDraft.entities);
  }, [annotationProjectType, currentDraft.entities, nerSourceText]);

  const nerLabelColorMap = useMemo(() => {
    return new Map(labels.map((label) => [label.name, label.color]));
  }, [labels]);

  const isFirstStep = currentStep != null && currentGlobalStepIndex <= 1;
  const isLastStep = currentStep != null && totalSteps > 0 && currentGlobalStepIndex >= totalSteps;
  const hasRenderableStep = currentStep != null;
  const areStepActionsDisabled =
    isAnnotationWorkspaceLoading || (!isReviewMode && saveProjectAnnotationStepMutation.isPending);
  const isSavingCurrentStep =
    saveProjectAnnotationStepMutation.isPending && savingStepId === currentStepId;

  const canRenderWorkspace =
    !isProjectLoading &&
    !isAnnotationWorkspaceLoading &&
    detailErrorMessage.length === 0 &&
    project != null &&
    annotationWorkspace != null;

  const shouldShowHeaderProgress = !isInvalidProjectId && canRenderWorkspace;
  const reviewedParticipantLabel = reviewedParticipant
    ? `${reviewedParticipant.firstName} ${reviewedParticipant.lastName}`
    : null;
  const classificationHeading = isReviewMode
    ? t('project.annotationPage.classificationTitle')
    : `${t('project.annotationPage.classificationTitle')} *`;
  let csvSourceContent: ReactNode = (
    <pre className="whitespace-pre-wrap px-3 py-2 text-sm leading-relaxed text-foreground/90">
      {currentStep?.preview ?? ''}
    </pre>
  );
  const csvLabelColumnContent =
    csvLabelColumnValues.length > 0 ? (
      <div className="mt-6">
        <h3 className="text-sm font-bold uppercase tracking-wider text-muted-foreground">
          {t('project.annotationPage.csvLabelOptionsTitle')}
        </h3>
        <div className="mt-3 space-y-4">
          {csvLabelColumnValues.map((columnValue) => (
            <div key={columnValue.name}>
              <p className="truncate text-xs font-bold uppercase tracking-wider text-muted-foreground">
                {columnValue.name}
              </p>
              <p className="mt-2 whitespace-pre-wrap text-sm leading-relaxed text-foreground/90">
                {columnValue.value}
              </p>
            </div>
          ))}
        </div>
      </div>
    ) : null;

  if (csvTargetColumnValue != null) {
    const columnHeader = annotationTargetColumn ? (
      <span className="text-xs font-bold uppercase tracking-wider text-muted-foreground">
        {annotationTargetColumn}
      </span>
    ) : null;

    if (annotationProjectType === 'NER') {
      csvSourceContent = (
        <div>
          {columnHeader}
          <div
            aria-label={t('project.annotationPage.nerSelectionAreaLabel')}
            className="mt-2 whitespace-pre-wrap text-sm leading-relaxed text-foreground/90 select-text"
            ref={nerSourceSelectionRef}
          >
            {nerTextSegments.map((segment) => {
              if (segment.entities.length === 0) {
                return <span key={segment.key}>{segment.text}</span>;
              }

              const segEnd = Number.parseInt(segment.key.split('-')[2], 10);

              const nestedMarks = segment.entities.reduce(
                (acc, entity) => (
                  <mark
                    className="rounded-xs px-px text-current"
                    key={`${entity.label}-${entity.startOffset}`}
                    style={getNerEntityStyle(entity.label, nerLabelColorMap)}
                    title={entity.label}
                  >
                    {acc}
                  </mark>
                ),
                <>{segment.text}</>,
              );

              return (
                <span className="group/ner relative inline" key={segment.key}>
                  {nestedMarks}
                  {!isReviewMode &&
                    segment.entities
                      .filter((entity) => entity.endOffset === segEnd)
                      .map((entity, index) => (
                        <button
                          aria-label={t('project.annotationPage.removeEntity')}
                          className="absolute -top-2 -right-2 inline-flex size-4 items-center justify-center rounded-full border border-border bg-background text-muted-foreground opacity-0 shadow-sm transition-opacity hover:text-destructive group-hover/ner:opacity-100"
                          key={`${entity.label}-${entity.startOffset}`}
                          onClick={() => removeNerEntity(currentStep, entity)}
                          onMouseDown={(event) => {
                            event.preventDefault();
                            event.stopPropagation();
                          }}
                          style={{
                            transform: index > 0 ? `translateX(${index * 1}rem)` : undefined,
                          }}
                          type="button"
                        >
                          <X aria-hidden className="size-3" />
                        </button>
                      ))}
                </span>
              );
            })}
          </div>
          {csvLabelColumnContent}
        </div>
      );
    } else {
      csvSourceContent = (
        <div>
          {columnHeader}
          <p className="mt-2 whitespace-pre-wrap text-sm leading-relaxed text-foreground/90">
            {csvTargetColumnValue}
          </p>
          {csvLabelColumnContent}
        </div>
      );
    }
  }

  let guidelineContent = (
    <p className="text-sm text-muted-foreground">{t('project.detail.noGuideline')}</p>
  );

  if (project?.guidelineText) {
    guidelineContent = (
      <p className="whitespace-pre-wrap text-sm leading-relaxed text-foreground/90">
        {project.guidelineText}
      </p>
    );
  } else if (guidelinePdfUrl != null) {
    guidelineContent = (
      <iframe
        className="h-120 w-full rounded-md border border-border bg-background"
        src={guidelinePdfUrl}
        title={t('project.annotationPage.guidelineTitle')}
      />
    );
  } else if (guidelinePdfError.length > 0) {
    guidelineContent = <p className="text-sm text-destructive">{guidelinePdfError}</p>;
  } else if (project?.guidelinePdfBase64) {
    guidelineContent = (
      <p className="text-sm text-muted-foreground">
        {t('project.annotationPage.guidelinePdfLoading')}
      </p>
    );
  }

  return (
    <PageContainer className="mb-16">
      {isReviewMode && (
        <h1 className="text-3xl font-black tracking-tight text-primary sm:text-4xl">
          {reviewedParticipantLabel ?? String(reviewedParticipantUserId)}
        </h1>
      )}

      <div className="flex items-center justify-between gap-3">
        <BackButton
          disabled={areStepActionsDisabled}
          fallbackTo={isInvalidProjectId ? '/home/projects' : `/home/projects/${numericProjectId}`}
          onBeforeNavigate={handleBackNavigation}
        />
        {shouldShowHeaderProgress && (
          <div className="w-full max-w-64 space-y-2">
            <div className="flex items-center justify-between text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              <span className="text-xs text-muted-foreground">
                {annotationWorkspace.completedSteps}/{annotationWorkspace.totalSteps}
              </span>
              <span className={completionColors.text}>{completionPercentage}%</span>
            </div>
            <div className="h-2.5 w-full overflow-hidden rounded-full bg-muted/60">
              <div
                className={`h-full rounded-full transition-all duration-500 ${completionColors.bar}`}
                style={{ width: `${completionPercentage}%` }}
              />
            </div>
          </div>
        )}
      </div>

      {(isProjectLoading || isAnnotationWorkspaceLoading) && (
        <div className="text-sm text-muted-foreground">
          <span className="inline-flex items-center gap-2">
            <Spinner aria-hidden className="size-4" />
            {t('project.annotationPage.loading')}
          </span>
        </div>
      )}

      {canRenderWorkspace && (
        <div className="space-y-4">
          {currentStep == null ? (
            <section className="rounded-xl border border-border bg-surface-base p-6">
              <p className="text-sm text-muted-foreground">
                {t('project.annotationPage.emptySteps')}
              </p>
            </section>
          ) : (
            <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_360px]">
              <div className="min-w-0 space-y-4">
                <section className="rounded-xl border border-border bg-surface-base p-5 sm:p-6">
                  <div className="flex items-center justify-between gap-3">
                    <h2 className="text-sm font-bold tracking-[0.12em] text-muted-foreground uppercase">
                      {t('project.annotationPage.guidelineTitle')}
                    </h2>
                    <Button
                      aria-controls="annotation-guideline-content"
                      aria-expanded={!isGuidelineCollapsed}
                      className="h-8 gap-1 px-2 text-xs"
                      onClick={() => setIsGuidelineCollapsed((previous) => !previous)}
                      type="button"
                      variant="ghost"
                    >
                      {isGuidelineCollapsed ? (
                        <ChevronDown aria-hidden className="size-4" />
                      ) : (
                        <ChevronUp aria-hidden className="size-4" />
                      )}
                    </Button>
                  </div>

                  {!isGuidelineCollapsed && (
                    <div
                      className="mt-3 border-t pt-3 bg-surface-soft"
                      id="annotation-guideline-content"
                    >
                      {guidelineContent}
                    </div>
                  )}
                </section>

                <section className="min-w-0 rounded-xl border border-border bg-surface-base p-5 sm:p-6">
                  <div className="flex flex-wrap items-center justify-between gap-2 border-b border-border pb-3">
                    <div>
                      <h2 className="text-sm font-bold tracking-[0.12em] text-muted-foreground uppercase">
                        {t('project.annotationPage.sourceTitle')}
                      </h2>
                      <p className="mt-1 text-sm font-medium text-primary">
                        {sourceFileName ?? currentStep.sourceName}
                      </p>
                    </div>
                    <div className="flex items-center gap-2">
                      {currentStep.completed && (isReviewMode || currentStep.warning) && (
                        <button
                          aria-label={t('project.annotationPage.warningToggleLabel')}
                          className={[
                            'inline-flex items-center justify-center transition-colors',
                            currentStep.warning ? 'text-red-500' : 'text-muted-foreground/40',
                            isReviewMode ? 'cursor-pointer hover:text-red-400' : 'cursor-default',
                          ].join(' ')}
                          disabled={
                            !isReviewMode || toggleProjectAnnotationWarningMutation.isPending
                          }
                          onClick={handleToggleWarning}
                          title={t('project.annotationPage.warningTooltip')}
                          type="button"
                        >
                          <AlertTriangle aria-hidden className="size-5" />
                        </button>
                      )}
                      <span className="inline-flex items-center px-2 py-1 text-xs font-semibold text-muted-foreground">
                        {currentGlobalStepIndex}/{totalSteps}
                      </span>
                      <span
                        className={[
                          'inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs font-semibold',
                          currentStep.completed
                            ? 'bg-emerald-100 text-emerald-700'
                            : 'bg-amber-100 text-amber-700',
                        ].join(' ')}
                      >
                        {currentStep.completed ? (
                          <CheckCircle2 aria-hidden className="size-3.5" />
                        ) : (
                          <CircleDashed aria-hidden className="size-3.5" />
                        )}
                        {currentStep.completed
                          ? t('project.annotationPage.status.completed')
                          : t('project.annotationPage.status.pending')}
                      </span>
                    </div>
                  </div>

                  <div className="mt-4 xl:min-h-64 min-w-0">
                    {isSourceLoading && (
                      <div className="flex h-95 items-center justify-center text-sm text-muted-foreground">
                        <span className="inline-flex items-center gap-2">
                          <Spinner aria-hidden className="size-4" />
                          {t('project.annotationPage.sourceLoading')}
                        </span>
                      </div>
                    )}

                    {!isSourceLoading && sourceLoadError.length > 0 && (
                      <p className="p-3 text-sm text-destructive">{sourceLoadError}</p>
                    )}

                    {!isSourceLoading &&
                      sourceLoadError.length === 0 &&
                      isCsvMimeType(currentStep.sourceMimeType) && (
                        <div className="max-h-130 w-full overflow-y-auto">{csvSourceContent}</div>
                      )}

                    {!isSourceLoading &&
                      sourceLoadError.length === 0 &&
                      sourceTextContent != null &&
                      annotationProjectType !== 'NER' && (
                        <pre className="max-h-130 overflow-auto whitespace-pre-wrap text-sm leading-relaxed text-foreground/90">
                          {sourceTextContent}
                        </pre>
                      )}

                    {!isSourceLoading &&
                      sourceLoadError.length === 0 &&
                      sourceUrl != null &&
                      sourceMimeType.toLowerCase().startsWith('image/') && (
                        <div className="flex h-full min-h-95 items-center justify-center overflow-auto">
                          <img
                            alt={t('project.annotationPage.sourceImageAlt')}
                            className="max-h-130 w-auto rounded-md"
                            src={sourceUrl}
                          />
                        </div>
                      )}

                    {!isSourceLoading &&
                      sourceLoadError.length === 0 &&
                      sourceUrl != null &&
                      sourceMimeType.toLowerCase().includes('pdf') && (
                        <iframe
                          className="h-140 w-full rounded-md border border-border bg-background"
                          src={sourceUrl}
                          title={sourceFileName ?? currentStep.sourceName}
                        />
                      )}

                    {!isSourceLoading &&
                      sourceLoadError.length === 0 &&
                      !isCsvMimeType(currentStep.sourceMimeType) &&
                      annotationProjectType === 'NER' &&
                      nerSourceText.length > 0 && (
                        <div className="mt-4 space-y-2">
                          <div
                            aria-label={t('project.annotationPage.nerSelectionAreaLabel')}
                            className="max-h-130 overflow-auto rounded-lg border border-border/70 bg-background p-3 text-sm leading-relaxed text-foreground/90 whitespace-pre-wrap select-text"
                            ref={nerSourceSelectionRef}
                          >
                            {nerTextSegments.map((segment) => {
                              if (segment.entities.length === 0) {
                                return <span key={segment.key}>{segment.text}</span>;
                              }

                              const segEnd = Number.parseInt(segment.key.split('-')[2], 10);

                              const nestedMarks = segment.entities.reduce(
                                (acc, entity) => (
                                  <mark
                                    className="rounded-xs px-px text-current"
                                    key={`${entity.label}-${entity.startOffset}`}
                                    style={getNerEntityStyle(entity.label, nerLabelColorMap)}
                                    title={entity.label}
                                  >
                                    {acc}
                                  </mark>
                                ),
                                <>{segment.text}</>,
                              );

                              return (
                                <span className="group/ner relative inline" key={segment.key}>
                                  {nestedMarks}
                                  {!isReviewMode &&
                                    segment.entities
                                      .filter((entity) => entity.endOffset === segEnd)
                                      .map((entity, index) => (
                                        <button
                                          aria-label={t('project.annotationPage.removeEntity')}
                                          className="absolute -top-2 -right-2 inline-flex size-4 items-center justify-center rounded-full border border-border bg-background text-muted-foreground opacity-0 shadow-sm transition-opacity hover:text-destructive group-hover/ner:opacity-100"
                                          key={`${entity.label}-${entity.startOffset}`}
                                          onClick={() => removeNerEntity(currentStep, entity)}
                                          onMouseDown={(event) => {
                                            event.preventDefault();
                                            event.stopPropagation();
                                          }}
                                          style={{
                                            transform:
                                              index > 0 ? `translateX(${index * 1}rem)` : undefined,
                                          }}
                                          type="button"
                                        >
                                          <X aria-hidden className="size-3" />
                                        </button>
                                      ))}
                                </span>
                              );
                            })}
                          </div>
                        </div>
                      )}

                    {!isSourceLoading &&
                      sourceLoadError.length === 0 &&
                      sourceUrl == null &&
                      sourceTextContent == null &&
                      annotationProjectType !== 'NER' &&
                      !isCsvMimeType(currentStep.sourceMimeType) && (
                        <div className="space-y-2 p-3">
                          <p className="text-sm text-muted-foreground">
                            {t('project.annotationPage.sourceNotSupported')}
                          </p>
                          <p className="rounded-md border border-border bg-background p-3 text-sm text-foreground/90 wrap-break-word">
                            {currentStep.preview}
                          </p>
                        </div>
                      )}
                  </div>
                </section>
              </div>

              <aside className="space-y-4 rounded-xl border border-border bg-surface-base p-5 sm:p-6">
                <section>
                  {project && (
                    <h1 className="mb-4 text-md font-bold tracking-wider text-muted-foreground uppercase">
                      {t(projectTypeI18nKey(project.projectType))}
                    </h1>
                  )}
                  <h3 className="flex items-center gap-2 text-sm font-bold tracking-[0.12em] text-muted-foreground uppercase">
                    <Tag className="size-4" />
                    {classificationHeading}
                  </h3>

                  <div className="mt-4 space-y-3">
                    {annotationProjectType === 'TEXT_CLASSIFICATION_SIMPLE' &&
                      labels.length > 0 && (
                        <select
                          className="h-10 w-full rounded-md border border-input bg-surface-base px-3 text-sm"
                          disabled={isReviewMode}
                          onChange={(event) =>
                            updateDraft(currentStep, { value: event.currentTarget.value })
                          }
                          value={currentDraft.value}
                        >
                          <option value="">
                            {t('project.annotationPage.fields.selectLabelPlaceholder')}
                          </option>
                          {labels.map((label) => (
                            <option key={label.name} value={label.name}>
                              {label.name}
                            </option>
                          ))}
                        </select>
                      )}

                    {annotationProjectType === 'TEXT_CLASSIFICATION_MULTILABEL' &&
                      labels.length > 0 && (
                        <div className="grid gap-2">
                          {labels.map((label) => {
                            const selected = selectedLabels.includes(label.name);

                            return (
                              <button
                                className={[
                                  'flex w-full items-center justify-between rounded-md border px-3 py-2 text-left text-sm font-medium transition',
                                  selected
                                    ? 'bg-primary/8 shadow-[inset_0_0_0_1px_rgba(0,0,0,0.03)]'
                                    : 'hover:bg-background',
                                  isReviewMode ? 'cursor-default opacity-80' : 'cursor-pointer',
                                ].join(' ')}
                                disabled={isReviewMode}
                                key={label.name}
                                onClick={() => toggleLabel(currentStep, label.name)}
                                style={getLabelStyle(label, selected)}
                                type="button"
                              >
                                <span className="truncate">{label.name}</span>
                                {selected && <CheckCircle2 aria-hidden className="size-4" />}
                              </button>
                            );
                          })}
                        </div>
                      )}

                    {annotationProjectType === 'NER' && labels.length > 0 && (
                      <div className="space-y-3">
                        <div className="grid gap-2">
                          {labels.map((label) => {
                            const selected = activeNerLabel === label.name;

                            return (
                              <button
                                className={[
                                  'flex w-full items-center justify-between rounded-md border px-3 py-2 text-left text-sm font-medium transition',
                                  selected
                                    ? 'bg-primary/8 shadow-[inset_0_0_0_1px_rgba(0,0,0,0.03)]'
                                    : 'hover:bg-transparent',
                                  isReviewMode ? 'cursor-default opacity-80' : 'cursor-pointer',
                                ].join(' ')}
                                disabled={isReviewMode}
                                key={label.name}
                                onClick={() => setActiveNerLabel(label.name)}
                                style={getLabelStyle(label, selected)}
                                type="button"
                              >
                                <span className="truncate">{label.name}</span>
                                {selected && <CheckCircle2 aria-hidden className="size-4" />}
                              </button>
                            );
                          })}
                        </div>
                      </div>
                    )}

                    {annotationProjectType === 'SEQ2SEQ' && (
                      <Textarea
                        disabled={isReviewMode}
                        onChange={(event) =>
                          updateDraft(currentStep, { value: event.currentTarget.value })
                        }
                        placeholder={t('project.annotationPage.fields.annotationPlaceholder')}
                        readOnly={isReviewMode}
                        rows={5}
                        value={currentDraft.value}
                      />
                    )}

                    {annotationProjectType === 'TEXT_CLASSIFICATION_SIMPLE' &&
                      labels.length === 0 && (
                        <Input
                          disabled={isReviewMode}
                          onChange={(event) =>
                            updateDraft(currentStep, { value: event.currentTarget.value })
                          }
                          placeholder={t('project.annotationPage.fields.selectLabelPlaceholder')}
                          readOnly={isReviewMode}
                          value={currentDraft.value}
                        />
                      )}

                    {annotationProjectType === 'TEXT_CLASSIFICATION_MULTILABEL' &&
                      labels.length === 0 && (
                        <Input
                          disabled={isReviewMode}
                          onChange={(event) =>
                            updateDraft(currentStep, { value: event.currentTarget.value })
                          }
                          placeholder={t('project.annotationPage.fields.labelsPlaceholder')}
                          readOnly={isReviewMode}
                          value={currentDraft.value}
                        />
                      )}

                    {annotationProjectType === 'NER' && labels.length === 0 && (
                      <p className="rounded-md border border-dashed border-border bg-background px-3 py-2 text-sm text-muted-foreground">
                        {t('project.annotationPage.nerNoLabels')}
                      </p>
                    )}
                  </div>
                </section>

                <section>
                  <h3 className="text-sm font-bold tracking-[0.12em] text-muted-foreground uppercase">
                    {t('project.annotationPage.notesTitle')}
                  </h3>
                  <Textarea
                    className="mt-3"
                    disabled={isReviewMode}
                    onChange={(event) =>
                      updateDraft(currentStep, { notes: event.currentTarget.value })
                    }
                    placeholder={t('project.annotationPage.fields.notesPlaceholder')}
                    readOnly={isReviewMode}
                    rows={6}
                    value={currentDraft.notes}
                  />
                </section>
              </aside>
            </div>
          )}
        </div>
      )}

      {hasRenderableStep && canRenderWorkspace && (
        <div className="fixed inset-x-0 bottom-0 z-20 border-t border-border bg-surface-base/95 backdrop-blur-sm">
          <div className="mx-auto flex w-full max-w-7xl items-center justify-end gap-2 px-4 py-3 sm:px-6">
            {!isFirstStep && (
              <Button
                disabled={!canMovePrevious || areStepActionsDisabled}
                onClick={handlePreviousAction}
                type="button"
                className="h-10 rounded-md border bg-surface-base px-6 text-sm font-semibold text-primary hover:bg-accent cursor-pointer"
                variant="outline"
              >
                <ArrowLeft className="size-4" />
                {t('project.annotationPage.previous')}
              </Button>
            )}

            {isLastStep ? (
              <Button
                disabled={areStepActionsDisabled}
                onClick={handleFinishAction}
                type="button"
                className="h-10 rounded-md bg-primary px-6 text-sm font-semibold text-white transition-colors hover:bg-primary-strong disabled:bg-secondary cursor-pointer"
              >
                {isSavingCurrentStep && <Spinner aria-hidden className="size-4" />}
                {t('project.annotationPage.finish')}
              </Button>
            ) : (
              <Button
                disabled={!canMoveNext || areStepActionsDisabled}
                onClick={handleNextAction}
                type="button"
                className="h-10 rounded-md bg-primary px-6 text-sm font-semibold text-white transition-colors hover:bg-primary-strong disabled:bg-secondary cursor-pointer"
              >
                {isSavingCurrentStep && <Spinner aria-hidden className="size-4" />}
                {t('project.annotationPage.next')}
                <ArrowRight className="size-4" />
              </Button>
            )}
          </div>
        </div>
      )}
    </PageContainer>
  );
}
