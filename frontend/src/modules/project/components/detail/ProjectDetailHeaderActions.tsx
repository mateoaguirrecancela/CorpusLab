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
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { Spinner } from '@/components/ui/spinner';
import { EditProjectDialog } from '@/modules/project/components/EditProjectDialog';
import { type ProjectDetail } from '@/modules/project/types/project';

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

  return (
    <div className="flex items-center justify-between gap-4">
      <BackButton fallbackTo="/home/projects" />
      {!isLoading && !detailErrorMessage && project && (
        <div className="flex items-center gap-2">
          <Link
            className="inline-flex h-10 shrink-0 items-center gap-2 rounded-lg bg-primary px-3 text-sm font-semibold text-primary-foreground transition hover:bg-primary/90"
            to={`/home/projects/${project.id}/annotate`}
          >
            <PenSquare className="size-4" />
            {t('project.detail.openAnnotationWorkspace')}
          </Link>

          {project.participantRole === 'CREATOR' && (
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
                  <button
                    className="flex h-9 w-full items-center gap-2 rounded-md px-3 text-left text-sm font-medium text-foreground transition-colors hover:bg-accent disabled:cursor-not-allowed disabled:opacity-60 cursor-pointer"
                    disabled={isArchiveStateUpdating}
                    onClick={onArchiveAction}
                    type="button"
                  >
                    {isArchiveStateUpdating ? (
                      <Spinner aria-hidden className="size-4" />
                    ) : project.archived ? (
                      <ArchiveRestore className="size-4" />
                    ) : (
                      <Archive className="size-4" />
                    )}
                    {isArchiveStateUpdating
                      ? t('project.detail.updatingArchiveState')
                      : project.archived
                        ? t('project.detail.unarchiveProject')
                        : t('project.detail.archiveProject')}
                  </button>

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
                initialParticipantUserIds={project.participants
                  .filter((participant) => participant.role === 'PARTICIPANT')
                  .map((participant) => participant.userId)}
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
