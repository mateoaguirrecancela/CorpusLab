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
