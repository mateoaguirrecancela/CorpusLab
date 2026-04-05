import { AuthCard } from '@/modules/auth/components/AuthCard';
import { LogInForm } from '@/modules/auth/components/LogInForm';
import { useTranslation } from 'react-i18next';
import { useSignInForm } from '@/modules/auth/hooks/useLogInForm';

export default function LogInPage() {
  const { t } = useTranslation();
  const {
    form,
    canSubmit,
    isSubmitting,
    errorMessage,
    successMessage,
    updateField,
    handleSubmit,
    handleOAuthClick,
  } = useSignInForm();

  return (
    <AuthCard title={t('auth.login.title')}>
      <LogInForm
        canSubmit={canSubmit}
        errorMessage={errorMessage}
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
