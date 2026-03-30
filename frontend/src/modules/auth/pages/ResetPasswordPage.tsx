import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router';
import { ResetPasswordForm } from '@/modules/auth/components/ResetPasswordForm';
import { useResetPasswordForm } from '@/modules/auth/hooks/useResetPasswordForm';

export default function ResetPasswordPage() {
  const { t } = useTranslation();
  const [searchParams] = useSearchParams();
  const initialToken = useMemo(() => searchParams.get('token') ?? '', [searchParams]);
  const { form, canSubmit, isSubmitting, errorMessage, updateField, handleSubmit } =
    useResetPasswordForm(initialToken);

  return (
    <section className="signup-card w-full max-w-md rounded-xl border border-[color:var(--cl-line)] bg-white/80 p-6 shadow-[0_20px_60px_-45px_rgba(15,23,42,0.75)] backdrop-blur sm:p-8">
      <h1 className="reveal text-center text-4xl font-extrabold tracking-tight text-[color:var(--cl-primary)]">
        {t('auth.resetPassword.title')}
      </h1>

      <ResetPasswordForm
        canSubmit={canSubmit}
        errorMessage={errorMessage}
        form={form}
        isSubmitting={isSubmitting}
        onFieldChange={updateField}
        onSubmit={handleSubmit}
      />
    </section>
  );
}
