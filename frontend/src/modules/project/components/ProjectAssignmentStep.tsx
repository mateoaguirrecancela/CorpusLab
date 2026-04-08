import { type ReactNode, useMemo, useState } from 'react';
import { Check } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Spinner } from '@/components/ui/spinner';
import { useProfileQuery } from '@/modules/auth/hooks/useProfileQuery';
import { useAssignProjectParticipantsMutation } from '@/modules/project/hooks/useProjectQueries';
import { getAssignParticipantsErrorMessage } from '@/modules/project/services/projectService';
import { useResearchGroupDetailQuery } from '@/modules/researchgroup/hooks/useResearchGroupQueries';

type ProjectAssignmentStepProps = Readonly<{
  groupId: number;
  onBack: () => void;
  onCompleted: () => void;
  projectId: number;
}>;

function initials(firstName: string, lastName: string): string {
  const first = firstName.trim().charAt(0);
  const last = lastName.trim().charAt(0);
  return `${first}${last}`.toUpperCase();
}

export function ProjectAssignmentStep({
  groupId,
  onBack,
  onCompleted,
  projectId,
}: ProjectAssignmentStepProps) {
  const { t } = useTranslation();
  const { data: profile } = useProfileQuery();
  const { data: group, isLoading } = useResearchGroupDetailQuery(groupId);
  const assignParticipantsMutation = useAssignProjectParticipantsMutation();

  const creatorEmail = profile?.email?.toLowerCase() ?? '';
  const members = useMemo(
    () => (group?.members ?? []).filter((member) => member.email.toLowerCase() !== creatorEmail),
    [creatorEmail, group?.members],
  );

  const [selectedUserIds, setSelectedUserIds] = useState<number[]>([]);

  const selectedSet = useMemo(() => new Set(selectedUserIds), [selectedUserIds]);

  let membersContent: ReactNode;
  if (isLoading) {
    membersContent = (
      <div className="rounded-md border border-border bg-background px-4 py-6 text-sm text-muted-foreground">
        <span className="inline-flex items-center gap-2">
          <Spinner aria-hidden className="size-4" />
          {t('project.create.loadingMembers')}
        </span>
      </div>
    );
  } else if (members.length === 0) {
    membersContent = (
      <p className="rounded-md border border-dashed border-border bg-background px-4 py-5 text-sm text-muted-foreground">
        {t('project.create.noAssignableMembers')}
      </p>
    );
  } else {
    membersContent = (
      <div className="grid gap-3 sm:grid-cols-2">
        {members.map((member) => {
          const isSelected = selectedSet.has(member.userId);
          const cardClass = isSelected
            ? 'border-primary bg-primary/5 shadow-sm shadow-primary/10'
            : 'border-border bg-background hover:border-primary/40 hover:bg-accent/30';

          return (
            <button
              className={['rounded-xl border p-4 text-left transition-all', cardClass].join(' ')}
              key={member.userId}
              onClick={() => toggleSelection(member.userId)}
              type="button"
            >
              <div className="flex items-start justify-between gap-3">
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

                <span
                  className={[
                    'inline-flex size-5 items-center justify-center rounded border transition-colors',
                    isSelected
                      ? 'border-primary bg-primary text-white'
                      : 'border-border bg-surface-base text-transparent',
                  ].join(' ')}
                >
                  <Check className="size-3" />
                </span>
              </div>
            </button>
          );
        })}
      </div>
    );
  }

  const toggleSelection = (userId: number) => {
    setSelectedUserIds((prev) =>
      prev.includes(userId) ? prev.filter((id) => id !== userId) : [...prev, userId],
    );
  };

  const handleAssign = async () => {
    try {
      await assignParticipantsMutation.mutateAsync({
        groupId,
        projectId,
        participantUserIds: selectedUserIds,
      });
      onCompleted();
    } catch (error) {
      toast.error(getAssignParticipantsErrorMessage(error));
    }
  };

  return (
    <div className="mt-8 space-y-6">
      <section className="rounded-xl border border-border bg-surface-base p-4 sm:p-5">
        <p className="text-xs font-bold uppercase tracking-wider text-muted-foreground">
          {t('project.create.assignmentTitle')}
        </p>
        <p className="mt-1 text-sm text-muted-foreground">
          {t('project.create.assignmentDescription')}
        </p>
      </section>

      {membersContent}

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
          disabled={assignParticipantsMutation.isPending}
          onClick={() => void handleAssign()}
          type="button"
        >
          {assignParticipantsMutation.isPending ? (
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
