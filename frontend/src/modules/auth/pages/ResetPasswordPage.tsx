import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router';
import { AuthCard } from '@/modules/auth/components/AuthCard';
import { ResetPasswordForm } from '@/modules/auth/components/ResetPasswordForm';
import { useResetPasswordForm } from '@/modules/auth/hooks/useResetPasswordForm';

export default function ResetPasswordPage() {
  const { t } = useTranslation();
  const [searchParams] = useSearchParams();
  const initialToken = useMemo(() => searchParams.get('token') ?? '', [searchParams]);
  const { form, canSubmit, isSubmitting, errorMessage, updateField, handleSubmit } =
    useResetPasswordForm(initialToken);

  return (
    <AuthCard title={t('auth.resetPassword.title')}>
      <ResetPasswordForm
        canSubmit={canSubmit}
        errorMessage={errorMessage}
        form={form}
        isSubmitting={isSubmitting}
        onFieldChange={updateField}
        onSubmit={handleSubmit}
      />
    </AuthCard>
  );
}
