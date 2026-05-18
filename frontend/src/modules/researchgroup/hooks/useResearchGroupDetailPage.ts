import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router';
import { useToastMessages } from '@/hooks/useToastMessages';
import { useAssignedProjectsByGroupQuery } from '@/modules/project/hooks/useProjectQueries';
import { getProjectsLoadErrorMessage } from '@/modules/project/services/projectService';
import { type ProjectAssignedSummary } from '@/modules/project/types/project';
import { useResearchGroupDetailQuery } from '@/modules/researchgroup/hooks/useResearchGroupQueries';
import { getResearchGroupDetailErrorMessage } from '@/modules/researchgroup/services/researchGroupService';
import { useResearchGroupUIStore } from '@/modules/researchgroup/stores/useResearchGroupUIStore';
import { type ResearchGroupDetail } from '@/modules/researchgroup/types/researchGroup';
import {
  getAssignedProjectsFromPages,
  isValidResearchGroupId,
  parseResearchGroupId,
} from '@/modules/researchgroup/utils/researchGroupDetailPage';

type ResearchGroupDetailPageState = Readonly<{
  assignedProjects: ProjectAssignedSummary[];
  canCreateProjects: boolean;
  canManageResearchers: boolean;
  errorMessage: string;
  group: ResearchGroupDetail | undefined;
  hasNextProjectsPage: boolean;
  isFetchingNextProjectsPage: boolean;
  isLoading: boolean;
  isLoadingProjects: boolean;
  showArchivedProjects: boolean;
  createProject: () => void;
  deleteGroup: () => void;
  loadMoreProjects: () => void;
  openProject: (projectId: number) => void;
  toggleArchivedProjects: () => void;
}>;

export function useResearchGroupDetailPage(): ResearchGroupDetailPageState {
  const { t } = useTranslation();
  const { id } = useParams();
  const navigate = useNavigate();

  const numericGroupId = parseResearchGroupId(id);
  const isInvalidGroupId = !isValidResearchGroupId(numericGroupId);
  const showArchivedProjects = useResearchGroupUIStore(
    (state) => state.archivedProjectsByGroupId[numericGroupId] ?? false,
  );
  const toggleArchivedProjectsForGroup = useResearchGroupUIStore(
    (state) => state.toggleArchivedProjects,
  );
  const { data: group, isLoading, isError, error } = useResearchGroupDetailQuery(numericGroupId);

  const errorMessage = getResearchGroupDetailPageErrorMessage({
    error,
    isError,
    isInvalidGroupId,
    invalidGroupMessage: t('researchGroup.errors.invalidGroupId'),
  });

  const {
    data: assignedProjectsData,
    isLoading: isLoadingProjects,
    isError: isProjectsError,
    error: projectsError,
    hasNextPage: hasNextProjectsPage,
    fetchNextPage: fetchNextProjectsPage,
    isFetchingNextPage: isFetchingNextProjectsPage,
  } = useAssignedProjectsByGroupQuery(numericGroupId, showArchivedProjects);

  const assignedProjects = getAssignedProjectsFromPages(assignedProjectsData);
  const projectsErrorMessage = isProjectsError ? getProjectsLoadErrorMessage(projectsError) : '';

  useToastMessages({
    errorMessage,
    errorToastId: 'research-group-detail-load-error',
  });
  useToastMessages({
    errorMessage: projectsErrorMessage,
    errorToastId: 'group-projects-load-error',
  });

  return {
    assignedProjects,
    canCreateProjects: group?.canCreateProjects ?? false,
    canManageResearchers: group?.canManageResearchers ?? false,
    errorMessage,
    group,
    hasNextProjectsPage,
    isFetchingNextProjectsPage,
    isLoading,
    isLoadingProjects,
    showArchivedProjects,
    createProject: () => {
      if (group) {
        navigate(`/home/projects/create?groupId=${group.id}`);
      }
    },
    deleteGroup: () => navigate('/home/research-groups'),
    loadMoreProjects: () => {
      void fetchNextProjectsPage();
    },
    openProject: (projectId: number) => navigate(`/home/projects/${projectId}`),
    toggleArchivedProjects: () => toggleArchivedProjectsForGroup(numericGroupId),
  };
}

function getResearchGroupDetailPageErrorMessage({
  error,
  invalidGroupMessage,
  isError,
  isInvalidGroupId,
}: {
  error: unknown;
  invalidGroupMessage: string;
  isError: boolean;
  isInvalidGroupId: boolean;
}): string {
  if (isInvalidGroupId) {
    return invalidGroupMessage;
  }

  if (isError) {
    return getResearchGroupDetailErrorMessage(error);
  }

  return '';
}
