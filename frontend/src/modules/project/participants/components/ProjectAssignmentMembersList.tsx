import { useTranslation } from 'react-i18next';
import { Spinner } from '@/components/ui/spinner';
import { cn } from '@/shared/utils/cn';
import {
  type ProjectAssignableMember,
  type ProjectParticipantAssignmentGroup,
} from '@/modules/project/shared/types/project';
import {
  assignmentGroupForUser,
  memberInitials,
  type AssignmentByUserId,
} from '@/modules/project/participants/utils/projectAssignmentUtils';

type ProjectAssignmentMembersListProps = Readonly<{
  assignmentByUserId: AssignmentByUserId;
  creatorUserId: number | null;
  isLoading: boolean;
  members: ProjectAssignableMember[];
  onToggleSelection: (userId: number) => void;
  selectionButtonClassName: string;
}>;

function membersSelectionCardClassName(
  group: ProjectParticipantAssignmentGroup | undefined,
): string {
  if (group === 'GROUP_A') {
    return 'border-primary bg-primary/5 shadow-sm shadow-primary/10 ring-2 ring-primary/20';
  }

  if (group === 'GROUP_B') {
    return 'border-sky-500/70 bg-sky-50 shadow-sm shadow-sky-500/10 ring-2 ring-sky-500/20';
  }

  return 'border-border bg-background hover:border-primary/40 hover:bg-accent/30';
}

function groupBadgeClassName(group: ProjectParticipantAssignmentGroup): string {
  return group === 'GROUP_A' ? 'bg-primary/10 text-primary' : 'bg-sky-100 text-sky-700';
}

export function ProjectAssignmentMembersList({
  assignmentByUserId,
  creatorUserId,
  isLoading,
  members,
  onToggleSelection,
  selectionButtonClassName,
}: ProjectAssignmentMembersListProps) {
  const { t } = useTranslation();

  if (isLoading) {
    return (
      <div className="rounded-md border border-border bg-background px-4 py-6 text-sm text-muted-foreground">
        <span className="inline-flex items-center gap-2">
          <Spinner aria-hidden className="size-4" />
          {t('project.create.loadingMembers')}
        </span>
      </div>
    );
  }

  if (members.length === 0) {
    return (
      <p className="rounded-md border border-dashed border-border bg-background px-4 py-5 text-sm text-muted-foreground">
        {t('project.create.noAssignableMembers')}
      </p>
    );
  }

  return (
    <div className="grid gap-3 sm:grid-cols-2">
      {members.map((member) => {
        const group = assignmentGroupForUser(assignmentByUserId, member.userId, creatorUserId);
        const memberName = `${member.firstName} ${member.lastName}`;

        return (
          <button
            aria-label={t('project.create.assignmentCycleLabel', {
              name: memberName,
              state: group
                ? t(`project.create.assignmentGroups.${group}`)
                : t('project.create.assignmentGroups.unassigned'),
            })}
            className={cn(selectionButtonClassName, membersSelectionCardClassName(group))}
            key={member.userId}
            onClick={() => onToggleSelection(member.userId)}
            type="button"
          >
            <div className="flex items-center gap-3">
              <div className="flex size-10 items-center justify-center rounded-full bg-primary/10 text-xs font-bold text-primary">
                {memberInitials(member.firstName, member.lastName)}
              </div>
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-semibold text-primary">{memberName}</p>
                <p className="truncate text-xs text-muted-foreground">{member.email}</p>
              </div>
              {group ? (
                <span
                  className={cn(
                    'shrink-0 rounded-full px-2.5 py-1 text-xs font-bold',
                    groupBadgeClassName(group),
                  )}
                >
                  {t(`project.create.assignmentGroups.${group}`)}
                </span>
              ) : null}
            </div>
          </button>
        );
      })}
    </div>
  );
}
