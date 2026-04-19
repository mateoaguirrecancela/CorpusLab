import { cn } from '@/lib/utils';

export type RoleBadgeRole = 'ANNOTATOR' | 'PARTICIPANT' | 'ADMIN' | 'CREATOR' | 'OWNER';

function roleBadgeColorClassName(role: RoleBadgeRole): string {
  if (role === 'ADMIN') {
    return 'bg-accent text-primary';
  }

  if (role === 'CREATOR' || role === 'OWNER') {
    return 'bg-primary text-white';
  }

  return 'bg-muted text-muted-foreground';
}

type RoleBadgeProps = Readonly<{
  label: string;
  role: RoleBadgeRole;
  className?: string;
}>;

export function RoleBadge({ label, role, className }: RoleBadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-md px-2.5 py-1 text-xs font-semibold',
        roleBadgeColorClassName(role),
        className,
      )}
    >
      {label}
    </span>
  );
}
