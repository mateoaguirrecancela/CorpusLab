import { type ReactNode } from 'react';
import { cn } from '@/lib/utils';

type DashboardPanelProps = Readonly<{
  action?: ReactNode;
  children: ReactNode;
  className?: string;
  title: string;
}>;

export function DashboardPanel({ action, children, className, title }: DashboardPanelProps) {
  return (
    <section className={cn('rounded-xl border border-border bg-surface-base p-5', className)}>
      <header className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-lg font-black leading-tight text-primary">{title}</h2>

        {action ? <div className="shrink-0">{action}</div> : null}
      </header>

      {children}
    </section>
  );
}
