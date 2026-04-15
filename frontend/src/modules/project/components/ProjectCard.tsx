import { ArrowRight, FolderKanban } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { type ProjectAssignedSummary } from '@/modules/project/types/project';
import {
  completionBadgeClassName,
  normalizeCompletionPercentage,
} from '@/modules/project/utils/projectUtils';

type ProjectCardProps = Readonly<{
  onOpen: (projectId: number) => void;
  project: ProjectAssignedSummary;
  showGroupName?: boolean;
}>;

export function ProjectCard({ onOpen, project, showGroupName = true }: ProjectCardProps) {
  const { t } = useTranslation();
  const completionPercentage = normalizeCompletionPercentage(project.completionPercentage);
  const completionBadgeClass = completionBadgeClassName(completionPercentage);

  return (
    <article className="stagger flex h-full flex-col justify-between rounded-2xl border border-border bg-surface-base p-5 transition-all duration-200 hover:-translate-y-0.5 hover:border-primary/30 hover:shadow-(--shadow-card-hover)">
      <div>
        <div className="flex flex-wrap items-center justify-between gap-2">
          <span className="inline-flex items-center rounded-md bg-primary px-2.5 py-1 text-[11px] font-bold tracking-wider text-white uppercase">
            {project.participantRole === 'CREATOR'
              ? t('project.list.roles.creator')
              : t('project.list.roles.participant')}
          </span>
          <span
            className={[
              'inline-flex items-center rounded-md px-2.5 py-1 text-[11px] font-bold tracking-wider uppercase',
              completionBadgeClass,
            ].join(' ')}
          >
            {t('project.list.completionBadge', { value: completionPercentage })}
          </span>
        </div>

        <h3 className="mt-4 text-lg font-black leading-tight text-primary">{project.name}</h3>

        {project.description ? (
          <p className="mt-2 line-clamp-2 text-sm leading-relaxed text-muted-foreground">
            {project.description}
          </p>
        ) : (
          <p className="mt-2 text-sm italic text-muted-foreground">
            {t('project.list.noDescription')}
          </p>
        )}
      </div>

      <div className="mt-5 flex items-center justify-between border-t border-border pt-4">
        {showGroupName && (
          <p className="inline-flex items-center gap-2 text-xs font-medium text-muted-foreground">
            <FolderKanban className="size-3.5" />
            {project.researchGroupName}
          </p>
        )}

        <button
          className="group ml-auto inline-flex cursor-pointer items-center gap-1 text-sm font-semibold text-primary transition-colors hover:text-primary-strong"
          onClick={() => onOpen(project.id)}
          type="button"
        >
          {t('project.list.openProject')}
          <ArrowRight className="size-3.5 transition-transform group-hover:translate-x-0.5" />
        </button>
      </div>
    </article>
  );
}
