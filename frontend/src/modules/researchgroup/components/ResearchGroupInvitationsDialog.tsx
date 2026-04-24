import { useEffect, useMemo, useState } from 'react';
import { Check, UserRound, X } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';
import { FormFieldControl } from '@/components/common/FormFieldControl';
import { RoleBadge } from '@/components/common/RoleBadge';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { Spinner } from '@/components/ui/spinner';
import {
  useAcceptResearchGroupInvitationMutation,
  useDeclineResearchGroupInvitationMutation,
  useJoinResearchGroupByCodeMutation,
  useResearchGroupInvitationsQuery,
} from '@/modules/researchgroup/hooks/useResearchGroupQueries';
import {
  getAcceptInvitationErrorMessage,
  getDeclineInvitationErrorMessage,
  getInvitationsErrorMessage,
  getJoinByCodeErrorMessage,
} from '@/modules/researchgroup/services/researchGroupService';

type ResearchGroupInvitationsDialogProps = {
  trigger: React.ReactNode;
};

export function ResearchGroupInvitationsDialog({
  trigger,
}: Readonly<ResearchGroupInvitationsDialogProps>) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const [invitationCode, setInvitationCode] = useState('');
  const { data: invitations = [], isLoading, isError, error } = useResearchGroupInvitationsQuery();
  const joinByCodeMutation = useJoinResearchGroupByCodeMutation();
  const acceptInvitationMutation = useAcceptResearchGroupInvitationMutation();
  const declineInvitationMutation = useDeclineResearchGroupInvitationMutation();

  const invitationsErrorMessage = isError ? getInvitationsErrorMessage(error) : '';
  const canJoinByCode = invitationCode.trim().length > 0 && !joinByCodeMutation.isPending;
  const pendingInvitationId =
    acceptInvitationMutation.variables ?? declineInvitationMutation.variables;
  const isInvitationActionPending =
    acceptInvitationMutation.isPending || declineInvitationMutation.isPending;
  const sortedInvitations = useMemo(
    () =>
      [...invitations].sort(
        (left, right) => new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime(),
      ),
    [invitations],
  );

  useEffect(() => {
    if (invitationsErrorMessage.length > 0) {
      toast.error(invitationsErrorMessage, { id: 'research-group-invitations-load-error' });
    }
  }, [invitationsErrorMessage]);

  const handleOpenChange = (nextOpen: boolean) => {
    if (!nextOpen) {
      setInvitationCode('');
    }

    setOpen(nextOpen);
  };

  const handleJoinByCode = async () => {
    if (!canJoinByCode) {
      return;
    }

    try {
      await joinByCodeMutation.mutateAsync(invitationCode);
      toast.success(t('researchGroup.invitationsDialog.joinSuccess'));
      setInvitationCode('');
    } catch (joinError) {
      toast.error(getJoinByCodeErrorMessage(joinError));
    }
  };

  const handleAcceptInvitation = async (invitationId: number) => {
    if (isInvitationActionPending) {
      return;
    }

    try {
      await acceptInvitationMutation.mutateAsync(invitationId);
      toast.success(t('researchGroup.invitationsDialog.acceptSuccess'));
    } catch (acceptError) {
      toast.error(getAcceptInvitationErrorMessage(acceptError));
    }
  };

  const handleDeclineInvitation = async (invitationId: number) => {
    if (isInvitationActionPending) {
      return;
    }

    try {
      await declineInvitationMutation.mutateAsync(invitationId);
      toast.success(t('researchGroup.invitationsDialog.declineSuccess'));
    } catch (declineError) {
      toast.error(getDeclineInvitationErrorMessage(declineError));
    }
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger render={trigger as React.JSX.Element} />

      <DialogContent className="max-h-[85vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{t('researchGroup.invitationsDialog.title')}</DialogTitle>
        </DialogHeader>

        <div className="my-6 space-y-6">
          <div className="space-y-3">
            <div className="flex items-end gap-2">
              <FormFieldControl
                className="flex-1"
                id="join-by-code"
                inputProps={{
                  autoComplete: 'off',
                  maxLength: 64,
                  placeholder: t('researchGroup.invitationsDialog.codePlaceholder'),
                }}
                label={t('researchGroup.invitationsDialog.codeLabel')}
                onValueChange={setInvitationCode}
                value={invitationCode}
              />

              <Button
                className="h-10 min-w-28 cursor-pointer rounded-md bg-primary text-sm font-semibold text-white transition-colors hover:bg-primary-strong disabled:bg-muted"
                disabled={!canJoinByCode}
                onClick={() => void handleJoinByCode()}
                type="button"
              >
                {joinByCodeMutation.isPending ? (
                  <span className="inline-flex items-center gap-2">
                    <Spinner aria-hidden className="size-4" />
                    {t('researchGroup.invitationsDialog.joining')}
                  </span>
                ) : (
                  t('researchGroup.invitationsDialog.joinSubmit')
                )}
              </Button>
            </div>
          </div>

          <div className="space-y-3">
            <h3 className="text-muted-foreground text-sm font-bold tracking-[0.08em] uppercase">
              {t('researchGroup.invitationsDialog.myInvitations')}
            </h3>

            {isLoading && (
              <div className="text-muted-foreground rounded-lg border border-border bg-surface-base px-4 py-4 text-sm">
                <span className="inline-flex items-center gap-2">
                  <Spinner aria-hidden className="size-4" />
                  {t('researchGroup.invitationsDialog.loadingInvitations')}
                </span>
              </div>
            )}

            {!isLoading &&
              invitationsErrorMessage.length === 0 &&
              sortedInvitations.length === 0 && (
                <div className="text-muted-foreground rounded-lg border border-border bg-surface-base px-4 py-4 text-sm">
                  {t('researchGroup.invitationsDialog.noInvitations')}
                </div>
              )}

            {!isLoading && sortedInvitations.length > 0 && (
              <div className="space-y-2">
                {sortedInvitations.map((invitation) => (
                  <div
                    className="flex items-center gap-2 rounded-lg border border-border bg-surface-base px-4 py-3"
                    key={invitation.id}
                  >
                    <p className="text-muted-foreground min-w-0 flex flex-1 items-center gap-8 overflow-hidden text-sm">
                      <span className="text-primary shrink-0 font-semibold">
                        {invitation.researchGroupName}
                      </span>
                      <span className="inline-flex min-w-0 items-center gap-2">
                        <UserRound className="size-3.5 shrink-0" />
                        <span className="truncate">{invitation.inviterFullName}</span>
                      </span>
                      <span className="inline-flex shrink-0 items-center gap-2">
                        <RoleBadge
                          label={t(`researchGroup.roles.${invitation.role}`)}
                          role={invitation.role}
                        />
                      </span>
                    </p>

                    <div className="flex shrink-0 items-center gap-2">
                      <Button
                        aria-label={t('researchGroup.invitationsDialog.acceptAria')}
                        className="rounded-full border-success bg-surface-base text-success hover:bg-success hover:text-white cursor-pointer"
                        disabled={isInvitationActionPending}
                        onClick={() => void handleAcceptInvitation(invitation.id)}
                        size="icon"
                        type="button"
                        variant="outline"
                      >
                        {acceptInvitationMutation.isPending &&
                        pendingInvitationId === invitation.id ? (
                          <Spinner aria-hidden className="size-4" />
                        ) : (
                          <Check className="size-4" />
                        )}
                      </Button>

                      <Button
                        aria-label={t('researchGroup.invitationsDialog.declineAria')}
                        className="rounded-full border-destructive bg-surface-base text-destructive hover:bg-destructive hover:text-white cursor-pointer"
                        disabled={isInvitationActionPending}
                        onClick={() => void handleDeclineInvitation(invitation.id)}
                        size="icon"
                        type="button"
                        variant="outline"
                      >
                        {declineInvitationMutation.isPending &&
                        pendingInvitationId === invitation.id ? (
                          <Spinner aria-hidden className="size-4" />
                        ) : (
                          <X className="size-4" />
                        )}
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
