import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';
import {
  useResolveOwnProjectAnnotationWarningMutation,
  useSaveProjectAnnotationStepMutation,
  useToggleProjectAnnotationWarningMutation,
} from '@/modules/project/shared/hooks/useProjectQueries';
import { getProjectAnnotationSaveErrorMessage } from '@/modules/project/shared/services/projectService';
import type { AnnotationStep, ProjectType } from '@/modules/project/shared/types/project';
import {
  ANNOTATION_PAGE_SIZE,
  annotationStepKey,
  buildAnnotationPayload,
  type AnnotationDraft,
  type PersistCurrentStepResult,
} from '@/modules/project/annotation/utils/annotationPageUtils';

type UseAnnotationPersistenceParams = Readonly<{
  annotationOffset: number;
  annotationProjectType: ProjectType;
  currentStep: AnnotationStep | null;
  getDraft: (step: AnnotationStep) => AnnotationDraft;
  isReviewMode: boolean;
  numericProjectId: number;
  reviewedParticipantUserId: number | null;
}>;

type UseAnnotationPersistenceResult = Readonly<{
  handleBackNavigation: () => Promise<boolean>;
  handleToggleWarning: () => Promise<void>;
  isSavingCurrentStep: boolean;
  isStepSavePending: boolean;
  isWarningUpdating: boolean;
  runAfterPersist: (onSuccess: () => void) => Promise<void>;
}>;

export function useAnnotationPersistence({
  annotationOffset,
  annotationProjectType,
  currentStep,
  getDraft,
  isReviewMode,
  numericProjectId,
  reviewedParticipantUserId,
}: UseAnnotationPersistenceParams): UseAnnotationPersistenceResult {
  const { t } = useTranslation();
  const [savingStepId, setSavingStepId] = useState<string | null>(null);
  const saveProjectAnnotationStepMutation = useSaveProjectAnnotationStepMutation();
  const toggleProjectAnnotationWarningMutation = useToggleProjectAnnotationWarningMutation(
    annotationOffset,
    ANNOTATION_PAGE_SIZE,
  );
  const resolveOwnProjectAnnotationWarningMutation = useResolveOwnProjectAnnotationWarningMutation(
    annotationOffset,
    ANNOTATION_PAGE_SIZE,
  );

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

  const handleToggleWarning = async () => {
    if (!currentStep) {
      return;
    }

    const wasWarningActive = currentStep.warning;

    try {
      if (isReviewMode && reviewedParticipantUserId != null) {
        await toggleProjectAnnotationWarningMutation.mutateAsync({
          projectId: numericProjectId,
          participantUserId: reviewedParticipantUserId,
          datasetItemId: currentStep.datasetItemId,
          stepIndex: currentStep.stepIndex,
        });
        toast.success(
          t(
            wasWarningActive
              ? 'project.annotationPage.warningCleared'
              : 'project.annotationPage.warningMarked',
          ),
        );
        return;
      }

      if (currentStep.warning) {
        await resolveOwnProjectAnnotationWarningMutation.mutateAsync({
          projectId: numericProjectId,
          datasetItemId: currentStep.datasetItemId,
          stepIndex: currentStep.stepIndex,
        });
        toast.success(t('project.annotationPage.warningResolved'));
      }
    } catch {
      toast.error(t('project.annotationPage.errors.warningToggleFailed'));
    }
  };

  const currentStepId = currentStep ? annotationStepKey(currentStep) : null;

  return {
    handleBackNavigation,
    handleToggleWarning,
    isSavingCurrentStep:
      saveProjectAnnotationStepMutation.isPending && savingStepId === currentStepId,
    isStepSavePending: saveProjectAnnotationStepMutation.isPending,
    isWarningUpdating:
      toggleProjectAnnotationWarningMutation.isPending ||
      resolveOwnProjectAnnotationWarningMutation.isPending,
    runAfterPersist,
  };
}
