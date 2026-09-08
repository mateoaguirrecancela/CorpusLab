import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import { Spinner } from '@/components/ui/spinner';
import { ProjectAssignmentMembersList } from '@/modules/project/participants/components/ProjectAssignmentMembersList';
import { useProjectAssignmentStep } from '@/modules/project/participants/hooks/useProjectAssignmentStep';
import { type ProjectParticipantAssignment } from '@/modules/project/shared/types/project';

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
        <Button onClick={onBack} size="action" type="button" variant="secondaryAction">
          {t('project.create.previousStepSimple')}
        </Button>
        <Button
          disabled={isSubmitting || isLoading || isError}
          onClick={() => void handleAssign()}
          size="action-xl"
          type="button"
          variant="primaryAction"
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
