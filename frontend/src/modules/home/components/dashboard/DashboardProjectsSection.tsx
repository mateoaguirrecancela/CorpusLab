import { useTranslation } from 'react-i18next';
import { EntitySummaryCard } from '@/components/common/EntitySummaryCard';
import { DashboardPanel } from '@/modules/home/components/dashboard/DashboardPanel';
import { type DashboardProject } from '@/modules/home/types/dashboard';
import { participantRoleI18nKey } from '@/modules/project/utils/projectDisplayUtils';

type DashboardProjectsSectionProps = Readonly<{
  advancedProjects: DashboardProject[];
  errorMessage: string;
  isLoading: boolean;
  recentProjects: DashboardProject[];
  onOpenProject: (projectId: number) => void;
  onOpenProjects: () => void;
}>;

type ProjectGridProps = Readonly<{
  emptyLabel: string;
  projects: DashboardProject[];
  onOpenProject: (projectId: number) => void;
}>;

function ProjectGrid({ emptyLabel, projects, onOpenProject }: ProjectGridProps) {
  const { t } = useTranslation();

  if (projects.length === 0) {
    return (
      <p className="rounded-lg border border-dashed border-border bg-background px-4 py-5 text-sm text-muted-foreground">
        {emptyLabel}
      </p>
    );
  }

  return (
    <div className="grid gap-4 sm:grid-cols-2">
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
  );
}

function ProjectSectionLoading() {
  const { t } = useTranslation();

  return (
    <div className="rounded-xl border border-border bg-surface-base px-4 py-5 text-sm text-muted-foreground">
      {t('home.dashboard.loading.projects')}
    </div>
  );
}

export function DashboardProjectsSection({
  advancedProjects,
  errorMessage,
  isLoading,
  recentProjects,
  onOpenProject,
  onOpenProjects,
}: DashboardProjectsSectionProps) {
  const { t } = useTranslation();

  if (isLoading) {
    return <ProjectSectionLoading />;
  }

  if (errorMessage.length > 0) {
    return (
      <div className="rounded-xl border border-danger-border bg-danger-soft px-4 py-5 text-sm font-medium text-destructive">
        {errorMessage}
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <DashboardPanel
        action={
          <button
            className="cursor-pointer text-sm font-semibold text-primary hover:text-primary-strong"
            onClick={onOpenProjects}
            type="button"
          >
            {t('home.dashboard.projects.viewAll')}
          </button>
        }
        title={t('home.dashboard.projects.recentTitle')}
      >
        <ProjectGrid
          emptyLabel={t('home.dashboard.empty.recentProjects')}
          projects={recentProjects}
          onOpenProject={onOpenProject}
        />
      </DashboardPanel>

      <DashboardPanel title={t('home.dashboard.projects.advancedTitle')}>
        <ProjectGrid
          emptyLabel={t('home.dashboard.empty.advancedProjects')}
          projects={advancedProjects}
          onOpenProject={onOpenProject}
        />
      </DashboardPanel>
    </div>
  );
}
