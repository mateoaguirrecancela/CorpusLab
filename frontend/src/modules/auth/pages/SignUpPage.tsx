import { AuthCard } from '@/modules/auth/components/AuthCard';
import { SignUpForm } from '@/modules/auth/components/SignUpForm';
import { useTranslation } from 'react-i18next';
import { useSignUpForm } from '@/modules/auth/hooks/useSignUpForm';

export default function SignUpPage() {
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
  } = useSignUpForm();

  return (
    <AuthCard title={t('auth.signup.title')}>
      <SignUpForm
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
