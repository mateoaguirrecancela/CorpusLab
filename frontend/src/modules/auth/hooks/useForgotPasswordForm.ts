import { type FormEvent, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { INITIAL_FORGOT_PASSWORD_STATE } from '@/modules/auth/constants/forgotPassword';
import {
  createForgotPasswordSchema,
  type ForgotPasswordFormValues,
} from '@/modules/auth/schemas/authFormSchemas';
import {
  getForgotPasswordErrorMessage,
  requestPasswordReset,
} from '@/modules/auth/services/authService';
import { type ForgotPasswordFormState } from '@/modules/auth/types/forgotPassword';
import { useAuthFormController } from '@/modules/auth/hooks/useAuthFormController';

const FORGOT_PASSWORD_FIELDS = [
  'email',
] as const satisfies readonly (keyof ForgotPasswordFormValues)[];

export function useForgotPasswordForm() {
  const { t } = useTranslation();
  const forgotPasswordSchema = useMemo(() => createForgotPasswordSchema(t), [t]);
  const {
    form,
    canSubmit,
    fieldErrors,
    isSubmitting,
    errorMessage,
    successMessage,
    setErrorMessage,
    setSuccessMessage,
    submitForm,
    updateField,
  } = useAuthFormController<ForgotPasswordFormValues>({
    defaultValues: INITIAL_FORGOT_PASSWORD_STATE,
    fieldNames: FORGOT_PASSWORD_FIELDS,
    schema: forgotPasswordSchema,
  });

  const forgotPasswordValues: ForgotPasswordFormState = form;

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    await submitForm({
      event,
      invalidMessage: t('auth.forgotPassword.required'),
      onError: (error) => setErrorMessage(getForgotPasswordErrorMessage(error)),
      onValid: async (values) => {
        const response = await requestPasswordReset(values);
        setSuccessMessage(response.message);
      },
    });
  };

  return {
    form: forgotPasswordValues,
    canSubmit,
    fieldErrors,
    isSubmitting,
    errorMessage,
    successMessage,
    updateField,
    handleSubmit,
  };
}
