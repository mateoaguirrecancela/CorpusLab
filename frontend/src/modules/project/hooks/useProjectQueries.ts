import { useMutation, useQueryClient } from '@tanstack/react-query';
import { invalidateQueryKeys } from '@/lib/queryInvalidation';
import {
  configureProjectSetup,
  createProject,
  uploadProjectDataset,
} from '@/modules/project/services/projectService';
import {
  RESEARCH_GROUPS_QUERY_KEY,
  researchGroupDetailQueryKey,
} from '@/modules/researchgroup/hooks/useResearchGroupQueries';
import { type CreateProjectPayload } from '@/modules/project/types/project';

type CreateProjectMutationInput = {
  groupId: number;
  payload: CreateProjectPayload;
};

export function useCreateProjectMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ groupId, payload }: CreateProjectMutationInput) =>
      createProject(groupId, payload),
    onSuccess: (_, variables) => {
      invalidateQueryKeys(queryClient, [
        researchGroupDetailQueryKey(variables.groupId),
        RESEARCH_GROUPS_QUERY_KEY,
      ]);
    },
  });
}

type UploadDatasetMutationInput = {
  groupId: number;
  projectId: number;
  files: File[];
};

export function useUploadProjectDatasetMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ groupId, projectId, files }: UploadDatasetMutationInput) =>
      uploadProjectDataset(groupId, projectId, files),
    onSuccess: (_, variables) => {
      invalidateQueryKeys(queryClient, [
        researchGroupDetailQueryKey(variables.groupId),
        RESEARCH_GROUPS_QUERY_KEY,
      ]);
    },
  });
}

type ConfigureProjectSetupMutationInput = {
  groupId: number;
  projectId: number;
  payload: {
    projectType:
      | 'TEXT_CLASSIFICATION_SIMPLE'
      | 'TEXT_CLASSIFICATION_MULTILABEL'
      | 'NER'
      | 'SEQ2SEQ';
    labels: Array<{ name: string; color: string | null }>;
    guidelineText?: string;
    guidelinePdfBase64?: string;
  };
};

export function useConfigureProjectSetupMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ groupId, projectId, payload }: ConfigureProjectSetupMutationInput) =>
      configureProjectSetup(groupId, projectId, payload),
    onSuccess: (_, variables) => {
      invalidateQueryKeys(queryClient, [
        researchGroupDetailQueryKey(variables.groupId),
        RESEARCH_GROUPS_QUERY_KEY,
      ]);
    },
  });
}
