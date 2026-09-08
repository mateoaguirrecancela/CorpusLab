import { extractApiErrorMessage } from '@/shared/api/apiErrors';
import i18n from '@/app/config/i18n';

export {
  archiveProject,
  cleanupIncompleteProject,
  createProject,
  deleteProject,
  getAssignedProjectsByGroup,
  getMyAssignedProjects,
  getProjectDetail,
  unarchiveProject,
  updateProject,
} from '@/modules/project/core/services/projectCoreService';
export {
  getProjectDatasetItemContent,
  uploadProjectDataset,
} from '@/modules/project/dataset/services/projectDatasetService';
export {
  configureProjectSetup,
  getProjectGuidelinePdf,
} from '@/modules/project/setup/services/projectSetupService';
export {
  assignProjectParticipants,
  getProjectAssignmentContext,
} from '@/modules/project/participants/services/projectParticipantService';
export { getProjectMetrics } from '@/modules/project/metrics/services/projectMetricsService';
export {
  getProjectAnnotationWorkspace,
  getProjectParticipantAnnotationWorkspace,
  resolveOwnProjectAnnotationWarning,
  saveProjectAnnotationStep,
  toggleProjectAnnotationWarning,
} from '@/modules/project/annotation/services/projectAnnotationService';
export { exportProjectAnnotationResultsCsv } from '@/modules/project/exports/services/projectExportService';

function projectErrorMessage(translationKey: string) {
  return (error: unknown): string => extractApiErrorMessage(error, i18n.t(translationKey));
}

export const getCreateProjectErrorMessage = projectErrorMessage('project.errors.createFailed');
export const getUpdateProjectErrorMessage = projectErrorMessage('project.errors.updateFailed');
export const getDeleteProjectErrorMessage = projectErrorMessage('project.errors.deleteFailed');
export const getArchiveProjectErrorMessage = projectErrorMessage('project.errors.archiveFailed');
export const getUnarchiveProjectErrorMessage = projectErrorMessage(
  'project.errors.unarchiveFailed',
);
export const getUploadDatasetErrorMessage = projectErrorMessage(
  'project.errors.datasetUploadFailed',
);
export const getProjectSetupErrorMessage = projectErrorMessage('project.errors.setupFailed');
export const getAssignParticipantsErrorMessage = projectErrorMessage(
  'project.errors.assignmentFailed',
);
export const getProjectsLoadErrorMessage = projectErrorMessage('project.errors.loadFailed');
export const getProjectDetailLoadErrorMessage = projectErrorMessage(
  'project.errors.detailLoadFailed',
);
export const getProjectAssignmentContextErrorMessage = projectErrorMessage(
  'project.errors.assignmentContextLoadFailed',
);
export const getProjectGuidelinePdfErrorMessage = projectErrorMessage(
  'project.errors.guidelinePdfLoadFailed',
);
export const getProjectMetricsLoadErrorMessage = projectErrorMessage(
  'project.errors.metricsLoadFailed',
);
export const getProjectAnnotationLoadErrorMessage = projectErrorMessage(
  'project.errors.annotationLoadFailed',
);
export const getProjectAnnotationSaveErrorMessage = projectErrorMessage(
  'project.errors.annotationSaveFailed',
);
export const getProjectAnnotationExportErrorMessage = projectErrorMessage(
  'project.errors.annotationExportFailed',
);
export const getProjectDatasetItemContentErrorMessage = projectErrorMessage(
  'project.errors.sourceContentLoadFailed',
);
