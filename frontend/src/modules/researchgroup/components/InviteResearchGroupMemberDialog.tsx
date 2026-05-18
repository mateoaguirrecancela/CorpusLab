import { useMemo, useState } from 'react';
import { zodResolver } from '@hookform/resolvers/zod';
import { Check, Copy } from 'lucide-react';
import { useForm, useWatch } from 'react-hook-form';
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
import { DialogActionButton } from '@/modules/researchgroup/components/DialogActionButton';
import { useInviteResearchGroupMemberMutation } from '@/modules/researchgroup/hooks/useResearchGroupQueries';
import {
  createInviteResearchGroupMemberSchema,
  type InviteResearchGroupMemberFormValues,
} from '@/modules/researchgroup/schemas/researchGroupFormSchemas';
import { getInviteResearcherErrorMessage } from '@/modules/researchgroup/services/researchGroupService';
import {
  canSubmitInviteMember,
  DEFAULT_INVITE_MEMBER_FORM,
  DIRTY_VALIDATED_FIELD_OPTIONS,
  INVITABLE_ROLES,
  toInviteResearchGroupMemberPayload,
} from '@/modules/researchgroup/utils/researchGroupForm';

type InviteResearchGroupMemberDialogProps = {
  groupId: number;
  invitationCode: string;
  trigger: React.ReactNode;
};

export function InviteResearchGroupMemberDialog({
  groupId,
  invitationCode,
  trigger,
}: Readonly<InviteResearchGroupMemberDialogProps>) {
  const { t } = useTranslation();
  const inviteMutation = useInviteResearchGroupMemberMutation(groupId);
  const inviteSchema = useMemo(() => createInviteResearchGroupMemberSchema(t), [t]);
  const form = useForm<InviteResearchGroupMemberFormValues>({
    defaultValues: DEFAULT_INVITE_MEMBER_FORM,
    mode: 'onChange',
    resolver: zodResolver(inviteSchema),
  });
  const {
    control,
    formState: { errors, isValid },
    handleSubmit,
    reset,
    setValue,
  } = form;
  const [open, setOpen] = useState(false);
  const [hasCopiedCode, setHasCopiedCode] = useState(false);
  const email = useWatch({ control, name: 'email' });
  const role = useWatch({ control, name: 'role' });
  const isSaving = inviteMutation.isPending;
  const canSave = canSubmitInviteMember({
    email,
    isPending: isSaving,
    isValid,
    role,
  });

  const resetForm = () => {
    reset(DEFAULT_INVITE_MEMBER_FORM);
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

  const submitForm = async (values: InviteResearchGroupMemberFormValues) => {
    try {
      await inviteMutation.mutateAsync(toInviteResearchGroupMemberPayload(values));
      toast.success(t('researchGroup.invite.success'));
      reset(DEFAULT_INVITE_MEMBER_FORM);
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
              'aria-invalid': Boolean(errors.email),
            }}
            inputType="email"
            label={t('researchGroup.invite.emailLabel')}
            message={errors.email?.message}
            onValueChange={(nextEmail) =>
              setValue('email', nextEmail, DIRTY_VALIDATED_FIELD_OPTIONS)
            }
            value={email}
          />

          <FormFieldControl
            controlType="select"
            id="invite-member-role"
            label={t('researchGroup.invite.roleLabel')}
            onValueChange={(value) =>
              setValue(
                'role',
                value as InviteResearchGroupMemberFormValues['role'],
                DIRTY_VALIDATED_FIELD_OPTIONS,
              )
            }
            selectProps={{ 'aria-invalid': Boolean(errors.role) }}
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
          <DialogActionButton
            disabled={!canSave}
            isPending={isSaving}
            label={t('researchGroup.invite.submit')}
            loadingLabel={t('common.actions.saving')}
            onClick={() => void handleSubmit(submitForm)()}
          />
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
