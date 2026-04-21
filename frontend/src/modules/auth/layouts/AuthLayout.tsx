import { Outlet } from 'react-router';
import { AuthHeader } from '@/modules/auth/components/AuthHeader';

export default function AuthLayout() {
  return (
    <div className="flex min-h-screen flex-col bg-[image:var(--auth-layout-gradient)] text-foreground">
      <AuthHeader />

      <main className="mx-auto flex w-full max-w-6xl flex-1 items-center justify-center px-4 py-10 sm:py-14">
        <Outlet />
      </main>
    </div>
  );
}
