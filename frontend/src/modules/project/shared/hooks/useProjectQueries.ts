import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  invalidateProjectAnnotationSaveQueries,
  invalidateProjectAnnotationWarningQueries,
  invalidateProjectWriteQueries,
} from '@/app/config/queryInvalidation';
import {
  assignProjectParticipants,
  archiveProject,
  configureProjectSetup,
  createProject,
  deleteProject,
  getAssignedProjectsByGroup,
  getProjectParticipantAnnotationWorkspace,
  getProjectAnnotationWorkspace,
  getProjectAssignmentContext,
  getMyAssignedProjects,
  getProjectDetail,
  getProjectMetrics,
  resolveOwnProjectAnnotationWarning,
  saveProjectAnnotationStep,
  toggleProjectAnnotationWarning,
  unarchiveProject,
  updateProject,
  uploadProjectDataset,
} from '@/modules/project/shared/services/projectService';
import {
  type AssignProjectParticipantsPayload,
  type ConfigureProjectSetupPayload,
  type CreateProjectPayload,
  type ProjectAnnotationWorkspace,
  type ProjectDetail,
  type UpdateProjectPayload,
} from '@/modules/project/shared/types/project';
import { isPositiveId } from '@/modules/project/shared/utils/projectFormUtils';
import { projectQueryKeys } from '@/modules/project/shared/queryKeys';

type CreateProjectMutationInput = {
  groupId: number;
  payload: CreateProjectPayload;
};

export function useProjectCreateMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ groupId, payload }: CreateProjectMutationInput) =>
      createProject(groupId, payload),
    onSuccess: (_, variables) => {
      invalidateProjectWriteQueries(queryClient, { groupId: variables.groupId });
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
      invalidateProjectWriteQueries(queryClient, {
        groupId: variables.groupId,
        projectId: variables.projectId,
      });
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
      invalidateProjectWriteQueries(queryClient, {
        groupId: variables.groupId,
        projectId: variables.projectId,
      });
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
      invalidateProjectWriteQueries(queryClient, {
        groupId: project.researchGroupId,
        projectId: project.id,
      });
    },
  });
}

export function useUnarchiveProjectMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ projectId }: ArchiveProjectMutationInput) => unarchiveProject(projectId),
    onSuccess: (project) => {
      invalidateProjectWriteQueries(queryClient, {
        groupId: project.researchGroupId,
        projectId: project.id,
      });
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
      invalidateProjectWriteQueries(queryClient, {
        groupId: variables.groupId,
        projectId: variables.projectId,
      });
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
      invalidateProjectWriteQueries(queryClient, {
        groupId: variables.groupId,
        projectId: variables.projectId,
      });
    },
  });
}

type AssignParticipantsMutationInput = {
  groupId: number;
  projectId: number;
  payload: AssignProjectParticipantsPayload;
};

export function useAssignProjectParticipantsMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ groupId, projectId, payload }: AssignParticipantsMutationInput) =>
      assignProjectParticipants(groupId, projectId, payload),
    onSuccess: (_, variables) => {
      invalidateProjectWriteQueries(queryClient, {
        groupId: variables.groupId,
        projectId: variables.projectId,
      });
    },
  });
}

const PROJECTS_PAGE_SIZE = 12;

