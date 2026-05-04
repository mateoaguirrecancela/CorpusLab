import { useEffect, useMemo, useState } from 'react';
import { Plus } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { toast } from 'sonner';
import { EntitySummaryCard } from '@/components/common/EntitySummaryCard';
import { PageContainer } from '@/components/common/PageContainer';
import { PageHeader } from '@/components/common/PageHeader';
import { Button } from '@/components/ui/button';
import { Spinner } from '@/components/ui/spinner';
import { useMyAssignedProjectsQuery } from '@/modules/project/hooks/useProjectQueries';
import { getProjectsLoadErrorMessage } from '@/modules/project/services/projectService';

const TEXT_BUTTON_CLASS =
  'inline-flex appearance-none items-center gap-2 border-0 bg-transparent p-0 text-sm font-semibold text-muted-foreground shadow-none cursor-pointer hover:text-muted-foreground disabled:cursor-not-allowed disabled:opacity-60';

export default function ProjectsPage() {
  const { t } = useTranslation();
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

  const actions = (
    <div className="flex items-center gap-4">
      <button
        aria-pressed={showArchived}
        className={TEXT_BUTTON_CLASS}
        onClick={() => setShowArchived((current) => !current)}
        type="button"
      >
        {showArchived ? t('project.list.viewActive') : t('project.list.viewArchived')}
      </button>

      <Button
        className="h-10 rounded-md bg-primary px-4 text-sm font-semibold text-white hover:bg-primary-strong cursor-pointer"
        onClick={() => navigate('/home/projects/create')}
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

      <div className="mt-6">
        {isLoading && (
          <div className="text-sm text-muted-foreground">
            <span className="inline-flex items-center gap-2">
              <Spinner aria-hidden className="size-4" />
              {t('project.list.loading')}
            </span>
          </div>
        )}

        {!isLoading && errorMessage.length === 0 && projects.length === 0 && (
          <div className="text-sm text-muted-foreground">
            {showArchived ? t('project.list.emptyArchived') : t('project.list.empty')}
          </div>
        )}

        {!isLoading && projects.length > 0 && (
          <>
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
              {projects.map((project) => {
                const roleLabel =
                  project.participantRole === 'CREATOR'
                    ? t('project.list.roles.creator')
                    : t('project.list.roles.participant');

                return (
                  <EntitySummaryCard
                    actionLabel={t('project.list.openProject')}
                    completionPercentage={project.completionPercentage}
                    description={project.description}
                    footerMeta={{
                      kind: 'research-group',
                      text: project.researchGroupName,
                    }}
                    key={project.id}
                    onAction={() => navigate(`/home/projects/${project.id}`)}
                    roleLabel={roleLabel}
                    role={project.participantRole}
                    title={project.name}
                  />
                );
              })}
            </div>

            {hasNextPage && (
              <div className="mt-6 flex justify-center">
                <button
                  className={TEXT_BUTTON_CLASS}
                  disabled={isFetchingNextPage}
                  onClick={() => void fetchNextPage()}
                  type="button"
                >
                  {isFetchingNextPage && <Spinner aria-hidden className="size-4" />}
                  {isFetchingNextPage ? t('project.list.loadingMore') : t('project.list.loadMore')}
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </PageContainer>
  );
}
