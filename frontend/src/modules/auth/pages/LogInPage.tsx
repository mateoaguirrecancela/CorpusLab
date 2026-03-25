import { LogInForm } from '@/modules/auth/components/LogInForm'
import { useSignInForm } from '@/modules/auth/hooks/useLogInForm'

export default function LogInPage() {
  const {
    form,
    canSubmit,
    isSubmitting,
    isLoggedIn,
    errorMessage,
    successMessage,
    updateField,
    handleSubmit,
    handleLogout,
  } = useSignInForm()

  return (
    <section className="signup-card w-full max-w-md rounded-xl border border-[color:var(--cl-line)] bg-white/80 p-6 shadow-[0_20px_60px_-45px_rgba(15,23,42,0.75)] backdrop-blur sm:p-8">
      <h1 className="reveal text-center text-5xl font-extrabold tracking-tight text-[color:var(--cl-primary)]">Log In</h1>

      <LogInForm
        canSubmit={canSubmit}
        errorMessage={errorMessage}
        form={form}
        isLoggedIn={isLoggedIn}
        isSubmitting={isSubmitting}
        successMessage={successMessage}
        onFieldChange={updateField}
        onLogout={handleLogout}
        onSubmit={handleSubmit}
      />
    </section>
  )
}
