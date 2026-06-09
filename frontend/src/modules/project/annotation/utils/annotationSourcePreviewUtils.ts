import {
  type AnnotationStep,
  type ProjectDatasetItemContent,
} from '@/modules/project/shared/types/project';
import {
  formatTextSourceContent,
  isCsvMimeType,
  isInlineSourceMimeType,
  isTextSourceMimeType,
  type SourceCacheEntry,
} from '@/modules/project/annotation/utils/annotationPageUtils';

export type { SourceCacheEntry };

export type SourcePreviewState = {
  isSourceLoading: boolean;
  sourceUrl: string | null;
  sourceTextContent: string | null;
  sourceMimeType: string;
  sourceFileName: string | null;
  sourceLoadError: string;
};

const EMPTY_SOURCE_PREVIEW_STATE: SourcePreviewState = {
  isSourceLoading: false,
  sourceFileName: null,
  sourceLoadError: '',
  sourceMimeType: '',
  sourceTextContent: null,
  sourceUrl: null,
};

export function createEmptySourcePreviewState(): SourcePreviewState {
  return { ...EMPTY_SOURCE_PREVIEW_STATE };
}

export function createStepSourcePreviewState(
  step: AnnotationStep,
  overrides: Partial<SourcePreviewState> = {},
): SourcePreviewState {
  return {
    ...EMPTY_SOURCE_PREVIEW_STATE,
    sourceFileName: step.sourceName,
    sourceMimeType: step.sourceMimeType,
    ...overrides,
  };
}

export function shouldFetchSourcePreview(step: AnnotationStep): boolean {
  if (isCsvMimeType(step.sourceMimeType)) {
    return false;
  }

  return (
    isInlineSourceMimeType(step.sourceMimeType) ||
    isTextSourceMimeType(step.sourceMimeType, step.sourceName)
  );
}

export function createCachedSourcePreviewState(
  source: SourceCacheEntry,
  fallbackFileName: string,
): SourcePreviewState {
  return {
    isSourceLoading: false,
    sourceFileName: source.fileName ?? fallbackFileName,
    sourceLoadError: '',
    sourceMimeType: source.mimeType,
    sourceTextContent: source.textContent,
    sourceUrl: source.url,
  };
}

export async function createSourceCacheEntry(
  content: ProjectDatasetItemContent,
  fallbackFileName: string,
): Promise<SourceCacheEntry> {
  const resolvedMimeType = content.mimeType;
  const resolvedFileName = content.fileName ?? fallbackFileName;

  if (isTextSourceMimeType(resolvedMimeType, resolvedFileName)) {
    const rawTextContent = await content.blob.text();

    return {
      fileName: resolvedFileName,
      mimeType: resolvedMimeType,
      textContent: formatTextSourceContent(resolvedMimeType, resolvedFileName, rawTextContent),
      url: null,
    };
  }

  return {
    fileName: resolvedFileName,
    mimeType: resolvedMimeType,
    textContent: null,
    url: URL.createObjectURL(content.blob),
  };
}

export function revokeSourceCacheEntry(source: SourceCacheEntry): void {
  if (source.url != null) {
    URL.revokeObjectURL(source.url);
  }
}

export function revokeSourceCache(sourceCache: Map<number, SourceCacheEntry>): void {
  sourceCache.forEach(revokeSourceCacheEntry);
  sourceCache.clear();
}
