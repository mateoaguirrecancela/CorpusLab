import { type FormEvent, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { buildInitialResetPasswordState } from '@/modules/auth/constants/resetPassword';
import { getResetPasswordErrorMessage, resetPassword } from '@/modules/auth/services/authService';
import { type ResetPasswordFormState } from '@/modules/auth/types/resetPassword';

export function useResetPasswordForm(initialToken: string) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [form, setForm] = useState<ResetPasswordFormState>(() =>
    buildInitialResetPasswordState(initialToken),
  );
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState(
    initialToken.trim().length === 0 ? t('auth.resetPassword.missingToken') : '',
  );
  const [successMessage, setSuccessMessage] = useState('');

  const canSubmit = useMemo(
    () =>
      form.token.trim().length >= 16 &&
      form.newPassword.length >= 8 &&
      form.confirmPassword === form.newPassword &&
      !isSubmitting,
    [form, isSubmitting],
  );

  const updateField = <K extends keyof ResetPasswordFormState>(
    field: K,
    value: ResetPasswordFormState[K],
  ) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (form.token.trim().length === 0) {
      setErrorMessage(t('auth.resetPassword.missingToken'));
      setSuccessMessage('');
      return;
    }

    if (form.confirmPassword !== form.newPassword) {
      setErrorMessage(t('auth.resetPassword.passwordMismatch'));
      setSuccessMessage('');
      return;
    }

    if (!canSubmit) {
      setErrorMessage(t('auth.resetPassword.required'));
      setSuccessMessage('');
      return;
    }

    setIsSubmitting(true);
    setErrorMessage('');
    setSuccessMessage('');

    try {
      await resetPassword({
        token: form.token,
        newPassword: form.newPassword,
      });
      setForm((current) => ({ ...current, newPassword: '', confirmPassword: '' }));
      navigate('/auth/login', { replace: true });
    } catch (error) {
      setErrorMessage(getResetPasswordErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  };

  return {
    form,
    canSubmit,
    isSubmitting,
    errorMessage,
    successMessage,
    updateField,
    handleSubmit,
  };
}
