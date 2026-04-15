import { useEffect } from 'react';
import { Plus } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { toast } from 'sonner';
import { PageContainer } from '@/components/common/PageContainer';
import { PageHeader } from '@/components/common/PageHeader';
import { Button } from '@/components/ui/button';
import { Spinner } from '@/components/ui/spinner';
import { ProjectCard } from '@/modules/project/components/ProjectCard';
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
          <div className="rounded-lg border border-border bg-surface-base px-4 py-6 text-sm text-muted-foreground">
            <span className="inline-flex items-center gap-2">
              <Spinner aria-hidden className="size-4" />
              {t('project.list.loading')}
            </span>
          </div>
        )}

        {!isLoading && errorMessage.length === 0 && projects.length === 0 && (
          <div className="rounded-lg border border-dashed border-border bg-surface-base px-4 py-6 text-center text-sm text-muted-foreground">
            {t('project.list.empty')}
          </div>
        )}

        {!isLoading && projects.length > 0 && (
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            {projects.map((project) => (
              <ProjectCard
                key={project.id}
                onOpen={(projectId) => navigate(`/home/projects/${projectId}`)}
                project={project}
              />
            ))}
          </div>
        )}
      </div>
    </PageContainer>
  );
}
