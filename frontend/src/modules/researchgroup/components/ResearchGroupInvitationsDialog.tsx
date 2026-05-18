import { useTranslation } from 'react-i18next';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { JoinResearchGroupByCodeForm } from '@/modules/researchgroup/components/JoinResearchGroupByCodeForm';
import { ResearchGroupInvitationList } from '@/modules/researchgroup/components/ResearchGroupInvitationList';
import { useResearchGroupInvitationsDialog } from '@/modules/researchgroup/hooks/useResearchGroupInvitationsDialog';

type ResearchGroupInvitationsDialogProps = {
  trigger: React.ReactNode;
};

export function ResearchGroupInvitationsDialog({
  trigger,
}: Readonly<ResearchGroupInvitationsDialogProps>) {
  const { t } = useTranslation();
  const {
    acceptInvitation,
    canJoinByCode,
    declineInvitation,
    invitationCode,
    invitations,
    isAcceptingInvitation,
    isDecliningInvitation,
    isInvitationActionPending,
    isJoiningByCode,
    joinByCodeErrors,
    listStatus,
    onOpenChange,
    open,
    pendingInvitationId,
    setInvitationCode,
    submitJoinByCode,
  } = useResearchGroupInvitationsDialog();

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogTrigger render={trigger as React.JSX.Element} />

      <DialogContent className="max-h-[85vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{t('researchGroup.invitationsDialog.title')}</DialogTitle>
        </DialogHeader>

        <div className="my-6 space-y-6">
          <JoinResearchGroupByCodeForm
            canJoinByCode={canJoinByCode}
            errors={joinByCodeErrors}
            invitationCode={invitationCode}
            isJoiningByCode={isJoiningByCode}
            onInvitationCodeChange={setInvitationCode}
            onSubmit={submitJoinByCode}
          />

          <div className="space-y-3">
            <h3 className="text-muted-foreground text-sm font-bold tracking-[0.08em] uppercase">
              {t('researchGroup.invitationsDialog.myInvitations')}
            </h3>

            <ResearchGroupInvitationList
              invitations={invitations}
              isAcceptingInvitation={isAcceptingInvitation}
              isDecliningInvitation={isDecliningInvitation}
              isInvitationActionPending={isInvitationActionPending}
              pendingInvitationId={pendingInvitationId}
              status={listStatus}
              onAcceptInvitation={(invitationId) => void acceptInvitation(invitationId)}
              onDeclineInvitation={(invitationId) => void declineInvitation(invitationId)}
            />
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
