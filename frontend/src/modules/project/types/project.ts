export type ProjectSummary = {
  id: number;
  researchGroupId: number;
  researchGroupName?: string;
  name: string;
  description: string | null;
  createdAt: string;
};

export type SliceResponse<T> = {
  content: T[];
  first: boolean;
  last: boolean;
  number: number;
  size: number;
  numberOfElements: number;
  empty: boolean;
};

export type ProjectParticipantRole = 'CREATOR' | 'PARTICIPANT';

export type ProjectParticipantAssignmentGroup = 'GROUP_A' | 'GROUP_B';

export type ProjectParticipantAssignment = {
  userId: number;
  iaaGroup: ProjectParticipantAssignmentGroup;
};

export type ProjectAssignableMember = {
  userId: number;
  firstName: string;
  lastName: string;
  email: string;
};

export type ProjectAssignmentContext = {
  researchGroupId: number;
  currentUserId: number;
  members: ProjectAssignableMember[];
  defaultAssignments: ProjectParticipantAssignment[];
};

export type ProjectDetailParticipant = {
  userId: number;
  firstName: string;
  lastName: string;
  email: string;
  role: ProjectParticipantRole;
  iaaGroup: ProjectParticipantAssignmentGroup | null;
  completionPercentage: number;
};

export type ProjectAssignedSummary = {
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

export type CreateProjectPayload = {
  name: string;
  description?: string;
};

export type UpdateProjectPayload = {
  name: string;
  description?: string;
  participantUserIds?: number[];
  participantAssignments?: ProjectParticipantAssignment[];
};

export type DatasetItem = {
  id: number;
  index: number;
  fileName: string;
  mimeType: string;
  sizeBytes: number;
  createdAt: string;
};

export type UploadDatasetResponse = {
  projectId: number;
  uploadedItems: number;
  items: DatasetItem[];
};

export type UploadDatasetJobResponse = {
  jobId: string;
};

export type UploadDatasetEvent = {
  jobId: string;
  status: 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED';
  progress: number;
  message: string | null;
  result: UploadDatasetResponse | null;
};

export type ProjectType =
  | 'TEXT_CLASSIFICATION_SIMPLE'
  | 'TEXT_CLASSIFICATION_MULTILABEL'
  | 'NER'
  | 'SEQ2SEQ';

export type ProjectMetricType =
  | 'COHENS_KAPPA'
  | 'KRIPPENDORFFS_ALPHA'
  | 'FLEISS_KAPPA'
  | 'XRR'
  | 'SPAN_OVERLAP_F1';

export type ProjectMetricStatus =
  | 'CALCULABLE'
  | 'NO_ANNOTATIONS'
  | 'INSUFFICIENT_ANNOTATORS'
  | 'INSUFFICIENT_ITEMS'
  | 'NO_SHARED_ITEMS'
  | 'NO_VALID_PAIRS'
  | 'UNDEFINED';

export type ProjectMetric = {
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

export type ProjectMetricsResponse = {
  projectId: number;
  metrics: ProjectMetric[];
};

export type ProjectSetupLabel = {
  name: string;
  color: string | null;
};

export type ConfigureProjectSetupPayload = {
  projectType: ProjectType;
  labels: ProjectSetupLabel[];
  guidelineText?: string;
  guidelinePdfFile?: File;
  annotationTargetColumn?: string;
};

export type ProjectSetupResponse = {
  projectId: number;
  projectType: ProjectType;
  labels: ProjectSetupLabel[];
  guidelineText: string | null;
  annotationTargetColumn: string | null;
  setupCompleted: boolean;
};

export type AssignProjectParticipantsPayload = {
  participantUserIds?: number[];
  participantAssignments?: ProjectParticipantAssignment[];
};

export type ProjectDetail = {
  id: number;
  researchGroupId: number;
  researchGroupName: string;
  name: string;
  description: string | null;
  projectType: ProjectType;
  completionPercentage: number;
  participantRole: ProjectParticipantRole;
  participants: ProjectDetailParticipant[];
  datasetItems: DatasetItem[];
  labels: ProjectSetupLabel[];
  guidelineText: string | null;
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

export type AnnotationStep = {
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

export type ProjectAnnotationWorkspace = {
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
  steps: AnnotationStep[];
};

export type ProjectDatasetItemContent = {
  blob: Blob;
  mimeType: string;
  fileName: string | null;
};

export type SaveProjectAnnotationStepPayload = {
  datasetItemId: number;
  stepIndex?: number;
  annotation: unknown;
};

export type SaveProjectAnnotationStepResponse = {
  projectId: number;
  datasetItemId: number;
  stepIndex: number;
  participantCompletedSteps: number;
  participantTotalSteps: number;
  participantCompletionPercentage: number;
  projectCompletionPercentage: number;
};
