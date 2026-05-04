import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { invalidateQueryKeys } from '@/lib/queryInvalidation';
import {
  assignProjectParticipants,
  archiveProject,
  configureProjectSetup,
  createProject,
  deleteProject,
  getAssignedProjectsByGroup,
  getProjectParticipantAnnotationWorkspace,
  getProjectAnnotationWorkspace,
  getMyAssignedProjects,
  getProjectDetail,
  saveProjectAnnotationStep,
  toggleProjectAnnotationWarning,
  unarchiveProject,
  updateProject,
  uploadProjectDataset,
} from '@/modules/project/services/projectService';
import {
  type ConfigureProjectSetupPayload,
  type CreateProjectPayload,
  type UpdateProjectPayload,
} from '@/modules/project/types/project';
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

type UpdateProjectMutationInput = {
  groupId: number;
  projectId: number;
  payload: UpdateProjectPayload;
};

export function useUpdateProjectMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ groupId, projectId, payload }: UpdateProjectMutationInput) =>
      updateProject(groupId, projectId, payload),
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

type DeleteProjectMutationInput = {
  groupId: number;
  projectId: number;
};

export function useDeleteProjectMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ groupId, projectId }: DeleteProjectMutationInput) =>
      deleteProject(groupId, projectId),
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

type ArchiveProjectMutationInput = {
  projectId: number;
};

export function useArchiveProjectMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ projectId }: ArchiveProjectMutationInput) => archiveProject(projectId),
    onSuccess: (project) => {
      invalidateQueryKeys(queryClient, [
        myAssignedProjectsQueryKey(),
        assignedProjectsByGroupQueryKey(project.researchGroupId),
        projectDetailQueryKey(project.id),
        researchGroupDetailQueryKey(project.researchGroupId),
        RESEARCH_GROUPS_QUERY_KEY,
      ]);
    },
  });
}

export function useUnarchiveProjectMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ projectId }: ArchiveProjectMutationInput) => unarchiveProject(projectId),
    onSuccess: (project) => {
      invalidateQueryKeys(queryClient, [
        myAssignedProjectsQueryKey(),
        assignedProjectsByGroupQueryKey(project.researchGroupId),
        projectDetailQueryKey(project.id),
        researchGroupDetailQueryKey(project.researchGroupId),
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
  payload: ConfigureProjectSetupPayload;
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

export function myAssignedProjectsListQueryKey(showArchived: boolean) {
  return [...myAssignedProjectsQueryKey(), showArchived ? 'archived' : 'active'] as const;
}

export function assignedProjectsByGroupQueryKey(groupId: number) {
  return ['projects', 'group', groupId, 'my'] as const;
}

export function assignedProjectsByGroupListQueryKey(groupId: number, showArchived: boolean) {
  return [
    ...assignedProjectsByGroupQueryKey(groupId),
    showArchived ? 'archived' : 'active',
  ] as const;
}

export function projectDetailQueryKey(projectId: number) {
  return ['projects', 'detail', projectId] as const;
}

const PROJECTS_PAGE_SIZE = 12;

export function useMyAssignedProjectsQuery(showArchived = false) {
  return useInfiniteQuery({
    queryKey: myAssignedProjectsListQueryKey(showArchived),
    queryFn: ({ pageParam }) =>
      getMyAssignedProjects({
        page: pageParam,
        size: PROJECTS_PAGE_SIZE,
        showArchived,
      }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => (lastPage.last ? undefined : lastPage.number + 1),
  });
}

export function useAssignedProjectsByGroupQuery(groupId: number, showArchived = false) {
  return useInfiniteQuery({
    queryKey: assignedProjectsByGroupListQueryKey(groupId, showArchived),
    queryFn: ({ pageParam }) =>
      getAssignedProjectsByGroup(groupId, {
        page: pageParam,
        size: 3,
        showArchived,
      }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => (lastPage.last ? undefined : lastPage.number + 1),
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

type ToggleProjectAnnotationWarningMutationInput = {
  projectId: number;
  participantUserId: number;
  datasetItemId: number;
  stepIndex: number;
};

export function useToggleProjectAnnotationWarningMutation(offset: number, limit: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      projectId,
      participantUserId,
      datasetItemId,
      stepIndex,
    }: ToggleProjectAnnotationWarningMutationInput) =>
      toggleProjectAnnotationWarning(projectId, participantUserId, datasetItemId, stepIndex),
    onSuccess: (_, variables) => {
      invalidateQueryKeys(queryClient, [
        projectParticipantAnnotationWorkspaceQueryKey(
          variables.projectId,
          variables.participantUserId,
          offset,
          limit,
        ),
      ]);
    },
  });
}
