import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import { Spinner } from '@/components/ui/spinner';
import { ProjectAssignmentMembersList } from '@/modules/project/components/ProjectAssignmentMembersList';
import { useProjectAssignmentStep } from '@/modules/project/hooks/useProjectAssignmentStep';
import { type ProjectParticipantAssignment } from '@/modules/project/types/project';

type ProjectAssignmentStepProps = Readonly<{
  groupId: number;
  isSubmitting?: boolean;
  onBack: () => void;
  onCompleted: (participantAssignments: ProjectParticipantAssignment[]) => void | Promise<void>;
}>;

export function ProjectAssignmentStep({
  groupId,
  isSubmitting = false,
  onBack,
  onCompleted,
}: ProjectAssignmentStepProps) {
  const { t } = useTranslation();
  const {
    assignmentByUserId,
    creatorUserId,
    handleAssign,
    isError,
    isLoading,
    members,
    toggleSelection,
  } = useProjectAssignmentStep({ groupId, onCompleted });

  return (
    <div className="mt-8 space-y-3">
      <p className="text-xs font-bold uppercase tracking-wider text-muted-foreground">
        {t('project.create.steps.assignment')} *
      </p>

      <ProjectAssignmentMembersList
        assignmentByUserId={assignmentByUserId}
        creatorUserId={creatorUserId}
        isLoading={isLoading}
        members={members}
        onToggleSelection={toggleSelection}
        selectionButtonClassName="rounded-md border p-4 text-left transition-all"
      />

      <div className="flex justify-end gap-3">
        <Button
          className="h-10 rounded-md border border-border bg-surface-base px-6 text-sm font-semibold text-primary hover:bg-accent"
          onClick={onBack}
          type="button"
          variant="outline"
        >
          {t('project.create.previousStepSimple')}
        </Button>
        <Button
          className="h-10 min-w-44 rounded-md bg-primary text-sm font-semibold text-white transition-colors hover:bg-primary-strong disabled:bg-secondary"
          disabled={isSubmitting || isLoading || isError}
          onClick={() => void handleAssign()}
          type="button"
        >
          {isSubmitting ? (
            <span className="inline-flex items-center gap-2">
              <Spinner aria-hidden className="size-4" />
              {t('project.create.finishingCreateProject')}
            </span>
          ) : (
            t('project.create.finishCreateProject')
          )}
        </Button>
      </div>
    </div>
  );
}
