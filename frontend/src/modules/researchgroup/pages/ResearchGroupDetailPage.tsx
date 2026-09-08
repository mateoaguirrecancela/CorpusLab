import { PageContainer } from '@/components/common/PageContainer';
import { Spinner } from '@/components/ui/spinner';
import { ResearchGroupDetailToolbar } from '@/modules/researchgroup/components/ResearchGroupDetailToolbar';
import { ResearchGroupMembersSection } from '@/modules/researchgroup/components/ResearchGroupMembersSection';
import { ResearchGroupOverviewSection } from '@/modules/researchgroup/components/ResearchGroupOverviewSection';
import { ResearchGroupProjectsSection } from '@/modules/researchgroup/components/ResearchGroupProjectsSection';
import { useResearchGroupDetailPage } from '@/modules/researchgroup/hooks/useResearchGroupDetailPage';
import {
  type ResearchGroupAssignedProjectSummary,
  type ResearchGroupDetail,
} from '@/modules/researchgroup/types/researchGroup';
import { useTranslation } from 'react-i18next';

type ResearchGroupDetailContentProps = Readonly<{
  assignedProjects: ResearchGroupAssignedProjectSummary[];
  canCreateProjects: boolean;
  canManageResearchers: boolean;
  errorMessage: string;
  group: ResearchGroupDetail | undefined;
  hasNextProjectsPage: boolean;
  isFetchingNextProjectsPage: boolean;
  isLoading: boolean;
  isLoadingProjects: boolean;
  showArchivedProjects: boolean;
  onCreateProject: () => void;
  onDeleteGroup: () => void;
  onLoadMoreProjects: () => void;
  onOpenProject: (projectId: number) => void;
  onToggleArchivedProjects: () => void;
}>;

function ResearchGroupDetailLoading() {
  const { t } = useTranslation();

  return (
    <div className="text-sm text-muted-foreground">
      <span className="inline-flex items-center gap-2">
        <Spinner aria-hidden className="size-4" />
        {t('researchGroup.detail.loading')}
      </span>
    </div>
  );
}

function ResearchGroupDetailContent({
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
  onCreateProject,
  onDeleteGroup,
  onLoadMoreProjects,
  onOpenProject,
  onToggleArchivedProjects,
}: ResearchGroupDetailContentProps) {
  if (isLoading) {
    return <ResearchGroupDetailLoading />;
  }

  if (errorMessage || !group) {
    return null;
  }

  return (
    <div className="space-y-4">
      <ResearchGroupDetailToolbar
        canManageResearchers={canManageResearchers}
        group={group}
        onDeleted={onDeleteGroup}
      />

      <ResearchGroupOverviewSection group={group} />

      <ResearchGroupProjectsSection
        assignedProjects={assignedProjects}
        canCreateProjects={canCreateProjects}
        hasNextProjectsPage={hasNextProjectsPage}
        isFetchingNextProjectsPage={isFetchingNextProjectsPage}
        isLoadingProjects={isLoadingProjects}
        onCreateProject={onCreateProject}
        onLoadMoreProjects={onLoadMoreProjects}
        onOpenProject={onOpenProject}
        onToggleArchivedProjects={onToggleArchivedProjects}
        showArchivedProjects={showArchivedProjects}
      />

      <ResearchGroupMembersSection canManageResearchers={canManageResearchers} group={group} />
    </div>
  );
}

export default function ResearchGroupDetailPage() {
  const {
    assignedProjects,
    canCreateProjects,
    canManageResearchers,
    createProject,
    deleteGroup,
    errorMessage,
    group,
    hasNextProjectsPage,
    isFetchingNextProjectsPage,
    isLoading,
    isLoadingProjects,
    loadMoreProjects,
    openProject,
    showArchivedProjects,
    toggleArchivedProjects,
  } = useResearchGroupDetailPage();

  return (
    <PageContainer>
      <ResearchGroupDetailContent
        assignedProjects={assignedProjects}
        canCreateProjects={canCreateProjects}
        canManageResearchers={canManageResearchers}
        errorMessage={errorMessage}
        group={group}
        hasNextProjectsPage={hasNextProjectsPage}
        isFetchingNextProjectsPage={isFetchingNextProjectsPage}
        isLoading={isLoading}
        isLoadingProjects={isLoadingProjects}
        showArchivedProjects={showArchivedProjects}
        onCreateProject={createProject}
        onDeleteGroup={deleteGroup}
        onLoadMoreProjects={loadMoreProjects}
        onOpenProject={openProject}
        onToggleArchivedProjects={toggleArchivedProjects}
      />
    </PageContainer>
  );
}
