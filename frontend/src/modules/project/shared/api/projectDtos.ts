import type {
  ProjectMetricStatus,
  ProjectMetricType,
  ProjectParticipantAssignment,
  ProjectParticipantAssignmentGroup,
  ProjectParticipantRole,
  ProjectSetupLabel,
  ProjectType,
} from '@/modules/project/shared/types/projectPrimitives';

export type ProjectSummaryDto = {
  id: number;
  researchGroupId: number;
  name: string;
  description: string | null;
  createdAt: string;
};

export type ProjectAssignedSummaryDto = {
  id: number;
  researchGroupId: number;
  researchGroupName: string;
  name: string;
  description: string | null;
  completionPercentage: number;
  participantRole: ProjectParticipantRole;
  archived: boolean;
  createdAt: string;
};

export type DatasetItemDto = {
  id: number;
  index: number;
  fileName: string;
  mimeType: string;
  sizeBytes: number;
  createdAt: string;
};

export type ProjectAssignableMemberDto = {
  userId: number;
  firstName: string;
  lastName: string;
  email: string;
};

export type ProjectParticipantAssignmentDto = {
  userId: number;
  iaaGroup: ProjectParticipantAssignmentGroup;
};

export type ProjectAssignmentContextDto = {
  researchGroupId: number;
  currentUserId: number;
  members: ProjectAssignableMemberDto[];
  defaultAssignments: ProjectParticipantAssignmentDto[];
};

export type ProjectDetailParticipantDto = {
  userId: number;
  firstName: string;
  lastName: string;
  email: string;
  role: ProjectParticipantRole;
  iaaGroup: ProjectParticipantAssignmentGroup | null;
  completionPercentage: number;
};

export type ProjectDetailDto = {
  id: number;
  researchGroupId: number;
  researchGroupName: string;
  name: string;
  description: string | null;
  projectType: ProjectType;
  setupCompleted: boolean;
  completionPercentage: number;
  participantRole: ProjectParticipantRole;
  participants: ProjectDetailParticipantDto[];
  datasetItems: DatasetItemDto[];
  labels: ProjectSetupLabel[];
  guidelineText: string | null;
  guidelinePdfBase64?: string | null;
  guidelinePdfAvailable: boolean;
  guidelinePdfMimeType: string | null;
  guidelinePdfSizeBytes: number;
  annotationTargetColumn: string | null;
  datasetItemsCount: number;
  canManageProject: boolean;
  canArchiveProject: boolean;
  canExportAnnotations: boolean;
  archived: boolean;
  createdAt: string;
};

export type UploadProjectDatasetResponseDto = {
  projectId: number;
  uploadedItems: number;
  items: DatasetItemDto[];
};

export type UploadProjectDatasetJobDto = {
  jobId: string;
};

export type UploadProjectDatasetEventDto = {
  jobId: string;
  status: 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED';
  progress: number;
  message: string | null;
  result: UploadProjectDatasetResponseDto | null;
};

export type ProjectSetupResponseDto = {
  projectId: number;
  projectType: ProjectType;
  labels: ProjectSetupLabel[];
  guidelineText: string | null;
  annotationTargetColumn: string | null;
  setupCompleted: boolean;
};

export type ProjectMetricsDto = {
  projectId: number;
  metrics: ProjectMetricDto[];
};

export type ProjectMetricDto = {
  metricType: ProjectMetricType;
  projectType: ProjectType;
  value: number | null;
  calculable: boolean;
  status: ProjectMetricStatus;
  message: string | null;
  annotatorCount: number;
  itemCount: number;
  pairCount: number;
  details: Record<string, unknown>;
};

export type ProjectAnnotationStepDto = {
  datasetItemId: number;
  datasetItemIndex: number;
  stepIndex: number;
  totalStepsForItem: number;
  sourceName: string;
  sourceMimeType: string;
  preview: string;
  rowValues: Record<string, string> | null;
  completed: boolean;
  warning: boolean;
  annotation: unknown;
};

export type ProjectAnnotationWorkspaceDto = {
  projectId: number;
  projectType: ProjectType;
  annotationTargetColumn: string | null;
  labels: ProjectSetupLabel[];
  offset: number;
  limit: number;
  totalSteps: number;
  completedSteps: number;
  completionPercentage: number;
  firstPendingStepIndex: number;
  steps: ProjectAnnotationStepDto[];
};

export type SaveProjectAnnotationStepRequestDto = {
  datasetItemId: number;
  stepIndex?: number;
  annotation: unknown;
};

export type SaveProjectAnnotationStepResponseDto = {
  projectId: number;
  datasetItemId: number;
  stepIndex: number;
  participantCompletedSteps: number;
  participantTotalSteps: number;
  participantCompletionPercentage: number;
  projectCompletionPercentage: number;
  firstPendingStepIndex: number;
  annotation: unknown;
};

export type ProjectAnnotationWarningResponseDto = {
  projectId: number;
  datasetItemId: number;
  stepIndex: number;
  warning: boolean;
};

export type CreateProjectRequestDto = {
  name: string;
  description?: string;
};

export type UpdateProjectRequestDto = CreateProjectRequestDto & {
  participantUserIds?: number[];
  participantAssignments?: ProjectParticipantAssignment[];
};
