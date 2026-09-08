import { useCallback, useEffect, useMemo, useRef, type RefObject } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';
import type {
  AnnotationStep,
  ProjectSetupLabel,
  ProjectType,
} from '@/modules/project/shared/types/project';
import {
  buildNerTextSegments,
  clearTextSelection,
  getNerSourceText,
  mergeNerEntity,
  selectionOffsetsWithinElement,
  type AnnotationDraft,
  type NerAnnotationEntity,
  type NerTextSegment,
} from '@/modules/project/annotation/utils/annotationPageUtils';

type UseNerSelectionParams = Readonly<{
  activeNerLabel: string;
  annotationProjectType: ProjectType;
  annotationTargetColumn: string | null;
  currentDraft: AnnotationDraft;
  currentStep: AnnotationStep | null;
  getDraft: (step: AnnotationStep) => AnnotationDraft;
  isReviewMode: boolean;
  labels: ProjectSetupLabel[];
  setActiveNerLabel: (nextLabel: string) => void;
  sourceTextContent: string | null;
  updateDraft: (step: AnnotationStep, patch: Partial<AnnotationDraft>) => void;
}>;

type UseNerSelectionResult = Readonly<{
  nerLabelColorMap: Map<string, string | null>;
  nerSourceSelectionRef: RefObject<HTMLDivElement | null>;
  nerSourceText: string;
  nerTextSegments: NerTextSegment[];
  removeNerEntity: (step: AnnotationStep | null, entityToRemove: NerAnnotationEntity) => void;
}>;

export function useNerSelection({
  activeNerLabel,
  annotationProjectType,
  annotationTargetColumn,
  currentDraft,
  currentStep,
  getDraft,
  isReviewMode,
  labels,
  setActiveNerLabel,
  sourceTextContent,
  updateDraft,
}: UseNerSelectionParams): UseNerSelectionResult {
  const { t } = useTranslation();
  const nerSourceSelectionRef = useRef<HTMLDivElement | null>(null);

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
  }, [activeNerLabel, annotationProjectType, labels, setActiveNerLabel]);

  const handleNerSourceSelection = useCallback(() => {
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
  }, [activeNerLabel, annotationProjectType, currentStep, getDraft, isReviewMode, t, updateDraft]);

  const removeNerEntity = useCallback(
    (step: AnnotationStep | null, entityToRemove: NerAnnotationEntity) => {
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
    },
    [getDraft, isReviewMode, updateDraft],
  );

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

  return {
    nerLabelColorMap,
    nerSourceSelectionRef,
    nerSourceText,
    nerTextSegments,
    removeNerEntity,
  };
}
