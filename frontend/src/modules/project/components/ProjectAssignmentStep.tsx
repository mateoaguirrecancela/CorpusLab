import { type ReactNode, useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import { Spinner } from '@/components/ui/spinner';
import { cn } from '@/lib/utils';
import { useProfileQuery } from '@/modules/auth/hooks/useProfileQuery';
import {
  type ProjectParticipantAssignment,
  type ProjectParticipantAssignmentGroup,
} from '@/modules/project/types/project';
import { useResearchGroupDetailQuery } from '@/modules/researchgroup/hooks/useResearchGroupQueries';

type ProjectAssignmentStepProps = Readonly<{
  groupId: number;
  isSubmitting?: boolean;
  onBack: () => void;
  onCompleted: (participantAssignments: ProjectParticipantAssignment[]) => void | Promise<void>;
}>;

const GROUP_SEQUENCE: ReadonlyArray<ProjectParticipantAssignmentGroup | null> = [
  'GROUP_A',
  'GROUP_B',
  null,
];

function initials(firstName: string, lastName: string): string {
  const first = firstName.trim().charAt(0);
  const last = lastName.trim().charAt(0);
  return `${first}${last}`.toUpperCase();
}

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

export function ProjectAssignmentStep({
  groupId,
  isSubmitting = false,
  onBack,
  onCompleted,
}: ProjectAssignmentStepProps) {
  const { t } = useTranslation();
  const { data: profile } = useProfileQuery();
  const { data: group, isLoading } = useResearchGroupDetailQuery(groupId);

  const creatorEmail = profile?.email?.toLowerCase() ?? null;
  const members = group?.members ?? [];
  const creatorMember = useMemo(
    () =>
      creatorEmail === null
        ? null
        : members.find((member) => member.email.toLowerCase() === creatorEmail) ?? null,
    [creatorEmail, members],
  );
  const creatorUserId = creatorMember?.userId ?? null;
  const orderedMembers = useMemo(() => {
    if (creatorUserId === null) {
      return members;
    }

    const creatorFirst: typeof members = [];
    const rest: typeof members = [];
    for (const member of members) {
      if (member.userId === creatorUserId) {
        creatorFirst.push(member);
      } else {
        rest.push(member);
      }
    }

    return [...creatorFirst, ...rest];
  }, [creatorUserId, members]);

  const [assignmentByUserId, setAssignmentByUserId] = useState<
    Record<number, ProjectParticipantAssignmentGroup>
  >({});

  useEffect(() => {
    if (creatorUserId === null) {
      return;
    }

    setAssignmentByUserId((prev) => {
      if (prev[creatorUserId]) {
        return prev;
      }

      return {
        ...prev,
        [creatorUserId]: 'GROUP_A',
      };
    });
  }, [creatorUserId]);

  const participantAssignments = useMemo(
    () =>
      orderedMembers.flatMap<ProjectParticipantAssignment>((member) => {
        const iaaGroup =
          assignmentByUserId[member.userId] ??
          (member.userId === creatorUserId ? 'GROUP_A' : undefined);
        return iaaGroup ? [{ userId: member.userId, iaaGroup }] : [];
      }),
    [assignmentByUserId, creatorUserId, orderedMembers],
  );

  const toggleSelection = (userId: number) => {
    setAssignmentByUserId((prev) => {
      if (creatorUserId !== null && userId === creatorUserId) {
        const currentGroup = prev[userId] ?? 'GROUP_A';
        const nextAssignments = { ...prev };
        nextAssignments[userId] = currentGroup === 'GROUP_A' ? 'GROUP_B' : 'GROUP_A';
        return nextAssignments;
      }

      const currentGroup = prev[userId];
      const currentIndex = GROUP_SEQUENCE.indexOf(currentGroup ?? null);
      const nextGroup = GROUP_SEQUENCE[(currentIndex + 1) % GROUP_SEQUENCE.length];
      const nextAssignments = { ...prev };

      if (nextGroup === null) {
        delete nextAssignments[userId];
      } else {
        nextAssignments[userId] = nextGroup;
      }

      return nextAssignments;
    });
  };

  const handleAssign = async () => {
    await onCompleted(participantAssignments);
  };

  const renderMembersContent = (): ReactNode => {
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

    if (orderedMembers.length === 0) {
      return (
        <p className="rounded-md border border-dashed border-border bg-background px-4 py-5 text-sm text-muted-foreground">
          {t('project.create.noAssignableMembers')}
        </p>
      );
    }

    return (
      <div className="grid gap-3 sm:grid-cols-2">
        {orderedMembers.map((member) => {
          const group =
            assignmentByUserId[member.userId] ??
            (member.userId === creatorUserId ? 'GROUP_A' : undefined);
          const memberName = `${member.firstName} ${member.lastName}`;

          return (
            <button
              aria-label={t('project.create.assignmentCycleLabel', {
                name: memberName,
                state: group
                  ? t(`project.create.assignmentGroups.${group}`)
                  : t('project.create.assignmentGroups.unassigned'),
              })}
              className={cn(
                'rounded-md border p-4 text-left transition-all',
                membersSelectionCardClassName(group),
              )}
              key={member.userId}
              onClick={() => toggleSelection(member.userId)}
              type="button"
            >
              <div className="flex items-center gap-3">
                <div className="flex size-10 items-center justify-center rounded-full bg-primary/10 text-xs font-bold text-primary">
                  {initials(member.firstName, member.lastName)}
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
  };

  return (
    <div className="mt-8 space-y-3">
      <p className="text-xs font-bold uppercase tracking-wider text-muted-foreground">
        {t('project.create.steps.assignment')} *
      </p>

      {renderMembersContent()}

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
          disabled={isSubmitting}
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
