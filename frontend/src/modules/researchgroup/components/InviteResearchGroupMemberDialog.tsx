import { useState } from 'react';
import { Check, Copy } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';
import { FormFieldControl } from '@/components/common/FormFieldControl';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { Spinner } from '@/components/ui/spinner';
import { useInviteResearchGroupMemberMutation } from '@/modules/researchgroup/hooks/useResearchGroupQueries';
import { getInviteResearcherErrorMessage } from '@/modules/researchgroup/services/researchGroupService';
import { type ResearchGroupMemberRole } from '@/modules/researchgroup/types/researchGroup';

type InviteResearchGroupMemberDialogProps = {
  groupId: number;
  invitationCode: string;
  trigger: React.ReactNode;
};

const INVITABLE_ROLES: ResearchGroupMemberRole[] = ['ADMIN', 'ANNOTATOR'];

function getDefaultExpirationIsoString(): string {
  const expirationDate = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000);
  return expirationDate.toISOString();
}

export function InviteResearchGroupMemberDialog({
  groupId,
  invitationCode,
  trigger,
}: Readonly<InviteResearchGroupMemberDialogProps>) {
  const { t } = useTranslation();
  const inviteMutation = useInviteResearchGroupMemberMutation(groupId);
  const [open, setOpen] = useState(false);
  const [email, setEmail] = useState('');
  const [role, setRole] = useState<ResearchGroupMemberRole>('ANNOTATOR');
  const [hasCopiedCode, setHasCopiedCode] = useState(false);
  const isSaving = inviteMutation.isPending;

  const isEmailInvalid =
    email.trim().length > 0 && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim());

  const canSave = email.trim().length > 0 && role.length > 0 && !isSaving && !isEmailInvalid;

  const resetForm = () => {
    setEmail('');
    setRole('ANNOTATOR');
    setHasCopiedCode(false);
  };

  const handleCopyInvitationCode = async () => {
    if (!invitationCode) {
      return;
    }

    try {
      await navigator.clipboard.writeText(invitationCode);
      setHasCopiedCode(true);
      window.setTimeout(() => {
        setHasCopiedCode(false);
      }, 1500);
    } catch {
      setHasCopiedCode(false);
    }
  };

  const handleOpenChange = (nextOpen: boolean) => {
    if (!nextOpen) {
      resetForm();
    }
    setOpen(nextOpen);
  };

  const handleSubmit = async () => {
    if (!canSave) {
      return;
    }

    try {
      await inviteMutation.mutateAsync({
        email: email.trim(),
        role,
        expiresAt: getDefaultExpirationIsoString(),
      });
      toast.success(t('researchGroup.invite.success'));
      setEmail('');
      setRole('ANNOTATOR');
    } catch (error) {
      toast.error(getInviteResearcherErrorMessage(error));
    }
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger render={trigger as React.JSX.Element} />

      <DialogContent className="sm:max-w-xl">
        <DialogHeader>
          <DialogTitle>{t('researchGroup.invite.title')}</DialogTitle>
        </DialogHeader>

        <div className="my-8 space-y-4">
          <FormFieldControl
            controlType="custom"
            id="invite-member-code"
            label={t('researchGroup.detail.invitationCode')}
            renderControl={() => (
              <Button
                aria-label={t('researchGroup.detail.copyCode')}
                className="h-10 w-full justify-between rounded-md border-dashed border-border bg-background px-4 text-sm font-semibold text-muted-foreground cursor-pointer"
                onClick={() => void handleCopyInvitationCode()}
                type="button"
              >
                <span className="truncate text-left">{invitationCode}</span>
                {hasCopiedCode ? <Check className="size-4" /> : <Copy className="size-4" />}
              </Button>
            )}
          />

          <FormFieldControl
            id="invite-member-email"
            inputProps={{
              autoFocus: true,
              maxLength: 254,
              placeholder: t('researchGroup.invite.emailPlaceholder'),
              required: true,
            }}
            inputType="email"
            label={t('researchGroup.invite.emailLabel')}
            message={isEmailInvalid ? t('researchGroup.invite.invalidEmail') : undefined}
            onValueChange={setEmail}
            value={email}
          />

          <FormFieldControl
            controlType="select"
            id="invite-member-role"
            label={t('researchGroup.invite.roleLabel')}
            onValueChange={(value) => setRole(value as ResearchGroupMemberRole)}
            value={role}
          >
            {INVITABLE_ROLES.map((roleOption) => (
              <option key={roleOption} value={roleOption}>
                {t(`researchGroup.roles.${roleOption}`)}
              </option>
            ))}
          </FormFieldControl>
        </div>

        <DialogFooter>
          <Button
            className="h-10 min-w-28 rounded-md bg-primary text-sm font-semibold text-white transition-colors hover:bg-primary-strong disabled:bg-secondary cursor-pointer"
            disabled={!canSave}
            onClick={() => void handleSubmit()}
            type="button"
          >
            {isSaving ? (
              <span className="inline-flex items-center gap-2">
                <Spinner aria-hidden className="size-4" />
                {t('common.actions.saving')}
              </span>
            ) : (
              t('researchGroup.invite.submit')
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
