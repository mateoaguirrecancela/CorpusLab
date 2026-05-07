import { PageContainer } from '@/components/common/PageContainer';
import { Spinner } from '@/components/ui/spinner';
import { ResearchGroupDetailToolbar } from '@/modules/researchgroup/components/ResearchGroupDetailToolbar';
import { ResearchGroupMembersSection } from '@/modules/researchgroup/components/ResearchGroupMembersSection';
import { ResearchGroupOverviewSection } from '@/modules/researchgroup/components/ResearchGroupOverviewSection';
import { ResearchGroupProjectsSection } from '@/modules/researchgroup/components/ResearchGroupProjectsSection';
import { useResearchGroupDetailPage } from '@/modules/researchgroup/hooks/useResearchGroupDetailPage';
import { useTranslation } from 'react-i18next';

export default function ResearchGroupDetailPage() {
  const { t } = useTranslation();
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
      {isLoading && (
        <div className="text-sm text-muted-foreground">
          <span className="inline-flex items-center gap-2">
            <Spinner aria-hidden className="size-4" />
            {t('researchGroup.detail.loading')}
          </span>
        </div>
      )}

      {!isLoading && !errorMessage && group && (
        <div className="space-y-4">
          <ResearchGroupDetailToolbar
            canManageResearchers={canManageResearchers}
            group={group}
            onDeleted={deleteGroup}
          />

          <ResearchGroupOverviewSection group={group} />

          <ResearchGroupProjectsSection
            assignedProjects={assignedProjects}
            canCreateProjects={canCreateProjects}
            hasNextProjectsPage={hasNextProjectsPage}
            isFetchingNextProjectsPage={isFetchingNextProjectsPage}
            isLoadingProjects={isLoadingProjects}
            onCreateProject={createProject}
            onLoadMoreProjects={loadMoreProjects}
            onOpenProject={openProject}
            onToggleArchivedProjects={toggleArchivedProjects}
            showArchivedProjects={showArchivedProjects}
          />

          <ResearchGroupMembersSection canManageResearchers={canManageResearchers} group={group} />
        </div>
      )}
    </PageContainer>
  );
}
