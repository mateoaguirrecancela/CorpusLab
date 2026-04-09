import { useEffect, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router';
import { toast } from 'sonner';
import { BackButton } from '@/components/common/BackButton';
import { PageContainer } from '@/components/common/PageContainer';
import { Spinner } from '@/components/ui/spinner';
import { useProjectDetailQuery } from '@/modules/project/hooks/useProjectQueries';
import { getProjectDetailLoadErrorMessage } from '@/modules/project/services/projectService';

function formatDate(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '-';
  }
  return date.toLocaleString();
}

export default function ProjectDetailPage() {
  const { t } = useTranslation();
  const { projectId } = useParams();

  const numericProjectId = useMemo(() => Number(projectId), [projectId]);
  const isInvalidProjectId = !Number.isFinite(numericProjectId) || numericProjectId <= 0;

  const { data: project, isLoading, isError, error } = useProjectDetailQuery(numericProjectId);

  let errorMessage = '';
  if (isInvalidProjectId) {
    errorMessage = t('project.detail.invalidId');
  } else if (isError) {
    errorMessage = getProjectDetailLoadErrorMessage(error);
  }

  useEffect(() => {
    if (errorMessage.length > 0) {
      toast.error(errorMessage, { id: 'project-detail-load-error' });
    }
  }, [errorMessage]);

  let guidelineContent: string;
  if (project?.guidelineText) {
    guidelineContent = project.guidelineText;
  } else if (project?.guidelinePdfBase64) {
    guidelineContent = t('project.detail.guidelinePdfAttached');
  } else {
    guidelineContent = t('project.detail.noGuideline');
  }

  return (
    <PageContainer className="py-4 sm:py-6">
      <div className="space-y-4">
        <div className="flex items-center justify-between gap-3">
          <BackButton fallbackTo="/home/projects" />
        </div>

        {isLoading && (
          <div className="rounded-lg border border-border bg-surface-base px-4 py-6 text-sm text-muted-foreground">
            <span className="inline-flex items-center gap-2">
              <Spinner aria-hidden className="size-4" />
              {t('project.detail.loading')}
            </span>
          </div>
        )}

        {!isLoading && !errorMessage && project && (
          <div className="space-y-4">
            <section className="rounded-md border border-border bg-surface-base p-6 sm:p-7">
              <p className="text-xs font-bold uppercase tracking-wider text-muted-foreground">
                {project.researchGroupName}
              </p>
              <h1 className="mt-2 text-3xl font-black tracking-tight text-primary sm:text-4xl">
                {project.name}
              </h1>
              {project.description && (
                <p className="mt-2 text-sm text-muted-foreground">{project.description}</p>
              )}

              <div className="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                <div className="rounded-md border border-border bg-background p-3">
                  <p className="text-xs font-semibold text-muted-foreground uppercase">
                    {t('project.detail.cards.role')}
                  </p>
                  <p className="mt-1 text-sm font-semibold text-primary">
                    {project.participantRole === 'CREATOR'
                      ? t('project.list.roles.creator')
                      : t('project.list.roles.participant')}
                  </p>
                </div>
                <div className="rounded-md border border-border bg-background p-3">
                  <p className="text-xs font-semibold text-muted-foreground uppercase">
                    {t('project.detail.cards.status')}
                  </p>
                  <p className="mt-1 text-sm font-semibold text-primary">
                    {project.setupCompleted
                      ? t('project.list.status.ready')
                      : t('project.list.status.pendingSetup')}
                  </p>
                </div>
                <div className="rounded-md border border-border bg-background p-3">
                  <p className="text-xs font-semibold text-muted-foreground uppercase">
                    {t('project.detail.cards.datasetItems')}
                  </p>
                  <p className="mt-1 text-sm font-semibold text-primary">
                    {project.datasetItemsCount}
                  </p>
                </div>
                <div className="rounded-md border border-border bg-background p-3">
                  <p className="text-xs font-semibold text-muted-foreground uppercase">
                    {t('project.detail.cards.createdAt')}
                  </p>
                  <p className="mt-1 text-sm font-semibold text-primary">
                    {formatDate(project.createdAt)}
                  </p>
                </div>
              </div>
            </section>

            <section className="rounded-md border border-border bg-surface-base p-6 sm:p-7">
              <h2 className="text-lg font-bold text-primary">{t('project.detail.labelsTitle')}</h2>
              {project.labels.length === 0 ? (
                <p className="mt-3 text-sm text-muted-foreground">{t('project.detail.noLabels')}</p>
              ) : (
                <div className="mt-3 flex flex-wrap gap-2">
                  {project.labels.map((label) => (
                    <span
                      className="inline-flex items-center rounded-md border border-border bg-background px-3 py-1 text-xs font-semibold text-primary"
                      key={`${label.name}-${label.color ?? 'none'}`}
                    >
                      {label.name}
                    </span>
                  ))}
                </div>
              )}
            </section>

            <section className="rounded-md border border-border bg-surface-base p-6 sm:p-7">
              <h2 className="text-lg font-bold text-primary">
                {t('project.detail.guidelineTitle')}
              </h2>
              <p className="mt-3 whitespace-pre-wrap text-sm text-muted-foreground">
                {guidelineContent}
              </p>
            </section>
          </div>
        )}
      </div>
    </PageContainer>
  );
}
