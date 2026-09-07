import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  createCachedSourcePreviewState,
  createEmptySourcePreviewState,
  createSourceCacheEntry,
  createStepSourcePreviewState,
  revokeSourceCache,
  revokeSourceCacheEntry,
  shouldFetchSourcePreview,
  type SourceCacheEntry,
} from '@/modules/project/annotation/utils/annotationSourcePreviewUtils';
import {
  type AnnotationStep,
  type ProjectDatasetItemContent,
} from '@/modules/project/shared/types/project';

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

describe('createEmptySourcePreviewState / createStepSourcePreviewState', () => {
  it('creates a blank state', () => {
    expect(createEmptySourcePreviewState()).toEqual({
      isSourceLoading: false,
      sourceFileName: null,
      sourceLoadError: '',
      sourceMimeType: '',
      sourceTextContent: null,
      sourceUrl: null,
    });
  });

  it('seeds file name and mime type from the step, applying overrides', () => {
    const state = createStepSourcePreviewState(
      step({ sourceName: 'a.png', sourceMimeType: 'image/png' }),
      {
        isSourceLoading: true,
      },
    );

    expect(state.sourceFileName).toBe('a.png');
    expect(state.sourceMimeType).toBe('image/png');
    expect(state.isSourceLoading).toBe(true);
  });
});

describe('shouldFetchSourcePreview', () => {
  it('is false for csv steps', () => {
    expect(shouldFetchSourcePreview(step({ sourceMimeType: 'text/csv' }))).toBe(false);
  });

  it('is true for inline mime types (images/pdf)', () => {
    expect(shouldFetchSourcePreview(step({ sourceMimeType: 'image/png' }))).toBe(true);
  });

  it('is true for text-like sources', () => {
    expect(shouldFetchSourcePreview(step({ sourceMimeType: 'text/plain' }))).toBe(true);
  });

  it('is false for unrelated binary mime types', () => {
    expect(
      shouldFetchSourcePreview(
        step({ sourceMimeType: 'application/octet-stream', sourceName: 'a.bin' }),
      ),
    ).toBe(false);
  });
});

describe('createCachedSourcePreviewState', () => {
  it('uses the cached file name and falls back when missing', () => {
    const source: SourceCacheEntry = {
      url: 'blob:1',
      textContent: null,
      mimeType: 'image/png',
      fileName: null,
    };

    expect(createCachedSourcePreviewState(source, 'fallback.png').sourceFileName).toBe(
      'fallback.png',
    );
  });
});

describe('createSourceCacheEntry / revokeSourceCacheEntry / revokeSourceCache', () => {
  beforeEach(() => {
    vi.stubGlobal('URL', {
      ...URL,
      createObjectURL: vi.fn(() => 'blob:mock-url'),
      revokeObjectURL: vi.fn(),
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('reads text content for text mime types and formats json', async () => {
    const content: ProjectDatasetItemContent = {
      blob: new Blob(['{"a":1}'], { type: 'application/json' }),
      mimeType: 'application/json',
      fileName: 'data.json',
    };

    const entry = await createSourceCacheEntry(content, 'fallback.json');

    expect(entry.url).toBeNull();
    expect(entry.textContent).toBe('{\n  "a": 1\n}');
    expect(entry.fileName).toBe('data.json');
  });

  it('falls back to the provided file name when the content has none', async () => {
    const content: ProjectDatasetItemContent = {
      blob: new Blob(['hello'], { type: 'text/plain' }),
      mimeType: 'text/plain',
      fileName: null,
    };

    const entry = await createSourceCacheEntry(content, 'fallback.txt');
    expect(entry.fileName).toBe('fallback.txt');
  });

  it('creates an object URL for non-text mime types', async () => {
    const content: ProjectDatasetItemContent = {
      blob: new Blob(['binary'], { type: 'image/png' }),
      mimeType: 'image/png',
      fileName: 'photo.png',
    };

    const entry = await createSourceCacheEntry(content, 'fallback.png');
    expect(entry.url).toBe('blob:mock-url');
    expect(entry.textContent).toBeNull();
    expect(URL.createObjectURL).toHaveBeenCalledWith(content.blob);
  });

  it('revokes the object URL only when one is present', () => {
    revokeSourceCacheEntry({
      url: 'blob:mock-url',
      textContent: null,
      mimeType: 'image/png',
      fileName: null,
    });
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:mock-url');

    vi.mocked(URL.revokeObjectURL).mockClear();
    revokeSourceCacheEntry({
      url: null,
      textContent: 'text',
      mimeType: 'text/plain',
      fileName: null,
    });
    expect(URL.revokeObjectURL).not.toHaveBeenCalled();
  });

  it('revokes every cached entry and clears the cache', () => {
    const cache = new Map<number, SourceCacheEntry>([
      [1, { url: 'blob:1', textContent: null, mimeType: 'image/png', fileName: null }],
      [2, { url: null, textContent: 'text', mimeType: 'text/plain', fileName: null }],
    ]);

    revokeSourceCache(cache);

    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:1');
    expect(cache.size).toBe(0);
  });
});
