import { Plus } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { PageContainer } from '@/components/common/PageContainer';
import { PageHeader } from '@/components/common/PageHeader';
import { Button } from '@/components/ui/button';
import { ProjectsListContent } from '@/modules/project/core/components/ProjectsListContent';
import { useProjectsPage } from '@/modules/project/core/hooks/useProjectsPage';

export default function ProjectsPage() {
  const { t } = useTranslation();
  const {
    errorMessage,
    hasNextPage,
    isFetchingNextPage,
    isLoading,
    loadMoreProjects,
    openCreateProject,
    openProject,
    projects,
    showArchived,
    toggleArchived,
  } = useProjectsPage();

  const actions = (
    <div className="flex items-center gap-4">
      <Button
        aria-pressed={showArchived}
        onClick={toggleArchived}
        size="text"
        type="button"
        variant="text"
      >
        {showArchived ? t('project.list.viewActive') : t('project.list.viewArchived')}
      </Button>

      <Button onClick={openCreateProject} size="action" type="button" variant="primaryAction">
        <Plus className="size-4" />
        {t('project.list.newProject')}
      </Button>
    </div>
  );

  return (
    <PageContainer>
      <PageHeader actions={actions} title={t('project.list.pageTitle')} />

      <ProjectsListContent
        errorMessage={errorMessage}
        hasNextPage={hasNextPage}
        isFetchingNextPage={isFetchingNextPage}
        isLoading={isLoading}
        onLoadMore={loadMoreProjects}
        onOpenProject={openProject}
        projects={projects}
        showArchived={showArchived}
      />
    </PageContainer>
  );
}
