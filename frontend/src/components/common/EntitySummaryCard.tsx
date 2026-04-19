import { ArrowRight, FolderKanban, Users } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { type RoleBadgeRole, RoleBadge } from '@/components/common/RoleBadge';
import { cn } from '@/lib/utils';

type EntityFooterMeta = Readonly<{
  kind: 'research-group' | 'members';
  text: string;
}>;

type EntitySummaryCardProps = Readonly<{
  actionLabel: string;
  description?: string | null;
  footerMeta?: EntityFooterMeta;
  onAction: () => void;
  completionPercentage?: number;
  roleLabel?: string;
  role?: RoleBadgeRole;
  title: string;
}>;

function normalizeCompletionPercentage(value: number): number {
  if (!Number.isFinite(value)) {
    return 0;
  }

  return Math.min(100, Math.max(0, Math.round(value)));
}

function completionBadgeClassName(completionPercentage: number): string {
  if (completionPercentage >= 100) {
    return 'bg-emerald-100 text-emerald-800';
  }

  if (completionPercentage >= 50) {
    return 'bg-amber-100 text-amber-800';
  }

  return 'bg-sky-100 text-sky-800';
}

export function EntitySummaryCard({
  actionLabel,
  completionPercentage,
  description,
  footerMeta,
  onAction,
  roleLabel,
  role,
  title,
}: EntitySummaryCardProps) {
  const { t } = useTranslation();
  const hasDescription = typeof description === 'string' && description.trim().length > 0;

  const normalizedCompletion =
    typeof completionPercentage === 'number'
      ? normalizeCompletionPercentage(completionPercentage)
      : undefined;

  const computedTopLeft = roleLabel ? (
    <RoleBadge
      className="text-[11px] font-bold tracking-wider uppercase"
      label={roleLabel}
      role={role ?? 'PARTICIPANT'}
    />
  ) : null;

  const computedTopRight =
    typeof normalizedCompletion === 'number' ? (
      <span
        className={cn(
          'inline-flex items-center rounded-md px-2.5 py-1 text-[11px] font-bold tracking-wider uppercase',
          completionBadgeClassName(normalizedCompletion),
        )}
      >
        {t('project.list.completionBadge', { value: normalizedCompletion })}
      </span>
    ) : null;

  const computedFooterInfo = footerMeta ? (
    <p className="inline-flex items-center gap-1.5 text-xs font-medium text-muted-foreground">
      {footerMeta.kind === 'research-group' ? (
        <FolderKanban className="size-3.5" />
      ) : (
        <Users className="size-3.5" />
      )}
      {footerMeta.text}
    </p>
  ) : null;

  const hasTopRow = Boolean(computedTopLeft) || Boolean(computedTopRight);

  return (
    <article className="stagger flex h-full flex-col justify-between rounded-2xl border border-border bg-surface-base p-5 transition-all duration-200 hover:-translate-y-0.5 hover:border-primary/30 hover:shadow-(--shadow-card-hover)">
      <div>
        {hasTopRow && (
          <div className="flex flex-wrap items-center justify-between gap-2">
            {computedTopLeft ? (
              <div className="inline-flex min-w-0 items-center">{computedTopLeft}</div>
            ) : (
              <div />
            )}
            {computedTopRight && <div className="inline-flex items-center">{computedTopRight}</div>}
          </div>
        )}

        <h3 className={cn('text-lg font-black leading-tight text-primary', hasTopRow && 'mt-4')}>
          {title}
        </h3>

        {hasDescription ? (
          <p className="mt-2 line-clamp-2 text-sm leading-relaxed text-muted-foreground">
            {description}
          </p>
        ) : (
          <p className="mt-2 text-sm italic text-muted-foreground">
            {t('project.list.noDescription')}
          </p>
        )}
      </div>

      <div className="mt-5 flex items-center justify-between gap-3 border-t border-border pt-4">
        {computedFooterInfo ? (
          <div className="min-w-0 flex-1">{computedFooterInfo}</div>
        ) : (
          <div className="flex-1" />
        )}

        <button
          className="group ml-auto inline-flex cursor-pointer items-center gap-1 text-sm font-semibold text-primary transition-colors hover:text-primary-strong"
          onClick={onAction}
          type="button"
        >
          {actionLabel}
          <ArrowRight className="size-3.5" />
        </button>
      </div>
    </article>
  );
}
