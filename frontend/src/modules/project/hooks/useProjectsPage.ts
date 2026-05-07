import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router';
import { toast } from 'sonner';
import { useMyAssignedProjectsQuery } from '@/modules/project/hooks/useProjectQueries';
import { getProjectsLoadErrorMessage } from '@/modules/project/services/projectService';
import { type ProjectAssignedSummary } from '@/modules/project/types/project';

type ProjectsPageState = Readonly<{
  errorMessage: string;
  hasNextPage: boolean;
  isFetchingNextPage: boolean;
  isLoading: boolean;
  projects: ProjectAssignedSummary[];
  showArchived: boolean;
  loadMoreProjects: () => void;
  openCreateProject: () => void;
  openProject: (projectId: number) => void;
  toggleArchived: () => void;
}>;

export function useProjectsPage(): ProjectsPageState {
  const navigate = useNavigate();
  const [showArchived, setShowArchived] = useState(false);

  const { data, isLoading, isError, error, hasNextPage, fetchNextPage, isFetchingNextPage } =
    useMyAssignedProjectsQuery(showArchived);

  const projects = useMemo(() => data?.pages.flatMap((page) => page.content) ?? [], [data]);
  const errorMessage = isError ? getProjectsLoadErrorMessage(error) : '';

  useEffect(() => {
    if (errorMessage.length > 0) {
      toast.error(errorMessage, { id: 'projects-load-error' });
    }
  }, [errorMessage]);

  return {
    errorMessage,
    hasNextPage,
    isFetchingNextPage,
    isLoading,
    projects,
    showArchived,
    loadMoreProjects: () => {
      void fetchNextPage();
    },
    openCreateProject: () => navigate('/home/projects/create'),
    openProject: (projectId: number) => navigate(`/home/projects/${projectId}`),
    toggleArchived: () => setShowArchived((current) => !current),
  };
}
