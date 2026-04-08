export type ProjectSummary = {
  id: number;
  researchGroupId: number;
  name: string;
  description: string | null;
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
