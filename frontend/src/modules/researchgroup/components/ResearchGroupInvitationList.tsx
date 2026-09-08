import { Check, UserRound, X } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { RoleBadge } from '@/components/common/RoleBadge';
import { Button } from '@/components/ui/button';
import { Spinner } from '@/components/ui/spinner';
import { type ResearchGroupInvitation } from '@/modules/researchgroup/types/researchGroup';
import { type ResearchGroupInvitationListStatus } from '@/modules/researchgroup/utils/researchGroupInvitations';

type ResearchGroupInvitationListProps = Readonly<{
  invitations: ResearchGroupInvitation[];
  isAcceptingInvitation: boolean;
  isDecliningInvitation: boolean;
  isInvitationActionPending: boolean;
  pendingInvitationId: number | undefined;
  status: ResearchGroupInvitationListStatus;
  onAcceptInvitation: (invitationId: number) => void;
  onDeclineInvitation: (invitationId: number) => void;
}>;

type ResearchGroupInvitationCardProps = Readonly<{
  invitation: ResearchGroupInvitation;
  isAcceptingInvitation: boolean;
  isDecliningInvitation: boolean;
  isInvitationActionPending: boolean;
  pendingInvitationId: number | undefined;
  onAcceptInvitation: (invitationId: number) => void;
  onDeclineInvitation: (invitationId: number) => void;
}>;

function ResearchGroupInvitationsLoading() {
  const { t } = useTranslation();

  return (
    <div className="rounded-lg border border-border bg-surface-base px-4 py-4 text-sm text-muted-foreground">
      <span className="inline-flex items-center gap-2">
        <Spinner aria-hidden className="size-4" />
        {t('researchGroup.invitationsDialog.loadingInvitations')}
      </span>
    </div>
  );
}

function ResearchGroupInvitationsEmpty() {
  const { t } = useTranslation();

  return (
    <div className="rounded-lg border border-border bg-surface-base px-4 py-4 text-sm text-muted-foreground">
      {t('researchGroup.invitationsDialog.noInvitations')}
    </div>
  );
}

function ResearchGroupInvitationCard({
  invitation,
  isAcceptingInvitation,
  isDecliningInvitation,
  isInvitationActionPending,
  pendingInvitationId,
  onAcceptInvitation,
  onDeclineInvitation,
}: ResearchGroupInvitationCardProps) {
  const { t } = useTranslation();
  const isCurrentInvitationPending = pendingInvitationId === invitation.id;

  return (
    <div className="flex items-center gap-2 rounded-lg border border-border bg-surface-base px-4 py-3">
      <p className="min-w-0 flex flex-1 items-center gap-8 overflow-hidden text-sm text-muted-foreground">
        <span className="shrink-0 font-semibold text-primary">{invitation.researchGroupName}</span>
        <span className="inline-flex min-w-0 items-center gap-2">
          <UserRound className="size-3.5 shrink-0" />
          <span className="truncate">{invitation.inviterFullName}</span>
        </span>
        <span className="inline-flex shrink-0 items-center gap-2">
          <RoleBadge label={t(`researchGroup.roles.${invitation.role}`)} role={invitation.role} />
        </span>
      </p>

      <div className="flex shrink-0 items-center gap-2">
        <Button
          aria-label={t('researchGroup.invitationsDialog.acceptAria')}
          className="cursor-pointer rounded-full border-success bg-surface-base text-success hover:bg-success hover:text-white"
          disabled={isInvitationActionPending}
          onClick={() => onAcceptInvitation(invitation.id)}
          size="icon"
          type="button"
          variant="outline"
        >
          {isAcceptingInvitation && isCurrentInvitationPending ? (
            <Spinner aria-hidden className="size-4" />
          ) : (
            <Check className="size-4" />
          )}
        </Button>

        <Button
          aria-label={t('researchGroup.invitationsDialog.declineAria')}
          className="cursor-pointer rounded-full border-destructive bg-surface-base text-destructive hover:bg-destructive hover:text-white"
          disabled={isInvitationActionPending}
          onClick={() => onDeclineInvitation(invitation.id)}
          size="icon"
          type="button"
          variant="outline"
        >
          {isDecliningInvitation && isCurrentInvitationPending ? (
            <Spinner aria-hidden className="size-4" />
          ) : (
            <X className="size-4" />
          )}
        </Button>
      </div>
    </div>
  );
}

export function ResearchGroupInvitationList({
  invitations,
  isAcceptingInvitation,
  isDecliningInvitation,
  isInvitationActionPending,
  pendingInvitationId,
  status,
  onAcceptInvitation,
  onDeclineInvitation,
}: ResearchGroupInvitationListProps) {
  if (status === 'loading') {
    return <ResearchGroupInvitationsLoading />;
  }

  if (status === 'empty') {
    return <ResearchGroupInvitationsEmpty />;
  }

  if (status === 'error') {
    return null;
  }

  return (
    <div className="space-y-2">
      {invitations.map((invitation) => (
        <ResearchGroupInvitationCard
          invitation={invitation}
          isAcceptingInvitation={isAcceptingInvitation}
          isDecliningInvitation={isDecliningInvitation}
          isInvitationActionPending={isInvitationActionPending}
          key={invitation.id}
          pendingInvitationId={pendingInvitationId}
          onAcceptInvitation={onAcceptInvitation}
          onDeclineInvitation={onDeclineInvitation}
        />
      ))}
    </div>
  );
}
