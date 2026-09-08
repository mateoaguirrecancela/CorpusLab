import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useToastMessages } from '@/shared/hooks/useToastMessages';
import {
  useProjectAnnotationWorkspaceQuery,
  useProjectDetailQuery,
  useProjectParticipantAnnotationWorkspaceQuery,
} from '@/modules/project/shared/hooks/useProjectQueries';
import {
  getProjectAnnotationLoadErrorMessage,
  getProjectDetailLoadErrorMessage,
} from '@/modules/project/shared/services/projectService';
import type {
  ProjectAnnotationWorkspace,
  ProjectDetail,
  ProjectDetailParticipant,
  ProjectSetupLabel,
  ProjectType,
} from '@/modules/project/shared/types/project';
import { ANNOTATION_PAGE_SIZE } from '@/modules/project/annotation/utils/annotationPageUtils';

type UseAnnotationWorkspaceParams = Readonly<{
  annotationOffset: number;
  isInvalidProjectId: boolean;
  isReviewMode: boolean;
  numericProjectId: number;
  reviewedParticipantUserId: number | null;
}>;

type UseAnnotationWorkspaceResult = Readonly<{
  annotationProjectType: ProjectType;
  annotationTargetColumn: string | null;
  annotationWorkspace: ProjectAnnotationWorkspace | undefined;
  canRenderWorkspace: boolean;
  detailErrorMessage: string;
  isAnnotationWorkspaceLoading: boolean;
  isProjectLoading: boolean;
  labels: ProjectSetupLabel[];
  project: ProjectDetail | undefined;
  reviewedParticipant: ProjectDetailParticipant | null;
  totalSteps: number;
}>;

export function useAnnotationWorkspace({
  annotationOffset,
  isInvalidProjectId,
  isReviewMode,
  numericProjectId,
  reviewedParticipantUserId,
}: UseAnnotationWorkspaceParams): UseAnnotationWorkspaceResult {
  const { t } = useTranslation();
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

  useToastMessages({
    errorMessage: detailErrorMessage,
    errorToastId: 'project-annotation-detail-load-error',
  });

  useToastMessages({
    errorMessage: annotationWorkspaceErrorMessage,
    errorToastId: 'project-annotation-workspace-load-error',
  });

  const annotationProjectType =
    annotationWorkspace?.projectType ?? project?.projectType ?? 'TEXT_CLASSIFICATION_SIMPLE';
  const annotationTargetColumn =
    annotationWorkspace?.annotationTargetColumn ?? project?.annotationTargetColumn ?? null;
  const labels = useMemo(
    () => annotationWorkspace?.labels ?? project?.labels ?? [],
    [annotationWorkspace?.labels, project?.labels],
  );
  const totalSteps = annotationWorkspace?.totalSteps ?? 0;
  const canRenderWorkspace =
    !isProjectLoading &&
    !isAnnotationWorkspaceLoading &&
    detailErrorMessage.length === 0 &&
    project != null &&
    annotationWorkspace != null;

  return {
    annotationProjectType,
    annotationTargetColumn,
    annotationWorkspace,
    canRenderWorkspace,
    detailErrorMessage,
    isAnnotationWorkspaceLoading,
    isProjectLoading,
    labels,
    project,
    reviewedParticipant,
    totalSteps,
  };
}
