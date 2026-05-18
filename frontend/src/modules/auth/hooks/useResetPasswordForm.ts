import { type FormEvent, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { buildInitialResetPasswordState } from '@/modules/auth/constants/resetPassword';
import {
  createResetPasswordSchema,
  type ResetPasswordFormValues,
} from '@/modules/auth/schemas/authFormSchemas';
import { getResetPasswordErrorMessage, resetPassword } from '@/modules/auth/services/authService';
import { type ResetPasswordFormState } from '@/modules/auth/types/resetPassword';
import { useAuthFormController } from '@/modules/auth/hooks/useAuthFormController';

const RESET_PASSWORD_FIELDS = [
  'confirmPassword',
  'newPassword',
  'token',
] as const satisfies readonly (keyof ResetPasswordFormValues)[];

export function useResetPasswordForm(initialToken: string) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const resetPasswordSchema = useMemo(() => createResetPasswordSchema(t), [t]);
  const initialFormState = useMemo(
    () => buildInitialResetPasswordState(initialToken),
    [initialToken],
  );
  const {
    form,
    canSubmit,
    fieldErrors,
    isSubmitting,
    errorMessage,
    setErrorMessage,
    submitForm,
    updateField,
    methods: { resetField },
  } = useAuthFormController<ResetPasswordFormValues>({
    defaultValues: initialFormState,
    fieldNames: RESET_PASSWORD_FIELDS,
    initialErrorMessage:
      initialToken.trim().length === 0 ? t('auth.resetPassword.missingToken') : '',
    schema: resetPasswordSchema,
  });

  const resetPasswordValues: ResetPasswordFormState = form;

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    await submitForm({
      event,
      invalidMessage: t('auth.resetPassword.required'),
      onError: (error) => setErrorMessage(getResetPasswordErrorMessage(error)),
      onValid: async (values) => {
        await resetPassword({
          token: values.token,
          newPassword: values.newPassword,
        });
        resetField('newPassword');
        resetField('confirmPassword');
        navigate('/auth/login', { replace: true });
      },
    });
  };

  return {
    form: resetPasswordValues,
    canSubmit,
    fieldErrors,
    isSubmitting,
    errorMessage,
    updateField,
    handleSubmit,
  };
}
