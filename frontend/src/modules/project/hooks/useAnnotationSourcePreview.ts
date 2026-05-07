import { useEffect, useRef, useState } from 'react';
import {
  getProjectDatasetItemContent,
  getProjectDatasetItemContentErrorMessage,
} from '@/modules/project/services/projectService';
import { type AnnotationStep } from '@/modules/project/types/project';
import {
  formatTextSourceContent,
  isCsvMimeType,
  isInlineSourceMimeType,
  isTextSourceMimeType,
  type SourceCacheEntry,
} from '@/modules/project/utils/annotationPageUtils';

type SourcePreviewState = {
  isSourceLoading: boolean;
  sourceUrl: string | null;
  sourceTextContent: string | null;
  sourceMimeType: string;
  sourceFileName: string | null;
  sourceLoadError: string;
};

export function useAnnotationSourcePreview(
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
    // eslint-disable-next-line react-hooks/set-state-in-effect
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
    const sourceCache = sourceCacheRef.current;

    return () => {
      sourceCache.forEach((source) => {
        if (source.url != null) {
          URL.revokeObjectURL(source.url);
        }
      });
      sourceCache.clear();
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
