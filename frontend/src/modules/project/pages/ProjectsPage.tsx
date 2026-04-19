import { useEffect } from 'react';
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

export default function ProjectsPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const { data: projects = [], isLoading, isError, error } = useMyAssignedProjectsQuery();
  const errorMessage = isError ? getProjectsLoadErrorMessage(error) : '';

  useEffect(() => {
    if (errorMessage.length > 0) {
      toast.error(errorMessage, { id: 'projects-load-error' });
    }
  }, [errorMessage]);

  const actions = (
    <Button
      className="h-10 rounded-md bg-primary px-4 text-sm font-semibold text-white hover:bg-primary-strong cursor-pointer"
      onClick={() => navigate('/home/projects/create')}
      type="button"
    >
      <Plus className="size-4" />
      {t('project.list.newProject')}
    </Button>
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
          <div className="text-sm text-muted-foreground">{t('project.list.empty')}</div>
        )}

        {!isLoading && projects.length > 0 && (
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
        )}
      </div>
    </PageContainer>
  );
}
