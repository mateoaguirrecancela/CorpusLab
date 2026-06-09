import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import { dashboardQueryKeys } from '@/modules/dashboard/queryKeys';
import {
  getDashboardProjects,
  getDashboardProjectsErrorMessage,
} from '@/modules/dashboard/services/dashboardService';
import { type DashboardProject } from '@/modules/dashboard/types/dashboard';

type DashboardProjectsState = Readonly<{
  advancedProjects: DashboardProject[];
  errorMessage: string;
  isLoading: boolean;
  recentProjects: DashboardProject[];
  createProject: () => void;
  openProject: (projectId: number) => void;
  openProjects: () => void;
}>;

export function useDashboardProjects(): DashboardProjectsState {
  const navigate = useNavigate();
  const query = useQuery({
    queryKey: dashboardQueryKeys.projects,
    queryFn: getDashboardProjects,
  });

  return {
    advancedProjects: query.data?.advancedProjects ?? [],
    errorMessage: query.isError ? getDashboardProjectsErrorMessage(query.error) : '',
    isLoading: query.isLoading,
    recentProjects: query.data?.recentProjects ?? [],
    createProject: () => navigate('/home/projects/create'),
    openProject: (projectId) => navigate(`/home/projects/${projectId}`),
    openProjects: () => navigate('/home/projects'),
  };
}
