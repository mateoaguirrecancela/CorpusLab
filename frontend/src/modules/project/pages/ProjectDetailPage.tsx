import { useEffect, useMemo } from 'react';
import {
  CalendarDays,
  Database,
  ExternalLink,
  FileText,
  FolderKanban,
  Layers,
  Tags,
  Users,
} from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router';
import { toast } from 'sonner';
import { BackButton } from '@/components/common/BackButton';
import { PageContainer } from '@/components/common/PageContainer';
import { Button } from '@/components/ui/button';
import { Spinner } from '@/components/ui/spinner';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { useProjectDetailQuery } from '@/modules/project/hooks/useProjectQueries';
import { getProjectDetailLoadErrorMessage } from '@/modules/project/services/projectService';
import type { ProjectParticipantRole, ProjectType } from '@/modules/project/types/project';
import {
  completionColor,
  formatDate,
  normalizeCompletionPercentage,
} from '@/modules/project/utils/projectUtils';

function projectTypeI18nKey(type: ProjectType): string {
  const map: Record<ProjectType, string> = {
    TEXT_CLASSIFICATION_SIMPLE: 'project.create.projectTypes.textClassificationSimple',
    TEXT_CLASSIFICATION_MULTILABEL: 'project.create.projectTypes.textClassificationMultiLabel',
    NER: 'project.create.projectTypes.ner',
    SEQ2SEQ: 'project.create.projectTypes.seq2seq',
  };
  return map[type];
}

function participantRoleI18nKey(role: ProjectParticipantRole): string {
  if (role === 'CREATOR') {
    return 'project.list.roles.creator';
  }

  return 'project.list.roles.participant';
}

function getParticipantInitials(firstName: string, lastName: string): string {
  const firstInitial = firstName.trim().charAt(0).toUpperCase();
  const lastInitial = lastName.trim().charAt(0).toUpperCase();

  return `${firstInitial}${lastInitial}`.trim();
}

