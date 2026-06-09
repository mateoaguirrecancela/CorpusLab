import { type ReactNode } from 'react';
import { cn } from '@/shared/utils/cn';

type AuthCardProps = {
  title: string;
  children: ReactNode;
  className?: string;
};

export function AuthCard({ title, children, className }: Readonly<AuthCardProps>) {
  return (
    <section
      className={cn(
        'w-full max-w-lg rounded-xl border border-border bg-surface-card p-6 shadow-[var(--shadow-elevated-card)] backdrop-blur sm:p-8',
        className,
      )}
    >
      <h1 className="text-center text-4xl font-extrabold tracking-tight text-primary">{title}</h1>

      {children}
    </section>
  );
}
