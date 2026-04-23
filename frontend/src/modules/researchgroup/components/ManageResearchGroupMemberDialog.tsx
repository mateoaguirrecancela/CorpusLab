import { useState } from 'react';
import { ShieldCheck, Trash2 } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';
import { ConfirmDestructiveDialog } from '@/components/common/ConfirmDestructiveDialog';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { Spinner } from '@/components/ui/spinner';
import {
  useRemoveResearchGroupMemberMutation,
  useUpdateResearchGroupMemberRoleMutation,
} from '@/modules/researchgroup/hooks/useResearchGroupQueries';
import {
  getRemoveMemberErrorMessage,
  getUpdateMemberRoleErrorMessage,
} from '@/modules/researchgroup/services/researchGroupService';
import { type ResearchGroupMember } from '@/modules/researchgroup/types/researchGroup';

type ManageResearchGroupMemberDialogProps = {
  groupId: number;
  member: ResearchGroupMember;
  trigger: React.ReactNode;
};

export function ManageResearchGroupMemberDialog({
  groupId,
  member,
  trigger,
}: Readonly<ManageResearchGroupMemberDialogProps>) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const [confirmRemoveOpen, setConfirmRemoveOpen] = useState(false);

  const updateRoleMutation = useUpdateResearchGroupMemberRoleMutation(groupId);
  const removeMemberMutation = useRemoveResearchGroupMemberMutation(groupId);

  const isUpdatingRole = updateRoleMutation.isPending;
  const isRemovingMember = removeMemberMutation.isPending;

  const isBusy = isUpdatingRole || isRemovingMember;
  const targetRole: 'ADMIN' | 'ANNOTATOR' = member.role === 'ADMIN' ? 'ANNOTATOR' : 'ADMIN';

  const handleOpenChange = (nextOpen: boolean) => {
    setOpen(nextOpen);
  };

  const handleUpdateRole = async (nextRole: 'ADMIN' | 'ANNOTATOR') => {
    if (isBusy || member.role === nextRole) {
      return;
    }

    try {
      await updateRoleMutation.mutateAsync({
        memberUserId: member.userId,
        role: nextRole,
      });
      setOpen(false);
      toast.success(t('researchGroup.detail.manage.updateSuccess'));
    } catch (error) {
      toast.error(getUpdateMemberRoleErrorMessage(error));
    }
  };

  const handleRemoveMember = async () => {
    if (isBusy) {
      return;
    }

    try {
      await removeMemberMutation.mutateAsync(member.userId);
      setConfirmRemoveOpen(false);
      setOpen(false);
      toast.success(t('researchGroup.detail.manage.remove'));
    } catch (error) {
      toast.error(getRemoveMemberErrorMessage(error));
    }
  };

  return (
    <Popover open={open} onOpenChange={handleOpenChange}>
      <PopoverTrigger render={trigger as React.JSX.Element} />

      <PopoverContent
        align="end"
        className="w-64 rounded-xl border border-border bg-surface-base p-1.5"
      >
        <button
          className="flex h-9 w-full items-center gap-2 rounded-md px-3 text-left text-sm font-medium text-foreground transition-colors hover:bg-accent disabled:cursor-not-allowed disabled:opacity-60 cursor-pointer"
          disabled={isBusy}
          onClick={() => void handleUpdateRole(targetRole)}
          type="button"
        >
          {isUpdatingRole ? (
            <Spinner aria-hidden className="size-4" />
          ) : (
            <ShieldCheck className="size-4" />
          )}
          {t('researchGroup.detail.manage.setRole', {
            role: t(`researchGroup.roles.${targetRole}`),
          })}
        </button>

        <button
          className="flex h-9 w-full items-center gap-2 rounded-md px-3 text-left text-sm font-medium text-destructive transition-colors hover:bg-danger-soft disabled:cursor-not-allowed disabled:opacity-70 cursor-pointer"
          disabled={isBusy}
          onClick={() => {
            setOpen(false);
            setConfirmRemoveOpen(true);
          }}
          type="button"
        >
          <Trash2 className="size-4" />
          {t('researchGroup.detail.manage.remove')}
        </button>
      </PopoverContent>

      <ConfirmDestructiveDialog
        confirmLabel={t('researchGroup.detail.manage.remove')}
        confirmingLabel={t('researchGroup.detail.manage.removing')}
        description={t('researchGroup.detail.manage.confirmRemove', {
          name: `${member.firstName} ${member.lastName}`,
        })}
        isConfirming={isRemovingMember}
        onConfirm={handleRemoveMember}
        onOpenChange={setConfirmRemoveOpen}
        open={confirmRemoveOpen}
        title={t('researchGroup.detail.manage.confirmRemoveTitle')}
      />
    </Popover>
  );
}
