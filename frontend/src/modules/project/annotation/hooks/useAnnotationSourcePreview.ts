import { useEffect, useRef, useState } from 'react';
import {
  getProjectDatasetItemContent,
  getProjectDatasetItemContentErrorMessage,
} from '@/modules/project/shared/services/projectService';
import { type AnnotationStep } from '@/modules/project/shared/types/project';
import {
  createCachedSourcePreviewState,
  createEmptySourcePreviewState,
  createSourceCacheEntry,
  createStepSourcePreviewState,
  revokeSourceCache,
  revokeSourceCacheEntry,
  shouldFetchSourcePreview,
  type SourceCacheEntry,
  type SourcePreviewState,
} from '@/modules/project/annotation/utils/annotationSourcePreviewUtils';

export function useAnnotationSourcePreview(
  currentStep: AnnotationStep | null,
  numericProjectId: number,
): SourcePreviewState {
  const [sourceState, setSourceState] = useState<SourcePreviewState>(createEmptySourcePreviewState);
  const sourceCacheRef = useRef<Map<number, SourceCacheEntry>>(new Map());

  useEffect(() => {
    if (!currentStep) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setSourceState(createEmptySourcePreviewState());
      return;
    }

    if (!shouldFetchSourcePreview(currentStep)) {
      setSourceState(createStepSourcePreviewState(currentStep));
      return;
    }

    const cachedSource = sourceCacheRef.current.get(currentStep.datasetItemId);
    if (cachedSource) {
      setSourceState(createCachedSourcePreviewState(cachedSource, currentStep.sourceName));
      return;
    }

    let cancelled = false;
    setSourceState(createStepSourcePreviewState(currentStep, { isSourceLoading: true }));

    getProjectDatasetItemContent(numericProjectId, currentStep.datasetItemId)
      .then((content) => createSourceCacheEntry(content, currentStep.sourceName))
      .then((source) => {
        const nextState = createCachedSourcePreviewState(source, currentStep.sourceName);

        if (cancelled) {
          revokeSourceCacheEntry(source);
          return;
        }

        sourceCacheRef.current.set(currentStep.datasetItemId, source);
        setSourceState(nextState);
      })
      .catch((error) => {
        if (cancelled) {
          return;
        }

        setSourceState(
          createStepSourcePreviewState(currentStep, {
            sourceLoadError: getProjectDatasetItemContentErrorMessage(error),
          }),
        );
      });

    return () => {
      cancelled = true;
    };
  }, [currentStep, numericProjectId]);

  useEffect(() => {
    const sourceCache = sourceCacheRef.current;

    return () => {
      revokeSourceCache(sourceCache);
    };
  }, []);

  return sourceState;
}
