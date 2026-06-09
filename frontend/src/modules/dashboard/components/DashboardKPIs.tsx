import {
  AlertTriangle,
  ArrowDownRight,
  ArrowRight,
  ArrowUpRight,
  CalendarCheck2,
  CircleDashed,
  ClipboardList,
  FolderKanban,
  type LucideIcon,
} from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Spinner } from '@/components/ui/spinner';
import { cn } from '@/shared/utils/cn';
import {
  type DashboardMetric,
  type DashboardMetricKind,
  type DashboardMetricTrend,
} from '@/modules/dashboard/types/dashboard';

type DashboardKPIsProps = Readonly<{
  errorMessage: string;
  isLoading: boolean;
  metrics: DashboardMetric[];
}>;

type DashboardKpiMetadata = Readonly<{
  Icon: LucideIcon;
  iconClassName: string;
  titleKey: string;
}>;

const KPI_METADATA: Record<DashboardMetricKind, DashboardKpiMetadata> = {
  'active-projects': {
    Icon: FolderKanban,
    iconClassName: 'bg-primary/10 text-primary',
    titleKey: 'home.dashboard.kpis.activeProjects.title',
  },
  'annotations-today': {
    Icon: CalendarCheck2,
    iconClassName: 'bg-success-soft text-success',
    titleKey: 'home.dashboard.kpis.annotationsToday.title',
  },
  'pending-annotations': {
    Icon: ClipboardList,
    iconClassName: 'bg-amber-100 text-amber-700',
    titleKey: 'home.dashboard.kpis.pendingAnnotations.title',
  },
  'open-alerts': {
    Icon: AlertTriangle,
    iconClassName: 'bg-danger-soft text-destructive',
    titleKey: 'home.dashboard.kpis.openAlerts.title',
  },
};

function formatMetricValue(value: number, locale: string): string {
  return new Intl.NumberFormat(locale, { maximumFractionDigits: 0 }).format(value);
}

function TrendDirectionIcon({
  className,
  trend,
}: Readonly<{ className: string; trend: DashboardMetricTrend }>) {
  if (trend.direction === 'up') {
    return <ArrowUpRight className={className} />;
  }

  if (trend.direction === 'down') {
    return <ArrowDownRight className={className} />;
  }

  return <ArrowRight className={className} />;
}

function getTrendClassName(metricKind: DashboardMetricKind, trend: DashboardMetricTrend): string {
  if (trend.direction === 'neutral') {
    return 'text-muted-foreground';
  }

  if (metricKind === 'open-alerts') {
    return 'text-destructive';
  }

  return 'text-success';
}

function DashboardKpiTrend({
  metricKind,
  trend,
}: Readonly<{ metricKind: DashboardMetricKind; trend: DashboardMetricTrend }>) {
  const { i18n } = useTranslation();

  return (
    <p
      className={cn(
        'mt-3 inline-flex min-w-0 items-center gap-1 text-xs font-semibold',
        getTrendClassName(metricKind, trend),
      )}
    >
      <TrendDirectionIcon className="size-3.5 shrink-0" trend={trend} />
      <span className="truncate">
        {formatMetricValue(trend.value, i18n.resolvedLanguage ?? i18n.language)} {trend.label}
      </span>
    </p>
  );
}

function DashboardKpiCard({ metric }: Readonly<{ metric: DashboardMetric }>) {
  const { i18n, t } = useTranslation();
  const metadata = KPI_METADATA[metric.kind];
  const Icon = metadata.Icon;

  return (
    <article className="min-w-0 rounded-xl border border-border bg-surface-base p-4">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="truncate text-xs font-bold tracking-[0.12em] text-muted-foreground uppercase">
            {t(metadata.titleKey)}
          </p>
          <p className="mt-4 text-3xl font-black leading-none text-primary tabular-nums">
            {formatMetricValue(metric.value, i18n.resolvedLanguage ?? i18n.language)}
          </p>
        </div>

        <span
          className={cn(
            'inline-flex size-10 shrink-0 items-center justify-center rounded-lg',
            metadata.iconClassName,
          )}
        >
          <Icon className="size-5" />
        </span>
      </div>

      <p className="mt-3 truncate text-sm text-muted-foreground">{metric.supportingText}</p>
      {metric.trend ? <DashboardKpiTrend metricKind={metric.kind} trend={metric.trend} /> : null}
    </article>
  );
}

export function DashboardKPIs({ errorMessage, isLoading, metrics }: DashboardKPIsProps) {
  const { t } = useTranslation();

  if (isLoading) {
    return (
      <div className="rounded-xl border border-border bg-surface-base px-4 py-5 text-sm text-muted-foreground">
        <span className="inline-flex items-center gap-2">
          <Spinner aria-hidden className="size-4" />
          {t('home.dashboard.loading.metrics')}
        </span>
      </div>
    );
  }

  if (errorMessage.length > 0) {
    return (
      <div className="rounded-xl border border-danger-border bg-danger-soft px-4 py-5 text-sm font-medium text-destructive">
        {errorMessage}
      </div>
    );
  }

  if (metrics.length === 0) {
    return (
      <div className="rounded-xl border border-dashed border-border bg-surface-base px-4 py-5 text-sm text-muted-foreground">
        <span className="inline-flex items-center gap-2">
          <CircleDashed className="size-4" />
          {t('home.dashboard.empty.metrics')}
        </span>
      </div>
    );
  }

  return (
    <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      {metrics.map((metric) => (
        <DashboardKpiCard key={metric.kind} metric={metric} />
      ))}
    </section>
  );
}
