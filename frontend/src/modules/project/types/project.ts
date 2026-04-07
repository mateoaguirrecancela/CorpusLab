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
