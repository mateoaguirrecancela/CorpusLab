import { type FieldErrors } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { FormFieldControl } from '@/components/common/FormFieldControl';
import { DialogActionButton } from '@/modules/researchgroup/components/DialogActionButton';
import { type JoinResearchGroupByCodeFormValues } from '@/modules/researchgroup/schemas/researchGroupFormSchemas';

type JoinResearchGroupByCodeFormProps = Readonly<{
  canJoinByCode: boolean;
  errors: FieldErrors<JoinResearchGroupByCodeFormValues>;
  invitationCode: string;
  isJoiningByCode: boolean;
  onInvitationCodeChange: (value: string) => void;
  onSubmit: () => void;
}>;

export function JoinResearchGroupByCodeForm({
  canJoinByCode,
  errors,
  invitationCode,
  isJoiningByCode,
  onInvitationCodeChange,
  onSubmit,
}: JoinResearchGroupByCodeFormProps) {
  const { t } = useTranslation();

  return (
    <div className="space-y-3">
      <div className="flex items-end gap-2">
        <FormFieldControl
          className="flex-1"
          id="join-by-code"
          inputProps={{
            autoComplete: 'off',
            maxLength: 64,
            placeholder: t('researchGroup.invitationsDialog.codePlaceholder'),
            'aria-invalid': Boolean(errors.invitationCode),
          }}
          label={t('researchGroup.invitationsDialog.codeLabel')}
          message={errors.invitationCode?.message}
          onValueChange={onInvitationCodeChange}
          value={invitationCode}
        />

        <DialogActionButton
          disabled={!canJoinByCode}
          isPending={isJoiningByCode}
          label={t('researchGroup.invitationsDialog.joinSubmit')}
          loadingLabel={t('researchGroup.invitationsDialog.joining')}
          onClick={onSubmit}
        />
      </div>
    </div>
  );
}
