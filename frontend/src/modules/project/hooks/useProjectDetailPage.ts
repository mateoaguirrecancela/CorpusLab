import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router';
import { toast } from 'sonner';
import {
  useArchiveProjectMutation,
  useProjectDetailQuery,
  useUnarchiveProjectMutation,
} from '@/modules/project/hooks/useProjectQueries';
import {
  exportProjectAnnotationResultsCsv,
  getArchiveProjectErrorMessage,
  getProjectAnnotationExportErrorMessage,
  getProjectDatasetItemContent,
  getProjectDatasetItemContentErrorMessage,
  getProjectDetailLoadErrorMessage,
  getUnarchiveProjectErrorMessage,
} from '@/modules/project/services/projectService';
import { type ProjectDetail } from '@/modules/project/types/project';
import {
  calculateBase64SizeBytes,
  decodeBase64ToBuffer,
  parseBase64FilePayload,
  triggerBlobDownload,
} from '@/modules/project/utils/fileDownloadUtils';
import { formatFileSize } from '@/modules/project/utils/projectDisplayUtils';
import {
  completionColor,
  normalizeCompletionPercentage,
} from '@/modules/project/utils/projectUtils';

type CompletionColors = ReturnType<typeof completionColor>;

type ProjectDetailPageState = Readonly<{
  colors: CompletionColors;
  completionPercentage: number;
  detailErrorMessage: string;
  editProjectOpen: boolean;
  guidelinePdfMetadata: string;
  isArchiveStateUpdating: boolean;
  isLoading: boolean;
  project: ProjectDetail | undefined;
  projectActionsOpen: boolean;
  closeProjectActions: () => void;
  downloadAnnotationResultsCsv: () => Promise<void>;
  downloadDatasetFile: (datasetItemId: number, fallbackFileName: string) => Promise<void>;
  downloadGuidelinePdf: () => void;
  handleArchiveAction: () => Promise<void>;
  handleExportAction: () => Promise<void>;
  openDatasetFile: (datasetItemId: number) => Promise<void>;
  openEditProject: () => void;
  openGuidelinePdf: () => void;
  setEditProjectOpen: (open: boolean) => void;
  setProjectActionsOpen: (open: boolean) => void;
}>;

export function useProjectDetailPage(): ProjectDetailPageState {
  const { t } = useTranslation();
  const { projectId } = useParams();
  const [projectActionsOpen, setProjectActionsOpen] = useState(false);
  const [editProjectOpen, setEditProjectOpen] = useState(false);

  const numericProjectId = useMemo(() => Number(projectId), [projectId]);
  const isInvalidProjectId = !Number.isFinite(numericProjectId) || numericProjectId <= 0;

  const { data: project, isLoading, isError, error } = useProjectDetailQuery(numericProjectId);
  const archiveProjectMutation = useArchiveProjectMutation();
  const unarchiveProjectMutation = useUnarchiveProjectMutation();
  const isArchiveStateUpdating =
    archiveProjectMutation.isPending || unarchiveProjectMutation.isPending;

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
  const guidelinePdfBase64 = project?.guidelinePdfBase64;

  const guidelinePdfMetadata = useMemo(() => {
    if (!guidelinePdfBase64) {
      return '';
    }

    const { mimeType, base64Payload } = parseBase64FilePayload(
      guidelinePdfBase64,
      'application/pdf',
    );
    const sizeBytes = calculateBase64SizeBytes(base64Payload);

    if (sizeBytes <= 0) {
      return mimeType;
    }

    return `${mimeType} - ${formatFileSize(sizeBytes)}`;
  }, [guidelinePdfBase64]);

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

  const downloadAnnotationResultsCsv = async () => {
    if (project?.participantRole !== 'CREATOR') {
      return;
    }

    try {
      const exportFile = await exportProjectAnnotationResultsCsv(project.id);
      const downloadFileName =
        exportFile.fileName?.trim() || `project-${project.id}-annotations.csv`;

      triggerBlobDownload(exportFile.blob, downloadFileName);
    } catch (exportError) {
      toast.error(getProjectAnnotationExportErrorMessage(exportError));
    }
  };

  const toggleArchivedState = async () => {
    if (!project || project.participantRole !== 'CREATOR') {
      return;
    }

    try {
      if (project.archived) {
        await unarchiveProjectMutation.mutateAsync({ projectId: project.id });
        toast.success(t('project.detail.unarchiveSuccess'));
      } else {
        await archiveProjectMutation.mutateAsync({ projectId: project.id });
        toast.success(t('project.detail.archiveSuccess'));
      }
    } catch (archiveError) {
      const message = project.archived
        ? getUnarchiveProjectErrorMessage(archiveError)
        : getArchiveProjectErrorMessage(archiveError);
      toast.error(message);
    }
  };

  const handleArchiveAction = async () => {
    await toggleArchivedState();
    setProjectActionsOpen(false);
  };

  const handleExportAction = async () => {
    setProjectActionsOpen(false);
    await downloadAnnotationResultsCsv();
  };

  return {
    colors,
    completionPercentage,
    detailErrorMessage,
    editProjectOpen,
    guidelinePdfMetadata,
    isArchiveStateUpdating,
    isLoading,
    project,
    projectActionsOpen,
    closeProjectActions: () => setProjectActionsOpen(false),
    downloadAnnotationResultsCsv,
    downloadDatasetFile,
    downloadGuidelinePdf,
    handleArchiveAction,
    handleExportAction,
    openDatasetFile,
    openEditProject: () => {
      setProjectActionsOpen(false);
      setEditProjectOpen(true);
    },
    openGuidelinePdf,
    setEditProjectOpen,
    setProjectActionsOpen,
  };
}
