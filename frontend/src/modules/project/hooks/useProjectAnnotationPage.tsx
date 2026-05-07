import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { X } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams, useSearchParams } from 'react-router';
import { toast } from 'sonner';
import { useAnnotationSourcePreview } from '@/modules/project/hooks/useAnnotationSourcePreview';
import { useAnnotationStepCursor } from '@/modules/project/hooks/useAnnotationStepCursor';
import {
  useProjectAnnotationWorkspaceQuery,
  useProjectDetailQuery,
  useProjectParticipantAnnotationWorkspaceQuery,
  useSaveProjectAnnotationStepMutation,
  useToggleProjectAnnotationWarningMutation,
} from '@/modules/project/hooks/useProjectQueries';
import {
  getProjectAnnotationLoadErrorMessage,
  getProjectAnnotationSaveErrorMessage,
  getProjectDetailLoadErrorMessage,
} from '@/modules/project/services/projectService';
import type { AnnotationStep } from '@/modules/project/types/project';
import {
  ANNOTATION_PAGE_SIZE,
  annotationStepKey,
  annotationValueToEditorValue,
  annotationValueToNerEntities,
  annotationValueToNotes,
  buildAnnotationPayload,
  buildNerTextSegments,
  clearTextSelection,
  findCsvColumnValue,
  getNerEntityStyle,
  getNerSourceText,
  isCsvMimeType,
  mergeNerEntity,
  normalizeStepIndex,
  parseCommaSeparatedLabels,
  parseCsvStepPreview,
  selectionOffsetsWithinElement,
  type AnnotationDraft,
  type AnnotationDraftByStep,
  type CsvLabelColumnValue,
  type NerAnnotationEntity,
  type PersistCurrentStepResult,
} from '@/modules/project/utils/annotationPageUtils';
import {
  completionColor,
  normalizeCompletionPercentage,
} from '@/modules/project/utils/projectUtils';

export function useProjectAnnotationPage() {
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
  const labels = useMemo(
    () => annotationWorkspace?.labels ?? project?.labels ?? [],
    [annotationWorkspace?.labels, project?.labels],
  );

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

  const getDraft = useCallback(
    (step: AnnotationStep): AnnotationDraft => {
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
    },
    [annotationProjectType, draftByStep],
  );

  const updateDraft = useCallback(
    (step: AnnotationStep, patch: Partial<AnnotationDraft>) => {
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
    },
    [getDraft, isReviewMode],
  );

  const toggleLabel = useCallback(
    (step: AnnotationStep, labelName: string) => {
      const currentDraft = getDraft(step);
      const selectedLabels = parseCommaSeparatedLabels(currentDraft.value);
      const hasLabel = selectedLabels.includes(labelName);

      const nextLabels = hasLabel
        ? selectedLabels.filter((existingLabel) => existingLabel !== labelName)
        : [...selectedLabels, labelName];

      updateDraft(step, { value: nextLabels.join(', ') });
    },
    [getDraft, updateDraft],
  );

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
    } catch {
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

  return {
    activeNerLabel,
    annotationProjectType,
    annotationWorkspace,
    areStepActionsDisabled,
    canMoveNext,
    canMovePrevious,
    canRenderWorkspace,
    classificationHeading,
    completionColors,
    completionPercentage,
    csvSourceContent,
    currentDraft,
    currentGlobalStepIndex,
    currentStep,
    guidelineContent,
    handleBackNavigation,
    handleFinishAction,
    handleNextAction,
    handlePreviousAction,
    handleToggleWarning,
    hasRenderableStep,
    isAnnotationWorkspaceLoading,
    isFirstStep,
    isGuidelineCollapsed,
    isInvalidProjectId,
    isLastStep,
    isProjectLoading,
    isReviewMode,
    isSavingCurrentStep,
    isSourceLoading,
    isWarningUpdating: toggleProjectAnnotationWarningMutation.isPending,
    labels,
    nerLabelColorMap,
    nerSourceSelectionRef,
    nerSourceText,
    nerTextSegments,
    numericProjectId,
    project,
    removeNerEntity,
    reviewedParticipantLabel,
    reviewedParticipantUserId,
    selectedLabels,
    setActiveNerLabel,
    setIsGuidelineCollapsed,
    shouldShowHeaderProgress,
    sourceFileName,
    sourceLoadError,
    sourceMimeType,
    sourceTextContent,
    sourceUrl,
    totalSteps,
    toggleLabel,
    updateDraft,
  };
}

export type ProjectAnnotationPageState = ReturnType<typeof useProjectAnnotationPage>;