function getRoleBadgeClasses(role: ProjectParticipantRole): string {
  if (role === 'CREATOR') {
    return 'bg-accent text-primary';
  }

  return 'bg-background text-muted-foreground';
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

  const completionPercentage = normalizeCompletionPercentage(project?.completionPercentage ?? 0);
  const colors = completionColor(completionPercentage);

  const openGuidelinePdf = () => {
    if (!project?.guidelinePdfBase64) {
      return;
    }

    try {
      const binary = globalThis.atob(project.guidelinePdfBase64);
      const bytes = new Uint8Array(binary.length);
      for (let index = 0; index < binary.length; index += 1) {
        bytes[index] = binary.codePointAt(index) ?? 0;
      }

      const blobUrl = URL.createObjectURL(new Blob([bytes], { type: 'application/pdf' }));
      globalThis.open(blobUrl, '_blank', 'noopener,noreferrer');

      setTimeout(() => {
        URL.revokeObjectURL(blobUrl);
      }, 60_000);
    } catch {
      toast.error(t('project.detail.openGuidelinePdfError'));
    }
  };

  return (
    <PageContainer className="py-4 sm:py-8 max-w-5xl mx-auto space-y-4">
      <div className="flex items-center">
        <BackButton fallbackTo="/home/projects" />
      </div>

      {isLoading && (
        <div className="flex min-h-[40vh] items-center justify-center">
          <span className="inline-flex items-center gap-2 text-sm text-muted-foreground">
            <Spinner aria-hidden className="size-4" />
            {t('project.detail.loading')}
          </span>
        </div>
      )}

      {!isLoading && !errorMessage && project && (
        <div className="reveal space-y-4">
          <div className="grid gap-4">
            <section className="stagger rounded-xl border border-border bg-surface-base p-6 sm:p-8 space-y-4">
              <div>
                <h1 className="text-3xl font-black tracking-tight text-primary sm:text-4xl text-balance">
                  {project.name}
                </h1>
                {project.description && (
                  <p className="mt-3 max-w-3xl text-base leading-relaxed text-muted-foreground text-balance">
                    {project.description}
                  </p>
                )}
              </div>
              <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4 pt-2">
                <div>
                  <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-1.5">
                    {t('project.detail.cards.group')}
                  </p>
                  <p className="flex items-center gap-2 text-sm font-medium text-foreground">
                    <FolderKanban className="size-4 text-primary" />
                    {project.researchGroupName}
                  </p>
                </div>
                <div>
                  <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-1.5">
                    {t('project.detail.cards.type')}
                  </p>
                  <p className="flex items-center gap-2 text-sm font-medium text-foreground">
                    <Layers className="size-4 text-primary" />
                    {t(projectTypeI18nKey(project.projectType))}
                  </p>
                </div>
                <div>
                  <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-1.5">
                    {t('project.detail.cards.datasetItems')}
                  </p>
                  <p className="flex items-center gap-2 text-sm font-medium text-foreground">
                    <Database className="size-4 text-primary" />
                    {project.datasetItemsCount}
                  </p>
                </div>
                <div>
                  <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-1.5">
                    {t('project.detail.cards.createdAt')}
                  </p>
                  <p className="flex items-center gap-2 text-sm font-medium text-foreground">
                    <CalendarDays className="size-4 text-primary" />
                    {formatDate(project.createdAt)}
                  </p>
                </div>
              </div>

              <div className="pt-4">
                <div className="flex items-baseline justify-between gap-2 mb-2">
                  <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                    {t('project.detail.cards.completion')}
                  </p>
                  <span className={`text-sm font-bold ${colors.text}`}>
                    {completionPercentage}%
                  </span>
                </div>
                <div className="h-2.5 w-full overflow-hidden rounded-full bg-muted/60">
                  <div
                    className={`h-full rounded-full transition-all duration-700 ease-out ${colors.bar}`}
                    style={{ width: `${completionPercentage}%` }}
                  />
                </div>
              </div>
            </section>

            <section className="stagger rounded-xl border border-border bg-surface-base p-6 sm:p-8 space-y-4">
              <h2 className="flex items-center gap-2 text-lg font-bold text-primary mb-2">
                <Users className="size-5" />
                {t('project.detail.participantsTitle')}
              </h2>

              <div className="pt-2">
                {project.participants.length === 0 ? (
                  <p className="text-sm text-muted-foreground">
                    {t('project.detail.participantsEmpty')}
                  </p>
                ) : (
                  <div className="rounded-md border border-border bg-surface-base">
                    <Table>
                      <TableHeader className="bg-muted/40">
                        <TableRow className="hover:bg-transparent">
                          <TableHead className="px-4 py-3 text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                            {t('project.detail.participantsName')}
                          </TableHead>
                          <TableHead className="px-4 py-3 text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                            {t('project.detail.participantsRole')}
                          </TableHead>
                          <TableHead className="px-4 py-3 text-right text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                            {t('project.detail.participantsCompletion')}
                          </TableHead>
                        </TableRow>
                      </TableHeader>

                      <TableBody>
                        {project.participants.map((participant) => {
                          const participantCompletion = normalizeCompletionPercentage(
                            participant.completionPercentage,
                          );

                          return (
                            <TableRow className="hover:bg-transparent" key={participant.userId}>
                              <TableCell className="px-4 py-3">
                                <div className="flex items-center gap-3">
                                  <div className="flex size-8 items-center justify-center rounded-full bg-accent text-xs font-bold text-primary">
                                    {getParticipantInitials(
                                      participant.firstName,
                                      participant.lastName,
                                    )}
                                  </div>
                                  <div>
                                    <p className="text-sm font-semibold text-primary">
                                      {participant.firstName} {participant.lastName}
                                    </p>
                                    <p className="text-xs text-muted-foreground">
                                      {participant.email}
                                    </p>
                                  </div>
                                </div>
                              </TableCell>

                              <TableCell className="px-4 py-3">
                                <span
                                  className={`inline-flex rounded-md px-2 py-1 text-xs font-semibold ${getRoleBadgeClasses(participant.role)}`}
                                >
                                  {t(participantRoleI18nKey(participant.role))}
                                </span>
                              </TableCell>

                              <TableCell className="px-4 py-3 text-right text-sm font-semibold text-muted-foreground tabular-nums">
                                {participantCompletion}%
                              </TableCell>
                            </TableRow>
                          );
                        })}
                      </TableBody>
                    </Table>
                  </div>
                )}
              </div>
            </section>

            <section className="stagger rounded-xl border border-border bg-surface-base p-6 sm:p-8 space-y-4">
              <h2 className="flex items-center gap-2 text-lg font-bold text-primary mb-2">
                <Tags className="size-5" />
                {t('project.detail.labelsTitle')}
              </h2>
              <div className="pt-2">
                {project.labels.length === 0 ? (
                  <p className="text-sm text-muted-foreground">{t('project.detail.noLabels')}</p>
                ) : (
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    {project.labels.map((label) => (
                      <div
                        className="flex items-center gap-3 rounded-lg border p-3"
                        key={`${label.name}-${label.color ?? 'none'}`}
                        style={{
                          backgroundColor: label.color ? `${label.color}0A` : undefined,
                          borderColor: label.color ? `${label.color}40` : undefined,
                        }}
                      >
                        {label.color && (
                          <span
                            className="inline-block size-3 rounded-full shrink-0"
                            style={{ backgroundColor: label.color }}
                          />
                        )}
                        <span
                          className="font-semibold text-sm truncate"
                          style={{ color: label.color ?? undefined }}
                        >
                          {label.name}
                        </span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </section>

            <section className="stagger rounded-xl border border-border bg-surface-base p-6 sm:p-8 space-y-4">
              <h2 className="flex items-center gap-2 text-lg font-bold text-primary mb-2">
                <FileText className="size-5" />
                {t('project.detail.guidelineTitle')}
              </h2>
              <div className="pt-2">
                {project.guidelineText && (
                  <div className="rounded-lg border border-border bg-surface-base p-5">
                    <p className="whitespace-pre-wrap text-sm leading-relaxed text-muted-foreground">
                      {project.guidelineText}
                    </p>
                  </div>
                )}

                {!project.guidelineText && project.guidelinePdfBase64 && (
                  <div className="flex items-center justify-between gap-4 rounded-lg border border-border bg-surface-base p-4">
                    <div className="flex items-center gap-3">
                      <div className="flex size-10 items-center justify-center rounded-lg bg-danger-soft text-danger-border">
                        <FileText className="size-5 text-red-600" />
                      </div>
                      <div>
                        <p className="text-sm font-medium text-foreground">
                          {t('project.detail.guidelineDocument')}
                        </p>
                        <p className="text-xs text-muted-foreground">
                          {t('project.detail.guidelinePdfFile')}
                        </p>
                      </div>
                    </div>
                    <Button
                      className="h-9 shrink-0 cursor-pointer gap-1.5 rounded-md bg-transparent border border-border px-3 text-xs font-semibold text-foreground hover:bg-surface-soft hover:text-primary"
                      onClick={openGuidelinePdf}
                      type="button"
                    >
                      <ExternalLink className="size-3.5" />
                      {t('project.detail.openGuidelinePdf')}
                    </Button>
                  </div>
                )}

                {!project.guidelineText && !project.guidelinePdfBase64 && (
                  <p className="text-sm text-muted-foreground">{t('project.detail.noGuideline')}</p>
                )}
              </div>
            </section>
          </div>
        </div>
      )}
    </PageContainer>
  );
}
