import { useTranslation } from 'react-i18next';
import { PageContainer } from '@/components/common/PageContainer';
import { Spinner } from '@/components/ui/spinner';
import { ProjectDatasetSection } from '@/modules/project/core/components/detail/ProjectDatasetSection';
import { ProjectDetailHeaderActions } from '@/modules/project/core/components/detail/ProjectDetailHeaderActions';
import { ProjectGuidelineSection } from '@/modules/project/core/components/detail/ProjectGuidelineSection';
import { ProjectLabelsSection } from '@/modules/project/core/components/detail/ProjectLabelsSection';
import { ProjectOverviewSection } from '@/modules/project/core/components/detail/ProjectOverviewSection';
import { ProjectParticipantsSection } from '@/modules/project/core/components/detail/ProjectParticipantsSection';
import { useProjectDetailPage } from '@/modules/project/core/hooks/useProjectDetailPage';

export default function ProjectDetailPage() {
  const { t } = useTranslation();
  const {
    colors,
    completionPercentage,
    detailErrorMessage,
    downloadDatasetFile,
    downloadGuidelinePdf,
    editProjectOpen,
    guidelinePdfMetadata,
    handleArchiveAction,
    handleExportAction,
    isArchiveStateUpdating,
    isLoading,
    openDatasetFile,
    openEditProject,
    openGuidelinePdf,
    project,
    projectActionsOpen,
    setEditProjectOpen,
    setProjectActionsOpen,
  } = useProjectDetailPage();

  return (
    <PageContainer>
      <ProjectDetailHeaderActions
        detailErrorMessage={detailErrorMessage}
        editProjectOpen={editProjectOpen}
        isArchiveStateUpdating={isArchiveStateUpdating}
        isLoading={isLoading}
        onArchiveAction={() => void handleArchiveAction()}
        onEditProjectOpenChange={setEditProjectOpen}
        onExportAction={() => void handleExportAction()}
        onOpenEditProject={openEditProject}
        onProjectActionsOpenChange={setProjectActionsOpen}
        project={project}
        projectActionsOpen={projectActionsOpen}
      />

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
          <ProjectOverviewSection
            colors={colors}
            completionPercentage={completionPercentage}
            project={project}
          />

          <ProjectParticipantsSection project={project} />

          <ProjectDatasetSection
            onDownloadDatasetFile={(datasetItemId, fallbackFileName) =>
              void downloadDatasetFile(datasetItemId, fallbackFileName)
            }
            onOpenDatasetFile={(datasetItemId) => void openDatasetFile(datasetItemId)}
            project={project}
          />

          <ProjectLabelsSection project={project} />

          <ProjectGuidelineSection
            guidelinePdfMetadata={guidelinePdfMetadata}
            onDownloadGuidelinePdf={downloadGuidelinePdf}
            onOpenGuidelinePdf={openGuidelinePdf}
            project={project}
          />
        </div>
      )}
    </PageContainer>
  );
}
