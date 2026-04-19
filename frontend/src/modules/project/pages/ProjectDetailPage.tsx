import { useEffect, useMemo } from 'react';
import {
  CalendarDays,
  Database,
  Download,
  ExternalLink,
  FileText,
  FolderKanban,
  Layers,
  PenSquare,
  Tags,
  Users,
} from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router';
import { toast } from 'sonner';
import { BackButton } from '@/components/common/BackButton';
import { PageContainer } from '@/components/common/PageContainer';
import { RoleBadge } from '@/components/common/RoleBadge';
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
import {
  getProjectDatasetItemContent,
  getProjectDatasetItemContentErrorMessage,
  getProjectDetailLoadErrorMessage,
} from '@/modules/project/services/projectService';
import { type ProjectParticipantRole, type ProjectType } from '@/modules/project/types/project';
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

function formatFileSize(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes < 1024) {
    return `${Math.max(0, Math.trunc(bytes))} B`;
  }

  const units = ['KB', 'MB', 'GB', 'TB'];
  let value = bytes / 1024;
  let unitIndex = 0;

  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024;
    unitIndex += 1;
  }

  return `${value.toFixed(1)} ${units[unitIndex]}`;
}

function parseBase64FilePayload(
  rawValue: string,
  fallbackMimeType: string,
): { mimeType: string; base64Payload: string } {
  const trimmedValue = rawValue.trim();
  const match = /^data:([^;]+);base64,(.+)$/i.exec(trimmedValue);
  if (!match) {
    return {
      mimeType: fallbackMimeType,
      base64Payload: trimmedValue,
    };
  }

  return {
    mimeType: match[1],
    base64Payload: match[2],
  };
}

function calculateBase64SizeBytes(base64Payload: string): number {
  try {
    return globalThis.atob(base64Payload).length;
  } catch {
    return 0;
  }
}

function decodeBase64ToBuffer(base64Payload: string): ArrayBuffer {
  const binary = globalThis.atob(base64Payload);
  const buffer = new ArrayBuffer(binary.length);
  const bytes = new Uint8Array(buffer);

  for (let index = 0; index < binary.length; index += 1) {
    bytes[index] = binary.codePointAt(index) ?? 0;
  }

  return buffer;
}

function triggerBlobDownload(blob: Blob, fileName: string): void {
  const blobUrl = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = blobUrl;
  anchor.download = fileName;
  anchor.style.display = 'none';

  document.body.append(anchor);
  anchor.click();
  anchor.remove();

  setTimeout(() => {
    URL.revokeObjectURL(blobUrl);
  }, 60_000);
}

