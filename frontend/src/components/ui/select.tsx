import * as React from 'react';
import { ChevronDown } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

type SelectProps = React.ComponentProps<'select'>;

function Select({ className, children, ...props }: SelectProps) {
  return (
    <div className="relative">
      <select
        className={cn(
          'h-10 w-full appearance-none rounded-md border border-input bg-surface-base px-3 pr-10 text-sm transition-colors outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:pointer-events-none disabled:cursor-not-allowed disabled:opacity-50',
          className,
        )}
        {...props}
      >
        {children}
      </select>
      <ChevronDown className="pointer-events-none absolute top-1/2 right-3 size-4 -translate-y-1/2 text-muted-foreground" />
    </div>
  );
}

export { Select };
