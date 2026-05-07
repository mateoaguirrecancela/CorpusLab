import { useTranslation } from 'react-i18next';
import { EntitySummaryCard } from '@/components/common/EntitySummaryCard';
import { TEXT_BUTTON_CLASS } from '@/components/common/textButtonClass';
import { Spinner } from '@/components/ui/spinner';
import { type ProjectAssignedSummary } from '@/modules/project/types/project';
import { participantRoleI18nKey } from '@/modules/project/utils/projectDisplayUtils';

type ProjectsListContentProps = Readonly<{
  errorMessage: string;
  hasNextPage: boolean;
  isFetchingNextPage: boolean;
  isLoading: boolean;
  projects: ProjectAssignedSummary[];
  showArchived: boolean;
  onLoadMore: () => void;
  onOpenProject: (projectId: number) => void;
}>;

export function ProjectsListContent({
  errorMessage,
  hasNextPage,
  isFetchingNextPage,
  isLoading,
  projects,
  showArchived,
  onLoadMore,
  onOpenProject,
}: ProjectsListContentProps) {
  const { t } = useTranslation();

  return (
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
              const roleLabel = t(participantRoleI18nKey(project.participantRole));

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
                  onAction={() => onOpenProject(project.id)}
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
                onClick={onLoadMore}
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
  );
}
