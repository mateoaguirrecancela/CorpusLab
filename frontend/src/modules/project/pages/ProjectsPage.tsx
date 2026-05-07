import { Plus } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { PageContainer } from '@/components/common/PageContainer';
import { PageHeader } from '@/components/common/PageHeader';
import { TEXT_BUTTON_CLASS } from '@/components/common/textButtonClass';
import { Button } from '@/components/ui/button';
import { ProjectsListContent } from '@/modules/project/components/ProjectsListContent';
import { useProjectsPage } from '@/modules/project/hooks/useProjectsPage';

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
      <button
        aria-pressed={showArchived}
        className={TEXT_BUTTON_CLASS}
        onClick={toggleArchived}
        type="button"
      >
        {showArchived ? t('project.list.viewActive') : t('project.list.viewArchived')}
      </button>

      <Button
        className="h-10 rounded-md bg-primary px-4 text-sm font-semibold text-white hover:bg-primary-strong cursor-pointer"
        onClick={openCreateProject}
        type="button"
      >
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
