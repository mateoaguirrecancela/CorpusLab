import {
  Archive,
  ArchiveRestore,
  Download,
  FilePenLine,
  MoreVertical,
  PenSquare,
} from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router';
import { BackButton } from '@/components/common/BackButton';
import { buttonVariants } from '@/components/ui/buttonVariants';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { Spinner } from '@/components/ui/spinner';
import { cn } from '@/shared/utils/cn';
import { EditProjectDialog } from '@/modules/project/core/components/EditProjectDialog';
import { type ProjectDetail } from '@/modules/project/shared/types/project';

type ArchiveActionButtonProps = Readonly<{
  isArchived: boolean;
  isUpdating: boolean;
  onArchiveAction: () => void;
}>;

function ArchiveActionButton({
  isArchived,
  isUpdating,
  onArchiveAction,
}: ArchiveActionButtonProps) {
  const { t } = useTranslation();

  let icon = <Archive className="size-4" />;
  let label = t('project.detail.archiveProject');
  if (isUpdating) {
    icon = <Spinner aria-hidden className="size-4" />;
    label = t('project.detail.updatingArchiveState');
  } else if (isArchived) {
    icon = <ArchiveRestore className="size-4" />;
    label = t('project.detail.unarchiveProject');
  }

  return (
    <button
      className="flex h-9 w-full items-center gap-2 rounded-md px-3 text-left text-sm font-medium text-foreground transition-colors hover:bg-accent disabled:cursor-not-allowed disabled:opacity-60 cursor-pointer"
      disabled={isUpdating}
      onClick={onArchiveAction}
      type="button"
    >
      {icon}
      {label}
    </button>
  );
}

type ProjectDetailHeaderActionsProps = Readonly<{
  detailErrorMessage: string;
  editProjectOpen: boolean;
  isArchiveStateUpdating: boolean;
  isLoading: boolean;
  project: ProjectDetail | undefined;
  projectActionsOpen: boolean;
  onArchiveAction: () => void;
  onExportAction: () => void;
  onOpenEditProject: () => void;
  onProjectActionsOpenChange: (open: boolean) => void;
  onEditProjectOpenChange: (open: boolean) => void;
}>;

export function ProjectDetailHeaderActions({
  detailErrorMessage,
  editProjectOpen,
  isArchiveStateUpdating,
  isLoading,
  project,
  projectActionsOpen,
  onArchiveAction,
  onExportAction,
  onOpenEditProject,
  onProjectActionsOpenChange,
  onEditProjectOpenChange,
}: ProjectDetailHeaderActionsProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const canOpenAnnotationWorkspace = project != null && project.setupCompleted && !project.archived;

  return (
    <div className="flex items-center justify-between gap-4">
      <BackButton fallbackTo="/home/projects" />
      {!isLoading && !detailErrorMessage && project && (
        <div className="flex items-center gap-2">
          {canOpenAnnotationWorkspace && (
            <Link
              className={cn(buttonVariants({ variant: 'primaryAction', size: 'action' }), 'px-3')}
              to={`/home/projects/${project.id}/annotate`}
            >
              <PenSquare className="size-4" />
              {t('project.detail.openAnnotationWorkspace')}
            </Link>
          )}

          {project.canManageProject && (
            <>
              <Popover open={projectActionsOpen} onOpenChange={onProjectActionsOpenChange}>
                <PopoverTrigger
                  aria-label={t('project.detail.projectActions')}
                  className="inline-flex size-10 items-center justify-center border rounded-md bg-white text-muted-foreground transition-colors hover:bg-accent hover:text-primary cursor-pointer"
                  type="button"
                >
                  <MoreVertical className="size-4" />
                </PopoverTrigger>

                <PopoverContent
                  align="end"
                  className="w-64 rounded-xl border border-border bg-surface-base p-1.5"
                >
                  <ArchiveActionButton
                    isArchived={project.archived}
                    isUpdating={isArchiveStateUpdating}
                    onArchiveAction={onArchiveAction}
                  />

                  <button
                    className="flex h-9 w-full items-center gap-2 rounded-md px-3 text-left text-sm font-medium text-foreground transition-colors hover:bg-accent cursor-pointer"
                    onClick={onOpenEditProject}
                    type="button"
                  >
                    <FilePenLine className="size-4" />
                    {t('project.detail.editProject')}
                  </button>

                  <button
                    className="flex h-9 w-full items-center gap-2 rounded-md px-3 text-left text-sm font-medium text-foreground transition-colors hover:bg-accent cursor-pointer"
                    onClick={onExportAction}
                    type="button"
                  >
                    <Download className="size-4" />
                    {t('project.detail.exportAnnotationsCsv')}
                  </button>
                </PopoverContent>
              </Popover>

              <EditProjectDialog
                groupId={project.researchGroupId}
                initialDescription={project.description}
                initialName={project.name}
                initialParticipantAssignments={project.participants.map((participant) => ({
                  userId: participant.userId,
                  iaaGroup: participant.iaaGroup ?? 'GROUP_A',
                }))}
                onDeleted={() => navigate(`/home/research-groups/${project.researchGroupId}`)}
                onOpenChange={onEditProjectOpenChange}
                open={editProjectOpen}
                projectId={project.id}
                showDeleteButton
              />
            </>
          )}
        </div>
      )}
    </div>
  );
}
