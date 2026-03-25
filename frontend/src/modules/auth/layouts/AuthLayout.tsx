import { Outlet } from 'react-router'
import { AppFooter } from '@/components/layout/AppFooter'
import { AuthHeader } from '@/modules/auth/components/AuthHeader'

export default function AuthLayout() {
  return (
    <div className="flex min-h-screen flex-col bg-[radial-gradient(circle_at_top,_#ffffff_0%,_#eceffc_42%,_#e5e9fb_100%)] text-[color:var(--cl-neutral)]">
      <AuthHeader />

      <main className="mx-auto flex w-full max-w-6xl flex-1 items-center justify-center px-4 py-10 sm:py-14">
        <Outlet />
      </main>

      <AppFooter />
    </div>
  )
}
