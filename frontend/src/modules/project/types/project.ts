export type ProjectSummary = {
  id: number;
  researchGroupId: number;
  researchGroupName?: string;
  name: string;
  description: string | null;
  createdAt: string;
};

export type ProjectParticipantRole = 'CREATOR' | 'PARTICIPANT';

export type ProjectDetailParticipant = {
  userId: number;
  firstName: string;
  lastName: string;
  email: string;
  role: ProjectParticipantRole;
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
  createdAt: string;
};

export type CreateProjectPayload = {
  name: string;
  description?: string;
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

export type ProjectType =
  | 'TEXT_CLASSIFICATION_SIMPLE'
  | 'TEXT_CLASSIFICATION_MULTILABEL'
  | 'NER'
  | 'SEQ2SEQ';

export type ProjectSetupLabel = {
  name: string;
  color: string | null;
};

export type ConfigureProjectSetupPayload = {
  projectType: ProjectType;
  labels: ProjectSetupLabel[];
  guidelineText?: string;
  guidelinePdfBase64?: string;
};

export type ProjectSetupResponse = {
  projectId: number;
  projectType: ProjectType;
  labels: ProjectSetupLabel[];
  guidelineText: string | null;
  guidelinePdfBase64: string | null;
  setupCompleted: boolean;
};

export type AssignProjectParticipantsPayload = {
  participantUserIds: number[];
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
  guidelinePdfBase64: string | null;
  datasetItemsCount: number;
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
  completed: boolean;
  annotation: unknown;
};

export type ProjectAnnotationWorkspace = {
  projectId: number;
  projectType: ProjectType;
  labels: ProjectSetupLabel[];
  offset: number;
  limit: number;
  totalSteps: number;
  completedSteps: number;
  completionPercentage: number;
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
