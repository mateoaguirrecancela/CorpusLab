import { type ReactNode } from 'react';
import { cn } from '@/shared/utils/cn';

type PageContainerProps = {
  children: ReactNode;
  className?: string;
};

export function PageContainer({ children, className }: Readonly<PageContainerProps>) {
  return (
    <section
      className={cn('mx-auto w-full max-w-7xl space-y-4 px-6 py-6 sm:px-8 sm:py-8', className)}
    >
      {children}
    </section>
  );
}
