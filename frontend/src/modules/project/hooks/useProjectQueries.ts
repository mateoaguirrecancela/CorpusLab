import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { invalidateQueryKeys } from '@/lib/queryInvalidation';
import {
  assignProjectParticipants,
  configureProjectSetup,
  createProject,
  getAssignedProjectsByGroup,
  getProjectParticipantAnnotationWorkspace,
  getProjectAnnotationWorkspace,
  getMyAssignedProjects,
  getProjectDetail,
  saveProjectAnnotationStep,
  uploadProjectDataset,
} from '@/modules/project/services/projectService';
import { type CreateProjectPayload } from '@/modules/project/types/project';
import {
  RESEARCH_GROUPS_QUERY_KEY,
  researchGroupDetailQueryKey,
} from '@/modules/researchgroup/hooks/useResearchGroupQueries';

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
        myAssignedProjectsQueryKey(),
        assignedProjectsByGroupQueryKey(variables.groupId),
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
        myAssignedProjectsQueryKey(),
        assignedProjectsByGroupQueryKey(variables.groupId),
        projectDetailQueryKey(variables.projectId),
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
        myAssignedProjectsQueryKey(),
        assignedProjectsByGroupQueryKey(variables.groupId),
        projectDetailQueryKey(variables.projectId),
        researchGroupDetailQueryKey(variables.groupId),
        RESEARCH_GROUPS_QUERY_KEY,
      ]);
    },
  });
}

type AssignParticipantsMutationInput = {
  groupId: number;
  projectId: number;
  participantUserIds: number[];
};

export function useAssignProjectParticipantsMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ groupId, projectId, participantUserIds }: AssignParticipantsMutationInput) =>
      assignProjectParticipants(groupId, projectId, { participantUserIds }),
    onSuccess: (_, variables) => {
      invalidateQueryKeys(queryClient, [
        myAssignedProjectsQueryKey(),
        assignedProjectsByGroupQueryKey(variables.groupId),
        projectDetailQueryKey(variables.projectId),
        researchGroupDetailQueryKey(variables.groupId),
        RESEARCH_GROUPS_QUERY_KEY,
      ]);
    },
  });
}

export function myAssignedProjectsQueryKey() {
  return ['projects', 'my'] as const;
}

export function assignedProjectsByGroupQueryKey(groupId: number) {
  return ['projects', 'group', groupId, 'my'] as const;
}

export function projectDetailQueryKey(projectId: number) {
  return ['projects', 'detail', projectId] as const;
}

export function useMyAssignedProjectsQuery() {
  return useQuery({
    queryKey: myAssignedProjectsQueryKey(),
    queryFn: getMyAssignedProjects,
  });
}

export function useAssignedProjectsByGroupQuery(groupId: number) {
  return useQuery({
    queryKey: assignedProjectsByGroupQueryKey(groupId),
    queryFn: () => getAssignedProjectsByGroup(groupId),
    enabled: Number.isFinite(groupId) && groupId > 0,
  });
}

export function useProjectDetailQuery(projectId: number) {
  return useQuery({
    queryKey: projectDetailQueryKey(projectId),
    queryFn: () => getProjectDetail(projectId),
    enabled: Number.isFinite(projectId) && projectId > 0,
  });
}

export function projectAnnotationWorkspaceQueryKey(
  projectId: number,
  offset: number,
  limit: number,
) {
  return ['projects', 'annotation-workspace', projectId, offset, limit] as const;
}

export function projectParticipantAnnotationWorkspaceQueryKey(
  projectId: number,
  participantUserId: number,
  offset: number,
  limit: number,
) {
  return [
    'projects',
    'annotation-workspace',
    projectId,
    'participant',
    participantUserId,
    offset,
    limit,
  ] as const;
}

type WorkspaceQueryOptions = {
  enabled?: boolean;
};

export function useProjectAnnotationWorkspaceQuery(
  projectId: number,
  offset: number,
  limit: number,
  options?: WorkspaceQueryOptions,
) {
  return useQuery({
    queryKey: projectAnnotationWorkspaceQueryKey(projectId, offset, limit),
    queryFn: () => getProjectAnnotationWorkspace(projectId, offset, limit),
    enabled: (options?.enabled ?? true) && Number.isFinite(projectId) && projectId > 0,
  });
}

export function useProjectParticipantAnnotationWorkspaceQuery(
  projectId: number,
  participantUserId: number,
  offset: number,
  limit: number,
  options?: WorkspaceQueryOptions,
) {
  return useQuery({
    queryKey: projectParticipantAnnotationWorkspaceQueryKey(
      projectId,
      participantUserId,
      offset,
      limit,
    ),
    queryFn: () =>
      getProjectParticipantAnnotationWorkspace(projectId, participantUserId, offset, limit),
    enabled:
      (options?.enabled ?? true) &&
      Number.isFinite(projectId) &&
      projectId > 0 &&
      Number.isFinite(participantUserId) &&
      participantUserId > 0,
  });
}

type SaveProjectAnnotationStepMutationInput = {
  projectId: number;
  datasetItemId: number;
  stepIndex?: number;
  annotation: unknown;
};

export function useSaveProjectAnnotationStepMutation(offset: number, limit: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      projectId,
      datasetItemId,
      stepIndex,
      annotation,
    }: SaveProjectAnnotationStepMutationInput) =>
      saveProjectAnnotationStep(projectId, {
        datasetItemId,
        stepIndex,
        annotation,
      }),
    onSuccess: (_, variables) => {
      invalidateQueryKeys(queryClient, [
        myAssignedProjectsQueryKey(),
        projectDetailQueryKey(variables.projectId),
        projectAnnotationWorkspaceQueryKey(variables.projectId, offset, limit),
      ]);
    },
  });
}
