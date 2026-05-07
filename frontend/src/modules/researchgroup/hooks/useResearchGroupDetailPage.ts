import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router';
import { toast } from 'sonner';
import { useProfileQuery } from '@/modules/auth/hooks/useProfileQuery';
import { useAssignedProjectsByGroupQuery } from '@/modules/project/hooks/useProjectQueries';
import { getProjectsLoadErrorMessage } from '@/modules/project/services/projectService';
import { type ProjectAssignedSummary } from '@/modules/project/types/project';
import { useResearchGroupDetailQuery } from '@/modules/researchgroup/hooks/useResearchGroupQueries';
import { getResearchGroupDetailErrorMessage } from '@/modules/researchgroup/services/researchGroupService';
import { type ResearchGroupDetail } from '@/modules/researchgroup/types/researchGroup';

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
  const { data: profile } = useProfileQuery();
  const [showArchivedProjects, setShowArchivedProjects] = useState(false);

  const numericGroupId = useMemo(() => Number(id), [id]);
  const isInvalidGroupId = !Number.isFinite(numericGroupId) || numericGroupId <= 0;
  const { data: group, isLoading, isError, error } = useResearchGroupDetailQuery(numericGroupId);

  const errorMessage = useMemo(() => {
    if (isInvalidGroupId) {
      return t('researchGroup.errors.invalidGroupId');
    }

    if (isError) {
      return getResearchGroupDetailErrorMessage(error);
    }

    return '';
  }, [error, isError, isInvalidGroupId, t]);

  useEffect(() => {
    if (errorMessage.length > 0) {
      toast.error(errorMessage, { id: 'research-group-detail-load-error' });
    }
  }, [errorMessage]);

  const profileEmail = profile?.email?.toLowerCase();
  const currentMember =
    group && profileEmail
      ? group.members.find((member) => member.email.toLowerCase() === profileEmail)
      : undefined;

  const canManageResearchers = currentMember?.role === 'OWNER';
  const canCreateProjects = currentMember?.role === 'OWNER' || currentMember?.role === 'ADMIN';

  const {
    data: assignedProjectsData,
    isLoading: isLoadingProjects,
    isError: isProjectsError,
    error: projectsError,
    hasNextPage: hasNextProjectsPage,
    fetchNextPage: fetchNextProjectsPage,
    isFetchingNextPage: isFetchingNextProjectsPage,
  } = useAssignedProjectsByGroupQuery(numericGroupId, showArchivedProjects);

  const assignedProjects = useMemo(
    () => assignedProjectsData?.pages.flatMap((page) => page.content) ?? [],
    [assignedProjectsData],
  );

  const projectsErrorMessage = isProjectsError ? getProjectsLoadErrorMessage(projectsError) : '';

  useEffect(() => {
    if (projectsErrorMessage.length > 0) {
      toast.error(projectsErrorMessage, { id: 'group-projects-load-error' });
    }
  }, [projectsErrorMessage]);

  return {
    assignedProjects,
    canCreateProjects,
    canManageResearchers,
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
    toggleArchivedProjects: () => setShowArchivedProjects((current) => !current),
  };
}
