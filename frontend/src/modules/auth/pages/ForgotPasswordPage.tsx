import { ForgotPasswordForm } from '@/modules/auth/components/ForgotPasswordForm'
import { useForgotPasswordForm } from '@/modules/auth/hooks/useForgotPasswordForm'

export default function ForgotPasswordPage() {
  const {
    form,
    canSubmit,
    isSubmitting,
    errorMessage,
    successMessage,
    updateField,
    handleSubmit,
  } = useForgotPasswordForm()

  return (
    <section className="signup-card w-full max-w-md rounded-xl border border-[color:var(--cl-line)] bg-white/80 p-6 shadow-[0_20px_60px_-45px_rgba(15,23,42,0.75)] backdrop-blur sm:p-8">
      <h1 className="reveal text-center text-4xl font-extrabold tracking-tight text-[color:var(--cl-primary)]">Forgot Password</h1>

      <ForgotPasswordForm
        canSubmit={canSubmit}
        errorMessage={errorMessage}
        form={form}
        isSubmitting={isSubmitting}
        successMessage={successMessage}
        onFieldChange={updateField}
        onSubmit={handleSubmit}
      />
    </section>
  )
}