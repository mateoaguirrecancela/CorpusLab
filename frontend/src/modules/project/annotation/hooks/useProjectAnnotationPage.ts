import { useEffect, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams, useSearchParams } from 'react-router';
import { useAnnotationDraft } from '@/modules/project/annotation/hooks/useAnnotationDraft';
import { useAnnotationNavigation } from '@/modules/project/annotation/hooks/useAnnotationNavigation';
import { useAnnotationPersistence } from '@/modules/project/annotation/hooks/useAnnotationPersistence';
import { useAnnotationWorkspace } from '@/modules/project/annotation/hooks/useAnnotationWorkspace';
import { useGuidelinePdf } from '@/modules/project/annotation/hooks/useGuidelinePdf';
import { useNerSelection } from '@/modules/project/annotation/hooks/useNerSelection';
import { useAnnotationSourcePreview } from '@/modules/project/annotation/hooks/useAnnotationSourcePreview';
import { useAnnotationUIStore } from '@/modules/project/annotation/stores/useAnnotationUIStore';
import {
  findCsvColumnValue,
  isCsvMimeType,
  type CsvLabelColumnValue,
} from '@/modules/project/annotation/utils/annotationPageUtils';
import { isPositiveId } from '@/modules/project/shared/utils/projectFormUtils';
import {
  completionColor,
  normalizeCompletionPercentage,
} from '@/modules/project/shared/utils/projectUtils';

export function useProjectAnnotationPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { projectId } = useParams();
  const [searchParams] = useSearchParams();

  const numericProjectId = useMemo(() => Number(projectId), [projectId]);
  const isInvalidProjectId = !isPositiveId(numericProjectId);
  const reviewedParticipantUserId = useMemo(() => {
    const rawParticipantUserId = searchParams.get('participantUserId');
    if (!rawParticipantUserId) {
      return null;
    }

    const parsedParticipantUserId = Number(rawParticipantUserId);
    if (!Number.isInteger(parsedParticipantUserId) || !isPositiveId(parsedParticipantUserId)) {
      return null;
    }

    return parsedParticipantUserId;
  }, [searchParams]);
  const isReviewMode = reviewedParticipantUserId != null;

  const {
    activeNerLabel,
    annotationOffset,
    hasResolvedResumeStep,
    isGuidelineCollapsed,
    resetAnnotationUI,
    resumeGlobalStepIndex,
    setActiveNerLabel,
    setAnnotationOffset,
    setHasResolvedResumeStep,
    setIsGuidelineCollapsed,
    setResumeGlobalStepIndex,
  } = useAnnotationUIStore();

  const {
    annotationProjectType,
    annotationTargetColumn,
    annotationWorkspace,
    canRenderWorkspace,
    isAnnotationWorkspaceLoading,
    isProjectLoading,
    labels,
    project,
    reviewedParticipant,
    totalSteps,
  } = useAnnotationWorkspace({
    annotationOffset,
    isInvalidProjectId,
    isReviewMode,
    numericProjectId,
    reviewedParticipantUserId,
  });

  const {
    canMoveNext,
    canMovePrevious,
    currentGlobalStepIndex,
    currentStep,
    goToNextStep,
    goToPreviousStep,
    isFirstStep,
    isLastStep,
  } = useAnnotationNavigation({
    annotationOffset,
    annotationWorkspace,
    hasResolvedResumeStep,
    isInvalidProjectId,
    isReviewMode,
    resumeGlobalStepIndex,
    setAnnotationOffset,
    setHasResolvedResumeStep,
    setResumeGlobalStepIndex,
  });

  const {
    isSourceLoading,
    sourceUrl,
    sourceTextContent,
    sourceMimeType,
    sourceFileName,
    sourceLoadError,
  } = useAnnotationSourcePreview(currentStep, numericProjectId);

  const { currentDraft, getDraft, resetDrafts, selectedLabels, toggleLabel, updateDraft } =
    useAnnotationDraft(currentStep, {
      annotationProjectType,
      isReviewMode,
    });

  useEffect(() => {
    resetAnnotationUI();
    resetDrafts();
  }, [numericProjectId, resetAnnotationUI, resetDrafts, reviewedParticipantUserId]);

  const { guidelinePdfError, guidelinePdfUrl } = useGuidelinePdf(project);

  const {
    nerLabelColorMap,
    nerSourceSelectionRef,
    nerSourceText,
    nerTextSegments,
    removeNerEntity,
  } = useNerSelection({
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
  });

  const {
    handleBackNavigation,
    handleToggleWarning,
    isSavingCurrentStep,
    isStepSavePending,
    isWarningUpdating,
    runAfterPersist,
  } = useAnnotationPersistence({
    annotationOffset,
    annotationProjectType,
    currentStep,
    getDraft,
    isReviewMode,
    numericProjectId,
    reviewedParticipantUserId,
  });

  const handlePreviousAction = async () => {
    await runAfterPersist(goToPreviousStep);
  };

  const handleNextAction = async () => {
    await runAfterPersist(goToNextStep);
  };

  const handleFinishAction = async () => {
    await runAfterPersist(() => navigate(`/home/projects/${numericProjectId}`));
  };

  const completionPercentage = normalizeCompletionPercentage(
    annotationWorkspace?.completionPercentage ?? 0,
  );
  const completionColors = completionColor(completionPercentage);

  const csvTargetColumnValue = useMemo(() => {
    if (!currentStep || !isCsvMimeType(currentStep.sourceMimeType)) {
      return null;
    }

    const normalizedTarget = annotationTargetColumn?.trim().toLowerCase() ?? '';

    if (normalizedTarget.length > 0) {
      const targetValue = findCsvColumnValue(normalizedTarget, currentStep.rowValues);

      if (targetValue != null) {
        return targetValue;
      }
    }

    return currentStep.preview;
  }, [annotationTargetColumn, currentStep]);

  const csvLabelColumnValues = useMemo<CsvLabelColumnValue[]>(() => {
    if (!currentStep || !isCsvMimeType(currentStep.sourceMimeType)) {
      return [];
    }

    const normalizedTarget = annotationTargetColumn?.trim().toLowerCase() ?? '';

    return labels
      .map((label) => ({
        name: label.name,
        value: findCsvColumnValue(label.name, currentStep.rowValues),
      }))
      .filter((entry): entry is CsvLabelColumnValue => {
        const normalizedName = entry.name.trim().toLowerCase();

        return (
          normalizedName.length > 0 && normalizedName !== normalizedTarget && entry.value !== null
        );
      });
  }, [annotationTargetColumn, currentStep, labels]);

  const hasRenderableStep = currentStep != null;
  const areStepActionsDisabled =
    isAnnotationWorkspaceLoading || (!isReviewMode && isStepSavePending);
  const shouldShowHeaderProgress = !isInvalidProjectId && canRenderWorkspace;
  const reviewedParticipantLabel = reviewedParticipant
    ? `${reviewedParticipant.firstName} ${reviewedParticipant.lastName}`
    : null;
  const classificationHeading = isReviewMode
    ? t('project.annotationPage.classificationTitle')
    : `${t('project.annotationPage.classificationTitle')} *`;

  return {
    activeNerLabel,
    annotationProjectType,
    annotationTargetColumn,
    annotationWorkspace,
    areStepActionsDisabled,
    canMoveNext,
    canMovePrevious,
    canRenderWorkspace,
    classificationHeading,
    completionColors,
    completionPercentage,
    csvLabelColumnValues,
    csvTargetColumnValue,
    currentDraft,
    currentGlobalStepIndex,
    currentStep,
    guidelinePdfError,
    guidelinePdfUrl,
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
    isWarningUpdating,
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