export default function ProjectDetailPage() {
  const { t } = useTranslation();
  const { projectId } = useParams();

  const numericProjectId = useMemo(() => Number(projectId), [projectId]);
  const isInvalidProjectId = !Number.isFinite(numericProjectId) || numericProjectId <= 0;

  const { data: project, isLoading, isError, error } = useProjectDetailQuery(numericProjectId);

  const detailErrorMessage = useMemo(() => {
    if (isInvalidProjectId) {
      return t('project.detail.invalidId');
    }

    if (isError) {
      return getProjectDetailLoadErrorMessage(error);
    }

    return '';
  }, [error, isError, isInvalidProjectId, t]);

  useEffect(() => {
    if (detailErrorMessage.length > 0) {
      toast.error(detailErrorMessage, { id: 'project-detail-load-error' });
    }
  }, [detailErrorMessage]);

  const completionPercentage = normalizeCompletionPercentage(project?.completionPercentage ?? 0);
  const colors = completionColor(completionPercentage);

  const guidelinePdfMetadata = useMemo(() => {
    if (!project?.guidelinePdfBase64) {
      return '';
    }

    const { mimeType, base64Payload } = parseBase64FilePayload(
      project.guidelinePdfBase64,
      'application/pdf',
    );
    const sizeBytes = calculateBase64SizeBytes(base64Payload);

    if (sizeBytes <= 0) {
      return mimeType;
    }

    return `${mimeType} - ${formatFileSize(sizeBytes)}`;
  }, [project?.guidelinePdfBase64]);

  const openGuidelinePdf = () => {
    if (!project?.guidelinePdfBase64) {
      return;
    }

    try {
      const { mimeType, base64Payload } = parseBase64FilePayload(
        project.guidelinePdfBase64,
        'application/pdf',
      );

      const buffer = decodeBase64ToBuffer(base64Payload);

      const blobUrl = URL.createObjectURL(new Blob([buffer], { type: mimeType }));
      globalThis.open(blobUrl, '_blank', 'noopener,noreferrer');

      setTimeout(() => {
        URL.revokeObjectURL(blobUrl);
      }, 60_000);
    } catch {
      toast.error(t('project.detail.openGuidelinePdfError'));
    }
  };

  const downloadGuidelinePdf = () => {
    if (!project?.guidelinePdfBase64) {
      return;
    }

    try {
      const { mimeType, base64Payload } = parseBase64FilePayload(
        project.guidelinePdfBase64,
        'application/pdf',
      );
      const buffer = decodeBase64ToBuffer(base64Payload);
      const blob = new Blob([buffer], { type: mimeType });

      triggerBlobDownload(blob, 'guideline.pdf');
    } catch {
      toast.error(t('project.detail.openGuidelinePdfError'));
    }
  };

  const openDatasetFile = async (datasetItemId: number) => {
    if (!project) {
      return;
    }

    try {
      const file = await getProjectDatasetItemContent(project.id, datasetItemId);
      const blobUrl = URL.createObjectURL(file.blob);
      globalThis.open(blobUrl, '_blank', 'noopener,noreferrer');

      setTimeout(() => {
        URL.revokeObjectURL(blobUrl);
      }, 60_000);
    } catch (openError) {
      toast.error(getProjectDatasetItemContentErrorMessage(openError));
    }
  };

  const downloadDatasetFile = async (datasetItemId: number, fallbackFileName: string) => {
    if (!project) {
      return;
    }

    try {
      const file = await getProjectDatasetItemContent(project.id, datasetItemId);
      const normalizedFallbackFileName = fallbackFileName.trim();
      const downloadFileName =
        file.fileName?.trim() || normalizedFallbackFileName || `dataset-item-${datasetItemId}`;

      triggerBlobDownload(file.blob, downloadFileName);
    } catch (downloadError) {
      toast.error(getProjectDatasetItemContentErrorMessage(downloadError));
    }
  };

  return (
    <PageContainer>
      <div className="flex items-center justify-between gap-4">
        <BackButton fallbackTo="/home/projects" />
        {!isLoading && !detailErrorMessage && project && (
          <Link
            className="inline-flex h-10 shrink-0 items-center gap-2 rounded-lg bg-primary px-3 text-sm font-semibold text-primary-foreground transition hover:bg-primary/90"
            to={`/home/projects/${project.id}/annotate`}
          >
            <PenSquare className="size-4" />
            {t('project.detail.openAnnotationWorkspace')}
          </Link>
        )}
      </div>

      {isLoading && (
        <div className="text-sm text-muted-foreground">
          <span className="inline-flex items-center gap-2">
            <Spinner aria-hidden className="size-4" />
            {t('project.detail.loading')}
          </span>
        </div>
      )}

      {!isLoading && !detailErrorMessage && project && (
        <div className="space-y-4">
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

          <section className="rounded-xl border border-border bg-surface-base p-6 sm:p-8">
            <h2 className="mb-4 flex items-center gap-2 text-lg font-bold text-primary">
              <Users className="size-5" />
              {t('project.detail.participantsTitle')}
            </h2>

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
                                <p className="text-xs text-muted-foreground">{participant.email}</p>
                              </div>
                            </div>
                          </TableCell>

                          <TableCell className="px-4 py-3">
                            <RoleBadge
                              label={t(participantRoleI18nKey(participant.role))}
                              role={participant.role}
                            />
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
          </section>

          <section className="rounded-xl border border-border bg-surface-base p-6 sm:p-8">
            <h2 className="mb-4 flex items-center gap-2 text-lg font-bold text-primary">
              <Database className="size-5" />
              {t('project.detail.datasetTitle')}
            </h2>

            {project.datasetItems.length === 0 ? (
              <p className="text-sm text-muted-foreground">{t('project.detail.datasetEmpty')}</p>
            ) : (
              <div className="space-y-3">
                {project.datasetItems.map((item) => (
                  <div
                    className="flex items-center justify-between gap-4 rounded-lg border border-border bg-surface-base p-4"
                    key={item.id}
                  >
                    <div className="flex min-w-0 items-center gap-3">
                      <div className="flex size-10 items-center justify-center rounded-lg bg-danger-soft text-danger-border">
                        <FileText className="size-5 text-red-600" />
                      </div>
                      <div className="min-w-0">
                        <p className="truncate text-sm font-medium text-foreground">
                          {item.fileName}
                        </p>
                        <p className="text-xs text-muted-foreground">
                          {item.mimeType || 'application/octet-stream'} -{' '}
                          {formatFileSize(item.sizeBytes)}
                        </p>
                      </div>
                    </div>
                    <div className="flex shrink-0 items-center gap-2">
                      <Button
                        className="h-9 gap-1.5 rounded-md border border-border bg-transparent px-3 text-xs font-semibold text-foreground hover:bg-surface-soft hover:text-primary cursor-pointer"
                        onClick={() => void downloadDatasetFile(item.id, item.fileName)}
                        type="button"
                      >
                        <Download className="size-3.5" />
                        {t('project.detail.downloadDatasetFile')}
                      </Button>
                      <Button
                        className="h-9 gap-1.5 rounded-md border border-border bg-transparent px-3 text-xs font-semibold text-foreground hover:bg-surface-soft hover:text-primary cursor-pointer"
                        onClick={() => void openDatasetFile(item.id)}
                        type="button"
                      >
                        <ExternalLink className="size-3.5" />
                        {t('project.detail.openDatasetFile')}
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>

          <section className="rounded-xl border border-border bg-surface-base p-6 sm:p-8">
            <h2 className="mb-4 flex items-center gap-2 text-lg font-bold text-primary">
              <Tags className="size-5" />
              {t('project.detail.labelsTitle')}
            </h2>

            {project.labels.length === 0 ? (
              <p className="text-sm text-muted-foreground">{t('project.detail.noLabels')}</p>
            ) : (
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
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
                        className="inline-block size-3 shrink-0 rounded-full"
                        style={{ backgroundColor: label.color }}
                      />
                    )}
                    <span
                      className="truncate text-sm font-semibold"
                      style={{ color: label.color ?? undefined }}
                    >
                      {label.name}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </section>

          <section className="rounded-xl border border-border bg-surface-base p-6 sm:p-8">
            <h2 className="mb-4 flex items-center gap-2 text-lg font-bold text-primary">
              <FileText className="size-5" />
              {t('project.detail.guidelineTitle')}
            </h2>

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
                      {guidelinePdfMetadata || t('project.detail.guidelinePdfFile')}
                    </p>
                  </div>
                </div>
                <div className="flex shrink-0 items-center gap-2">
                  <Button
                    className="h-9 gap-1.5 rounded-md border border-border bg-transparent px-3 text-xs font-semibold text-foreground hover:bg-surface-soft hover:text-primary cursor-pointer"
                    onClick={downloadGuidelinePdf}
                    type="button"
                  >
                    <Download className="size-3.5" />
                    {t('project.detail.downloadDatasetFile')}
                  </Button>
                  <Button
                    className="h-9 gap-1.5 rounded-md border border-border bg-transparent px-3 text-xs font-semibold text-foreground hover:bg-surface-soft hover:text-primary cursor-pointer"
                    onClick={openGuidelinePdf}
                    type="button"
                  >
                    <ExternalLink className="size-3.5" />
                    {t('project.detail.openDatasetFile')}
                  </Button>
                </div>
              </div>
            )}

            {!project.guidelineText && !project.guidelinePdfBase64 && (
              <p className="text-sm text-muted-foreground">{t('project.detail.noGuideline')}</p>
            )}
          </section>
        </div>
      )}
    </PageContainer>
  );
}
