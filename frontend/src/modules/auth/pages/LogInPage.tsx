import { AuthCard } from '@/modules/auth/components/AuthCard';
import { LogInForm } from '@/modules/auth/components/LogInForm';
import { useTranslation } from 'react-i18next';
import { useLogInForm } from '@/modules/auth/hooks/useLogInForm';

export default function LogInPage() {
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
    handleOAuthClick,
  } = useLogInForm();

  return (
    <AuthCard title={t('auth.login.title')}>
      <LogInForm
        canSubmit={canSubmit}
        errorMessage={errorMessage}
        fieldErrors={fieldErrors}
        form={form}
        isSubmitting={isSubmitting}
        successMessage={successMessage}
        onFieldChange={updateField}
        onOAuthClick={handleOAuthClick}
        onSubmit={handleSubmit}
      />
    </AuthCard>
  );
}
