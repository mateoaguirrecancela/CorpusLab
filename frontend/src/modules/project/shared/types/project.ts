import type {
  CreateProjectRequestDto,
  DatasetItemDto,
  ProjectAnnotationStepDto,
  ProjectAnnotationWorkspaceDto,
  ProjectAssignedSummaryDto,
  ProjectAssignableMemberDto,
  ProjectAssignmentContextDto,
  ProjectDetailDto,
  ProjectDetailParticipantDto,
  ProjectMetricDto,
  ProjectMetricsDto,
  ProjectSetupResponseDto,
  ProjectAnnotationWarningResponseDto,
  ProjectSummaryDto,
  SaveProjectAnnotationStepRequestDto,
  SaveProjectAnnotationStepResponseDto,
  UpdateProjectRequestDto,
  UploadProjectDatasetEventDto,
  UploadProjectDatasetJobDto,
  UploadProjectDatasetResponseDto,
} from '@/modules/project/shared/api/projectDtos';
import type {
  ProjectParticipantAssignment,
  ProjectParticipantAssignmentGroup,
  ProjectParticipantRole,
  ProjectMetricStatus,
  ProjectMetricType,
  ProjectSetupLabel,
  ProjectType,
} from '@/modules/project/shared/types/projectPrimitives';

export type {
  ProjectParticipantAssignment,
  ProjectParticipantAssignmentGroup,
  ProjectParticipantRole,
  ProjectMetricStatus,
  ProjectMetricType,
  ProjectSetupLabel,
  ProjectType,
};

export type ProjectSummary = ProjectSummaryDto & {
  researchGroupName?: string;
};

export type ProjectAssignableMember = ProjectAssignableMemberDto;

export type ProjectAssignmentContext = ProjectAssignmentContextDto;

export type ProjectDetailParticipant = ProjectDetailParticipantDto;

export type ProjectAssignedSummary = ProjectAssignedSummaryDto;

export type CreateProjectPayload = CreateProjectRequestDto;

export type UpdateProjectPayload = UpdateProjectRequestDto;

export type DatasetItem = DatasetItemDto;

export type UploadDatasetResponse = UploadProjectDatasetResponseDto;

export type UploadDatasetJobResponse = UploadProjectDatasetJobDto;

export type UploadDatasetEvent = UploadProjectDatasetEventDto;

export type ProjectMetric = ProjectMetricDto;

export type ProjectMetricsResponse = ProjectMetricsDto;

export type ConfigureProjectSetupPayload = {
  projectType: ProjectType;
  labels: ProjectSetupLabel[];
  guidelineText?: string;
  guidelinePdfFile?: File;
  annotationTargetColumn?: string;
};

export type ProjectSetupResponse = ProjectSetupResponseDto;

export type AssignProjectParticipantsPayload = {
  participantUserIds?: number[];
  participantAssignments?: ProjectParticipantAssignment[];
};

export type ProjectDetail = Omit<ProjectDetailDto, 'guidelinePdfBase64'>;

export type AnnotationStep = ProjectAnnotationStepDto;

export type ProjectAnnotationWorkspace = ProjectAnnotationWorkspaceDto;

export type ProjectDatasetItemContent = {
  blob: Blob;
  mimeType: string;
  fileName: string | null;
};

export type SaveProjectAnnotationStepPayload = SaveProjectAnnotationStepRequestDto;

export type SaveProjectAnnotationStepResponse = SaveProjectAnnotationStepResponseDto;

export type ProjectAnnotationWarningResponse = ProjectAnnotationWarningResponseDto;
