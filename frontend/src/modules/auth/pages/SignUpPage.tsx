import { SignUpForm } from '@/modules/auth/components/SignUpForm'
import { useSignUpForm } from '@/modules/auth/hooks/useSignUpForm'

export default function SignUpPage() {
  const {
    form,
    canSubmit,
    isSubmitting,
    errorMessage,
    updateField,
    handleSubmit,
  } = useSignUpForm()

  return (
    <section className="signup-card w-full max-w-xl rounded-xl border border-[color:var(--cl-line)] bg-white/80 p-6 shadow-[0_20px_60px_-45px_rgba(15,23,42,0.75)] backdrop-blur">
      <h1 className="reveal text-center text-4xl font-extrabold tracking-tight text-[color:var(--cl-primary)]">Sign Up</h1>

      <SignUpForm
        canSubmit={canSubmit}
        errorMessage={errorMessage}
        form={form}
        isSubmitting={isSubmitting}
        onFieldChange={updateField}
        onSubmit={handleSubmit}
      />
    </section>
  )
}
