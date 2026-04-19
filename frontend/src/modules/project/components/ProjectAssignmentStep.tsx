import { type ReactNode, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import { Spinner } from '@/components/ui/spinner';
import { useProfileQuery } from '@/modules/auth/hooks/useProfileQuery';
import { useResearchGroupDetailQuery } from '@/modules/researchgroup/hooks/useResearchGroupQueries';

type ProjectAssignmentStepProps = Readonly<{
  groupId: number;
  isSubmitting?: boolean;
  onBack: () => void;
  onCompleted: (participantUserIds: number[]) => void | Promise<void>;
}>;

function initials(firstName: string, lastName: string): string {
  const first = firstName.trim().charAt(0);
  const last = lastName.trim().charAt(0);
  return `${first}${last}`.toUpperCase();
}

function membersSelectionCardClassName(isSelected: boolean): string {
  return isSelected
    ? 'border-primary bg-primary/5 shadow-sm shadow-primary/10 ring-2 ring-primary/20'
    : 'border-border bg-background hover:border-primary/40 hover:bg-accent/30';
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

  const creatorEmail = profile?.email?.toLowerCase() ?? '';
  const members = useMemo(
    () => (group?.members ?? []).filter((member) => member.email.toLowerCase() !== creatorEmail),
    [creatorEmail, group?.members],
  );

  const [selectedUserIds, setSelectedUserIds] = useState<number[]>([]);

  const selectedSet = useMemo(() => new Set(selectedUserIds), [selectedUserIds]);

  const toggleSelection = (userId: number) => {
    setSelectedUserIds((prev) =>
      prev.includes(userId) ? prev.filter((id) => id !== userId) : [...prev, userId],
    );
  };

  const handleAssign = async () => {
    await onCompleted(selectedUserIds);
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
          const isSelected = selectedSet.has(member.userId);

          return (
            <button
              className={[
                'rounded-xl border p-4 text-left transition-all',
                membersSelectionCardClassName(isSelected),
              ].join(' ')}
              key={member.userId}
              onClick={() => toggleSelection(member.userId)}
              type="button"
            >
              <div className="flex items-center gap-3">
                <div className="flex size-10 items-center justify-center rounded-full bg-primary/10 text-xs font-bold text-primary">
                  {initials(member.firstName, member.lastName)}
                </div>
                <div className="min-w-0">
                  <p className="truncate text-sm font-semibold text-primary">
                    {member.firstName} {member.lastName}
                  </p>
                  <p className="truncate text-xs text-muted-foreground">{member.email}</p>
                </div>
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
              {t('project.create.assigningParticipants')}
            </span>
          ) : (
            t('project.create.finishCreateProject')
          )}
        </Button>
      </div>
    </div>
  );
}
