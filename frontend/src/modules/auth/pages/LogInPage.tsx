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
    <section className="signup-card w-full max-w-lg rounded-xl border border-[color:var(--cl-line)] bg-white/80 p-6 shadow-[0_20px_60px_-45px_rgba(15,23,42,0.75)] backdrop-blur sm:p-8">
      <h1 className="reveal text-center text-4xl font-extrabold tracking-tight text-[color:var(--cl-primary)]">
        {t('auth.login.title')}
      </h1>

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
    </section>
  );
}