export function useMyAssignedProjectsQuery(showArchived = false) {
  return useInfiniteQuery({
    queryKey: projectQueryKeys.myAssignedList(showArchived),
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
    queryKey: projectQueryKeys.assignedByGroupList(groupId, showArchived),
    queryFn: ({ pageParam }) =>
      getAssignedProjectsByGroup(groupId, {
        page: pageParam,
        size: 3,
        showArchived,
      }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => (lastPage.last ? undefined : lastPage.number + 1),
    enabled: isPositiveId(groupId),
  });
}

export function useProjectDetailQuery(projectId: number) {
  return useQuery({
    queryKey: projectQueryKeys.detail(projectId),
    queryFn: () => getProjectDetail(projectId),
    enabled: isPositiveId(projectId),
  });
}

export function useProjectAssignmentContextQuery(groupId: number) {
  return useQuery({
    queryKey: projectQueryKeys.assignmentContext(groupId),
    queryFn: () => getProjectAssignmentContext(groupId),
    enabled: isPositiveId(groupId),
  });
}

type ProjectMetricsQueryOptions = {
  enabled?: boolean;
};

export function useProjectMetricsQuery(projectId: number, options?: ProjectMetricsQueryOptions) {
  return useQuery({
    queryKey: projectQueryKeys.metrics(projectId),
    queryFn: () => getProjectMetrics(projectId),
    enabled: (options?.enabled ?? true) && isPositiveId(projectId),
  });
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
    queryKey: projectQueryKeys.annotationWorkspace(projectId, offset, limit),
    queryFn: () => getProjectAnnotationWorkspace(projectId, offset, limit),
    enabled: (options?.enabled ?? true) && isPositiveId(projectId),
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
    queryKey: projectQueryKeys.participantAnnotationWorkspace(
      projectId,
      participantUserId,
      offset,
      limit,
    ),
    queryFn: () =>
      getProjectParticipantAnnotationWorkspace(projectId, participantUserId, offset, limit),
    enabled:
      (options?.enabled ?? true) && isPositiveId(projectId) && isPositiveId(participantUserId),
  });
}

type SaveProjectAnnotationStepMutationInput = {
  projectId: number;
  datasetItemId: number;
  stepIndex?: number;
  annotation: unknown;
};

export function useSaveProjectAnnotationStepMutation() {
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
    onSuccess: (response, variables) => {
      queryClient.setQueriesData<ProjectAnnotationWorkspace>(
        {
          predicate: (query) => {
            const [scope, workspace, queriedProjectId, pageOffset] = query.queryKey;
            return (
              scope === 'projects' &&
              workspace === 'annotation-workspace' &&
              queriedProjectId === variables.projectId &&
              typeof pageOffset === 'number'
            );
          },
        },
        (current) => {
          if (!current) {
            return current;
          }

          const savedStepIndex = response.stepIndex;
          const nextSteps = current.steps.map((step) =>
            step.datasetItemId === variables.datasetItemId && step.stepIndex === savedStepIndex
              ? {
                  ...step,
                  annotation: response.annotation,
                  completed: response.annotation != null,
                }
              : step,
          );

          return {
            ...current,
            steps: nextSteps,
            completedSteps: response.participantCompletedSteps,
            totalSteps: response.participantTotalSteps,
            completionPercentage: response.participantCompletionPercentage,
            firstPendingStepIndex: response.firstPendingStepIndex,
          };
        },
      );

      queryClient.setQueryData<ProjectDetail>(
        projectQueryKeys.detail(variables.projectId),
        (current) =>
          current
            ? {
                ...current,
                completionPercentage: response.projectCompletionPercentage,
              }
            : current,
      );

      invalidateProjectAnnotationSaveQueries(queryClient, variables.projectId);
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
      invalidateProjectAnnotationWarningQueries(
        queryClient,
        projectQueryKeys.participantAnnotationWorkspace(
          variables.projectId,
          variables.participantUserId,
          offset,
          limit,
        ),
      );
    },
  });
}

type ResolveOwnProjectAnnotationWarningMutationInput = {
  projectId: number;
  datasetItemId: number;
  stepIndex: number;
};

export function useResolveOwnProjectAnnotationWarningMutation(offset: number, limit: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      projectId,
      datasetItemId,
      stepIndex,
    }: ResolveOwnProjectAnnotationWarningMutationInput) =>
      resolveOwnProjectAnnotationWarning(projectId, datasetItemId, stepIndex),
    onSuccess: (_, variables) => {
      invalidateProjectAnnotationWarningQueries(
        queryClient,
        projectQueryKeys.annotationWorkspace(variables.projectId, offset, limit),
      );
    },
  });
}
