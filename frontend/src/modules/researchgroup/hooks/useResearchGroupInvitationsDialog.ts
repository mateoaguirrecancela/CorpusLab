import { useMemo, useState } from 'react';
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm, useWatch } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';
import { useToastMessages } from '@/hooks/useToastMessages';
import {
  useAcceptResearchGroupInvitationMutation,
  useDeclineResearchGroupInvitationMutation,
  useJoinResearchGroupByCodeMutation,
  useResearchGroupInvitationsQuery,
} from '@/modules/researchgroup/hooks/useResearchGroupQueries';
import {
  createJoinResearchGroupByCodeSchema,
  type JoinResearchGroupByCodeFormValues,
} from '@/modules/researchgroup/schemas/researchGroupFormSchemas';
import {
  getAcceptInvitationErrorMessage,
  getDeclineInvitationErrorMessage,
  getInvitationsErrorMessage,
  getJoinByCodeErrorMessage,
} from '@/modules/researchgroup/services/researchGroupService';
import {
  canSubmitJoinByCode,
  DEFAULT_JOIN_BY_CODE_FORM,
  DIRTY_VALIDATED_FIELD_OPTIONS,
  toJoinByCodePayload,
} from '@/modules/researchgroup/utils/researchGroupForm';
import {
  getInvitationListStatus,
  getPendingInvitationId,
} from '@/modules/researchgroup/utils/researchGroupInvitations';

type InvitationActionConfig = {
  invitationId: number;
  getErrorMessage: (error: unknown) => string;
  mutate: (invitationId: number) => Promise<unknown>;
  successMessage: string;
};

export function useResearchGroupInvitationsDialog() {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const joinByCodeSchema = useMemo(() => createJoinResearchGroupByCodeSchema(t), [t]);
  const joinByCodeForm = useForm<JoinResearchGroupByCodeFormValues>({
    defaultValues: DEFAULT_JOIN_BY_CODE_FORM,
    mode: 'onChange',
    resolver: zodResolver(joinByCodeSchema),
  });
  const {
    control,
    formState: { errors, isValid },
    handleSubmit,
    reset,
    setValue,
  } = joinByCodeForm;
  const invitationCode = useWatch({ control, name: 'invitationCode' });
  const { data: invitations = [], isLoading, isError, error } = useResearchGroupInvitationsQuery();
  const joinByCodeMutation = useJoinResearchGroupByCodeMutation();
  const acceptInvitationMutation = useAcceptResearchGroupInvitationMutation();
  const declineInvitationMutation = useDeclineResearchGroupInvitationMutation();
  const invitationsErrorMessage = isError ? getInvitationsErrorMessage(error) : '';
  const isInvitationActionPending =
    acceptInvitationMutation.isPending || declineInvitationMutation.isPending;
  const listStatus = getInvitationListStatus({
    invitations,
    isError,
    isLoading,
  });
  const pendingInvitationId = getPendingInvitationId(
    acceptInvitationMutation.variables,
    declineInvitationMutation.variables,
  );
  const canJoinByCode = canSubmitJoinByCode({
    invitationCode,
    isPending: joinByCodeMutation.isPending,
    isValid,
  });

  useToastMessages({
    errorMessage: invitationsErrorMessage,
    errorToastId: 'research-group-invitations-load-error',
  });

  const resetJoinByCodeForm = () => reset(DEFAULT_JOIN_BY_CODE_FORM);

  const handleOpenChange = (nextOpen: boolean) => {
    if (!nextOpen) {
      resetJoinByCodeForm();
    }

    setOpen(nextOpen);
  };

  const setInvitationCode = (nextCode: string) => {
    setValue('invitationCode', nextCode, DIRTY_VALIDATED_FIELD_OPTIONS);
  };

  const joinByCode = async (values: JoinResearchGroupByCodeFormValues) => {
    try {
      await joinByCodeMutation.mutateAsync(toJoinByCodePayload(values));
      toast.success(t('researchGroup.invitationsDialog.joinSuccess'));
      resetJoinByCodeForm();
    } catch (joinError) {
      toast.error(getJoinByCodeErrorMessage(joinError));
    }
  };

  const runInvitationAction = async ({
    invitationId,
    getErrorMessage,
    mutate,
    successMessage,
  }: InvitationActionConfig) => {
    if (isInvitationActionPending) {
      return;
    }

    try {
      await mutate(invitationId);
      toast.success(successMessage);
    } catch (error) {
      toast.error(getErrorMessage(error));
    }
  };

  const acceptInvitation = (invitationId: number) =>
    runInvitationAction({
      invitationId,
      getErrorMessage: getAcceptInvitationErrorMessage,
      mutate: acceptInvitationMutation.mutateAsync,
      successMessage: t('researchGroup.invitationsDialog.acceptSuccess'),
    });

  const declineInvitation = (invitationId: number) =>
    runInvitationAction({
      invitationId,
      getErrorMessage: getDeclineInvitationErrorMessage,
      mutate: declineInvitationMutation.mutateAsync,
      successMessage: t('researchGroup.invitationsDialog.declineSuccess'),
    });

  return {
    acceptInvitation,
    canJoinByCode,
    declineInvitation,
    invitationCode,
    invitations,
    isAcceptingInvitation: acceptInvitationMutation.isPending,
    isDecliningInvitation: declineInvitationMutation.isPending,
    isInvitationActionPending,
    isJoiningByCode: joinByCodeMutation.isPending,
    joinByCodeErrors: errors,
    listStatus,
    onOpenChange: handleOpenChange,
    open,
    pendingInvitationId,
    setInvitationCode,
    submitJoinByCode: () => void handleSubmit(joinByCode)(),
  };
}
