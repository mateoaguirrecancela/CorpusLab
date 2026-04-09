import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { toast } from 'sonner';
import { PageContainer } from '@/components/common/PageContainer';
import { PageHeader } from '@/components/common/PageHeader';
import { Spinner } from '@/components/ui/spinner';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { useMyAssignedProjectsQuery } from '@/modules/project/hooks/useProjectQueries';
import { getProjectsLoadErrorMessage } from '@/modules/project/services/projectService';

function formatDate(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '-';
  }
  return date.toLocaleDateString();
}

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

  return (
    <PageContainer>
      <PageHeader title={t('project.list.pageTitle')} />

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
          <div className="rounded-md border border-border bg-surface-base">
            <Table>
              <TableHeader className="bg-muted/40">
                <TableRow className="hover:bg-transparent">
                  <TableHead className="px-4 py-3 text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                    {t('project.list.columns.name')}
                  </TableHead>
                  <TableHead className="px-4 py-3 text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                    {t('project.list.columns.group')}
                  </TableHead>
                  <TableHead className="px-4 py-3 text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                    {t('project.list.columns.role')}
                  </TableHead>
                  <TableHead className="px-4 py-3 text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                    {t('project.list.columns.status')}
                  </TableHead>
                  <TableHead className="px-4 py-3 text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                    {t('project.list.columns.createdAt')}
                  </TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {projects.map((project) => (
                  <TableRow
                    className="cursor-pointer hover:bg-accent/40"
                    key={project.id}
                    onClick={() => navigate(`/home/projects/${project.id}`)}
                  >
                    <TableCell className="px-4 py-3">
                      <p className="text-sm font-semibold text-primary">{project.name}</p>
                      {project.description && (
                        <p className="text-xs text-muted-foreground line-clamp-1">
                          {project.description}
                        </p>
                      )}
                    </TableCell>
                    <TableCell className="px-4 py-3 text-sm text-muted-foreground">
                      {project.researchGroupName}
                    </TableCell>
                    <TableCell className="px-4 py-3 text-sm text-muted-foreground">
                      {project.participantRole === 'CREATOR'
                        ? t('project.list.roles.creator')
                        : t('project.list.roles.participant')}
                    </TableCell>
                    <TableCell className="px-4 py-3 text-sm text-muted-foreground">
                      {project.setupCompleted
                        ? t('project.list.status.ready')
                        : t('project.list.status.pendingSetup')}
                    </TableCell>
                    <TableCell className="px-4 py-3 text-sm text-muted-foreground">
                      {formatDate(project.createdAt)}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </div>
    </PageContainer>
  );
}
