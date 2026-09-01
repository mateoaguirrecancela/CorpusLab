import { type ReactNode } from 'react';
import { Activity, AlertCircle, CircleDashed } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Spinner } from '@/components/ui/spinner';
import { cn } from '@/shared/utils/cn';
import {
  type ProjectMetric,
  type ProjectMetricType,
  type ProjectType,
} from '@/modules/project/shared/types/project';
import {
  type ProjectMetricCoverage,
  type ProjectMetricDisplayItem,
  useProjectMetricsSection,
} from '@/modules/project/metrics/hooks/useProjectMetricsSection';

type ProjectMetricsSectionProps = Readonly<{
  completionPercentage: number;
  isError: boolean;
  isLoading: boolean;
  metrics: ProjectMetric[] | undefined;
  projectType: ProjectType;
}>;

type ProjectMetricsHeaderProps = Readonly<{
  coverage: ProjectMetricCoverage | null;
  metricsSupported: boolean;
  showCoverage: boolean;
  showLoading: boolean;
}>;

function ProjectMetricsHeader({
  coverage,
  metricsSupported,
  showCoverage,
  showLoading,
}: ProjectMetricsHeaderProps) {
  const { t } = useTranslation();

  return (
    <div className="mb-4 flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
      <div className="min-w-0">
        <div className="flex min-w-0 items-center gap-2">
          <Activity className="size-4 shrink-0 text-primary" />
          <p className="truncate text-xs font-semibold tracking-wider text-muted-foreground uppercase">
            {t('project.detail.metrics.title')}
          </p>
        </div>
        {coverage && showCoverage && (
          <p className="mt-2 text-sm font-medium text-muted-foreground">
            {t('project.detail.metrics.sample', coverage)}
          </p>
        )}
      </div>
      {showLoading && metricsSupported && (
        <span className="inline-flex items-center gap-2 text-xs font-medium text-muted-foreground">
          <Spinner aria-hidden className="size-3.5" />
          {t('project.detail.metrics.loading')}
        </span>
      )}
    </div>
  );
}

type ProjectMetricsSkeletonProps = Readonly<{
  metricTypes: ProjectMetricType[];
}>;

function ProjectMetricsSkeleton({ metricTypes }: ProjectMetricsSkeletonProps) {
  return (
    <div className="grid gap-x-6 gap-y-4 sm:grid-cols-2 lg:grid-cols-4">
      {metricTypes.map((metricType) => (
        <div className="min-w-0 animate-pulse" key={metricType}>
          <div className="mb-2 h-3 w-24 rounded-full bg-muted" />
          <div className="mb-2 h-7 w-16 rounded-full bg-muted" />
          <div className="h-3 w-28 rounded-full bg-muted" />
        </div>
      ))}
    </div>
  );
}

type ProjectMetricsEmptyStateProps = Readonly<{
  isError: boolean;
  message: string;
}>;

function ProjectMetricsEmptyState({ isError, message }: ProjectMetricsEmptyStateProps) {
  return (
    <div className="flex items-center gap-2 text-sm text-muted-foreground">
      {isError ? (
        <AlertCircle className="size-4 shrink-0 text-amber-600" />
      ) : (
        <CircleDashed className="size-4 shrink-0 text-primary" />
      )}
      <span>{message}</span>
    </div>
  );
}

type ProjectMetricItemProps = Readonly<{
  item: ProjectMetricDisplayItem;
}>;

function ProjectMetricItem({ item }: ProjectMetricItemProps) {
  const { t } = useTranslation();

  return (
    <div className="min-w-0">
      <dt className="mb-1 truncate text-xs font-semibold tracking-wider text-muted-foreground uppercase">
        {t(item.labelKey)}
      </dt>
      <dd className={cn('text-2xl font-black leading-none tabular-nums', item.valueToneClassName)}>
        {item.valueText}
      </dd>
      {!item.calculable && item.nonCalculableMessage != null && (
        <p className="mt-2 truncate text-xs leading-snug text-muted-foreground">
          {item.nonCalculableMessage}
        </p>
      )}
      {item.provisionalMessage != null && (
        <p className="mt-2 truncate text-xs leading-snug text-muted-foreground">
          {item.provisionalMessage}
        </p>
      )}
    </div>
  );
}

type ProjectMetricsListProps = Readonly<{
  items: ProjectMetricDisplayItem[];
}>;

function ProjectMetricsList({ items }: ProjectMetricsListProps) {
  return (
    <dl className="grid gap-x-6 gap-y-4 sm:grid-cols-2 lg:grid-cols-4">
      {items.map((item) => (
        <ProjectMetricItem item={item} key={item.metricType} />
      ))}
    </dl>
  );
}

export function ProjectMetricsSection({
  completionPercentage,
  isError,
  isLoading,
  metrics,
  projectType,
}: ProjectMetricsSectionProps) {
  const {
    coverage,
    emptyMessage,
    metricItems,
    metricsSupported,
    showLoading,
    skeletonMetricTypes,
  } = useProjectMetricsSection({
    completionPercentage,
    isError,
    isLoading,
    metrics,
    projectType,
  });

  if (!metricsSupported) {
    return null;
  }

  let content: ReactNode;
  if (showLoading) {
    content = <ProjectMetricsSkeleton metricTypes={skeletonMetricTypes} />;
  } else if (emptyMessage.length > 0) {
    content = <ProjectMetricsEmptyState isError={isError} message={emptyMessage} />;
  } else {
    content = <ProjectMetricsList items={metricItems} />;
  }

  return (
    <div className="pt-6">
      <ProjectMetricsHeader
        coverage={coverage}
        metricsSupported={metricsSupported}
        showCoverage={emptyMessage.length === 0}
        showLoading={showLoading}
      />

      {content}
    </div>
  );
}
