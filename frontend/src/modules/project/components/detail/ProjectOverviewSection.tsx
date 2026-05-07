import { CalendarDays, FolderKanban, Layers } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { type ProjectDetail } from '@/modules/project/types/project';
import { projectTypeI18nKey } from '@/modules/project/utils/projectDisplayUtils';
import { formatDate } from '@/modules/project/utils/projectUtils';

type ProjectOverviewSectionProps = Readonly<{
  colors: Readonly<{
    bar: string;
    text: string;
  }>;
  completionPercentage: number;
  project: ProjectDetail;
}>;

export function ProjectOverviewSection({
  colors,
  completionPercentage,
  project,
}: ProjectOverviewSectionProps) {
  const { t } = useTranslation();

  return (
    <section className="rounded-xl border border-border bg-surface-base p-6 sm:p-8">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h1 className="text-3xl font-black tracking-tight text-primary sm:text-4xl">
            {project.name}
          </h1>
          {project.description && (
            <p className="mt-3 max-w-3xl text-base leading-relaxed text-muted-foreground">
              {project.description}
            </p>
          )}
        </div>
      </div>

      <div className="flex flex-col gap-6 pt-6 md:flex-row md:justify-between">
        <div>
          <p className="mb-1.5 text-xs font-semibold tracking-wider text-muted-foreground uppercase">
            {t('project.detail.cards.group')}
          </p>
          <p className="flex items-center gap-2 text-sm font-medium text-foreground">
            <FolderKanban className="size-4 text-primary" />
            {project.researchGroupName}
          </p>
        </div>
        <div>
          <p className="mb-1.5 text-xs font-semibold tracking-wider text-muted-foreground uppercase">
            {t('project.detail.cards.type')}
          </p>
          <p className="flex items-center gap-2 text-sm font-medium text-foreground">
            <Layers className="size-4 text-primary" />
            {t(projectTypeI18nKey(project.projectType))}
          </p>
        </div>
        <div>
          <p className="mb-1.5 text-xs font-semibold tracking-wider text-muted-foreground uppercase">
            {t('project.detail.cards.createdAt')}
          </p>
          <p className="flex items-center gap-2 text-sm font-medium text-foreground">
            <CalendarDays className="size-4 text-primary" />
            {formatDate(project.createdAt)}
          </p>
        </div>
      </div>

      <div className="pt-6">
        <div className="mb-2 flex items-baseline justify-between gap-2">
          <p className="text-xs font-semibold tracking-wider text-muted-foreground uppercase">
            {t('project.detail.cards.completion')}
          </p>
          <span className={`text-sm font-bold ${colors.text}`}>{completionPercentage}%</span>
        </div>
        <div className="h-2.5 w-full overflow-hidden rounded-full bg-muted/60">
          <div
            className={`h-full rounded-full transition-all duration-700 ease-out ${colors.bar}`}
            style={{ width: `${completionPercentage}%` }}
          />
        </div>
      </div>
    </section>
  );
}
