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
  getProjectGuidelinePdf,
  getProjectGuidelinePdfErrorMessage,
  getUnarchiveProjectErrorMessage,
} from '@/modules/project/services/projectService';
import {
  type ProjectDatasetItemContent,
  type ProjectDetail,
} from '@/modules/project/types/project';
import { triggerBlobDownload } from '@/modules/project/utils/fileDownloadUtils';
import { formatFileSize } from '@/modules/project/utils/projectDisplayUtils';
import {
  completionColor,
  normalizeCompletionPercentage,
} from '@/modules/project/utils/projectUtils';
import { isPositiveId } from '@/modules/project/utils/projectFormUtils';

type CompletionColors = ReturnType<typeof completionColor>;

function openBlobInNewTab(blob: Blob): void {
  const blobUrl = URL.createObjectURL(blob);
  globalThis.open(blobUrl, '_blank', 'noopener,noreferrer');

  setTimeout(() => {
    URL.revokeObjectURL(blobUrl);
  }, 60_000);
}

function getDownloadFileName(file: ProjectDatasetItemContent, fallbackFileName: string): string {
  return file.fileName?.trim() || fallbackFileName;
}

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
  downloadGuidelinePdf: () => Promise<void>;
  handleArchiveAction: () => Promise<void>;
  handleExportAction: () => Promise<void>;
  openDatasetFile: (datasetItemId: number) => Promise<void>;
  openEditProject: () => void;
  openGuidelinePdf: () => Promise<void>;
  setEditProjectOpen: (open: boolean) => void;
  setProjectActionsOpen: (open: boolean) => void;
}>;

export function useProjectDetailPage(): ProjectDetailPageState {
  const { t } = useTranslation();
  const { projectId } = useParams();
  const [projectActionsOpen, setProjectActionsOpen] = useState(false);
  const [editProjectOpen, setEditProjectOpen] = useState(false);

  const numericProjectId = useMemo(() => Number(projectId), [projectId]);
  const isInvalidProjectId = !isPositiveId(numericProjectId);

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

  let guidelinePdfMetadata = '';
  if (project?.guidelinePdfAvailable) {
    const mimeType = project.guidelinePdfMimeType ?? 'application/pdf';
    guidelinePdfMetadata =
      project.guidelinePdfSizeBytes > 0
        ? `${mimeType} - ${formatFileSize(project.guidelinePdfSizeBytes)}`
        : mimeType;
  }

  const openGuidelinePdf = async () => {
    if (!project?.guidelinePdfAvailable) {
      return;
    }

    try {
      const guidelinePdf = await getProjectGuidelinePdf(project.id);
      openBlobInNewTab(guidelinePdf.blob);
    } catch (openError) {
      toast.error(getProjectGuidelinePdfErrorMessage(openError));
    }
  };

  const downloadGuidelinePdf = async () => {
    if (!project?.guidelinePdfAvailable) {
      return;
    }

    try {
      const guidelinePdf = await getProjectGuidelinePdf(project.id);
      triggerBlobDownload(guidelinePdf.blob, getDownloadFileName(guidelinePdf, 'guideline.pdf'));
    } catch (downloadError) {
      toast.error(getProjectGuidelinePdfErrorMessage(downloadError));
    }
  };

  const openDatasetFile = async (datasetItemId: number) => {
    if (!project) {
      return;
    }

    try {
      const file = await getProjectDatasetItemContent(project.id, datasetItemId);
      openBlobInNewTab(file.blob);
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
      const downloadFileName = getDownloadFileName(
        file,
        normalizedFallbackFileName || `dataset-item-${datasetItemId}`,
      );

      triggerBlobDownload(file.blob, downloadFileName);
    } catch (downloadError) {
      toast.error(getProjectDatasetItemContentErrorMessage(downloadError));
    }
  };

  const downloadAnnotationResultsCsv = async () => {
    if (!project?.canExportAnnotations) {
      return;
    }

    try {
      const exportFile = await exportProjectAnnotationResultsCsv(project.id);
      const downloadFileName = getDownloadFileName(
        exportFile,
        `project-${project.id}-annotations.csv`,
      );

      triggerBlobDownload(exportFile.blob, downloadFileName);
    } catch (exportError) {
      toast.error(getProjectAnnotationExportErrorMessage(exportError));
    }
  };

  const toggleArchivedState = async () => {
    if (!project || !project.canArchiveProject) {
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
