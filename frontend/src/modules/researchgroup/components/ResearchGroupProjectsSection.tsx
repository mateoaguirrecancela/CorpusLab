import { FlaskConical, Plus } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { EntitySummaryCard } from '@/components/common/EntitySummaryCard';
import { TEXT_BUTTON_CLASS } from '@/components/common/textButtonClass';
import { Button } from '@/components/ui/button';
import { Spinner } from '@/components/ui/spinner';
import { type ProjectAssignedSummary } from '@/modules/project/types/project';
import { participantRoleI18nKey } from '@/modules/project/utils/projectDisplayUtils';

type ResearchGroupProjectsSectionProps = Readonly<{
  assignedProjects: ProjectAssignedSummary[];
  canCreateProjects: boolean;
  hasNextProjectsPage: boolean;
  isFetchingNextProjectsPage: boolean;
  isLoadingProjects: boolean;
  showArchivedProjects: boolean;
  onCreateProject: () => void;
  onLoadMoreProjects: () => void;
  onOpenProject: (projectId: number) => void;
  onToggleArchivedProjects: () => void;
}>;

type ResearchGroupProjectsContentProps = Readonly<{
  assignedProjects: ProjectAssignedSummary[];
  hasNextProjectsPage: boolean;
  isFetchingNextProjectsPage: boolean;
  isLoadingProjects: boolean;
  showArchivedProjects: boolean;
  onLoadMoreProjects: () => void;
  onOpenProject: (projectId: number) => void;
}>;

function ResearchGroupProjectsLoading() {
  const { t } = useTranslation();

  return (
    <div className="rounded-md border border-border bg-surface-base px-4 py-6 text-sm text-muted-foreground">
      <span className="inline-flex items-center gap-2">
        <Spinner aria-hidden className="size-4" />
        {t('project.list.loading')}
      </span>
    </div>
  );
}

function ResearchGroupProjectsEmpty({
  showArchivedProjects,
}: Readonly<{ showArchivedProjects: boolean }>) {
  const { t } = useTranslation();

  return (
    <p className="rounded-md border border-dashed border-border bg-surface-base px-4 py-5 text-sm text-muted-foreground">
      {showArchivedProjects ? t('project.list.emptyArchived') : t('project.list.empty')}
    </p>
  );
}

function ResearchGroupProjectsGrid({
  assignedProjects,
  hasNextProjectsPage,
  isFetchingNextProjectsPage,
  onLoadMoreProjects,
  onOpenProject,
}: Omit<ResearchGroupProjectsContentProps, 'isLoadingProjects' | 'showArchivedProjects'>) {
  const { t } = useTranslation();

  return (
    <>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {assignedProjects.map((project) => (
          <EntitySummaryCard
            actionLabel={t('project.list.openProject')}
            completionPercentage={project.completionPercentage}
            description={project.description}
            key={project.id}
            onAction={() => onOpenProject(project.id)}
            role={project.participantRole}
            roleLabel={t(participantRoleI18nKey(project.participantRole))}
            title={project.name}
          />
        ))}
      </div>

      {hasNextProjectsPage && (
        <div className="mt-6 flex justify-center">
          <button
            className={TEXT_BUTTON_CLASS}
            disabled={isFetchingNextProjectsPage}
            onClick={onLoadMoreProjects}
            type="button"
          >
            {isFetchingNextProjectsPage && <Spinner aria-hidden className="size-4" />}
            {isFetchingNextProjectsPage
              ? t('project.list.loadingMore')
              : t('project.list.loadMore')}
          </button>
        </div>
      )}
    </>
  );
}

function ResearchGroupProjectsContent({
  assignedProjects,
  hasNextProjectsPage,
  isFetchingNextProjectsPage,
  isLoadingProjects,
  showArchivedProjects,
  onLoadMoreProjects,
  onOpenProject,
}: ResearchGroupProjectsContentProps) {
  if (isLoadingProjects) {
    return <ResearchGroupProjectsLoading />;
  }

  if (assignedProjects.length === 0) {
    return <ResearchGroupProjectsEmpty showArchivedProjects={showArchivedProjects} />;
  }

  return (
    <ResearchGroupProjectsGrid
      assignedProjects={assignedProjects}
      hasNextProjectsPage={hasNextProjectsPage}
      isFetchingNextProjectsPage={isFetchingNextProjectsPage}
      onLoadMoreProjects={onLoadMoreProjects}
      onOpenProject={onOpenProject}
    />
  );
}

export function ResearchGroupProjectsSection({
  assignedProjects,
  canCreateProjects,
  hasNextProjectsPage,
  isFetchingNextProjectsPage,
  isLoadingProjects,
  showArchivedProjects,
  onCreateProject,
  onLoadMoreProjects,
  onOpenProject,
  onToggleArchivedProjects,
}: ResearchGroupProjectsSectionProps) {
  const { t } = useTranslation();

  return (
    <section>
      <div className="mb-4 flex items-center justify-between gap-3">
        <h2 className="inline-flex items-center gap-4 text-3xl font-black tracking-tight text-primary">
          <FlaskConical className="size-7" />
          {t('researchGroup.detail.projects')}
        </h2>

        <div className="flex items-center gap-4">
          <button
            aria-pressed={showArchivedProjects}
            className={TEXT_BUTTON_CLASS}
            onClick={onToggleArchivedProjects}
            type="button"
          >
            {showArchivedProjects ? t('project.list.viewActive') : t('project.list.viewArchived')}
          </button>

          {canCreateProjects && (
            <Button
              className="h-10 rounded-md bg-primary px-4 text-sm font-semibold text-white hover:bg-primary-strong cursor-pointer"
              onClick={onCreateProject}
              type="button"
            >
              <Plus className="size-4" />
              {t('researchGroup.detail.newProject')}
            </Button>
          )}
        </div>
      </div>

      <ResearchGroupProjectsContent
        assignedProjects={assignedProjects}
        hasNextProjectsPage={hasNextProjectsPage}
        isFetchingNextProjectsPage={isFetchingNextProjectsPage}
        isLoadingProjects={isLoadingProjects}
        showArchivedProjects={showArchivedProjects}
        onLoadMoreProjects={onLoadMoreProjects}
        onOpenProject={onOpenProject}
      />
    </section>
  );
}
