import { AuthCard } from '@/modules/auth/components/AuthCard';
import { ForgotPasswordForm } from '@/modules/auth/components/ForgotPasswordForm';
import { useTranslation } from 'react-i18next';
import { useForgotPasswordForm } from '@/modules/auth/hooks/useForgotPasswordForm';

export default function ForgotPasswordPage() {
  const { t } = useTranslation();
  const {
    form,
    canSubmit,
    fieldErrors,
    isSubmitting,
    errorMessage,
    successMessage,
    updateField,
    handleSubmit,
  } = useForgotPasswordForm();

  return (
    <AuthCard title={t('auth.forgotPassword.title')}>
      <ForgotPasswordForm
        canSubmit={canSubmit}
        errorMessage={errorMessage}
        fieldErrors={fieldErrors}
        form={form}
        isSubmitting={isSubmitting}
        successMessage={successMessage}
        onFieldChange={updateField}
        onSubmit={handleSubmit}
      />
    </AuthCard>
  );
}
