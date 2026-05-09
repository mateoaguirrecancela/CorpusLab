import { Activity, AlertCircle, CircleDashed } from 'lucide-react';
import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { type TFunction } from 'i18next';
import { Spinner } from '@/components/ui/spinner';
import { cn } from '@/lib/utils';
import {
  type ProjectMetric,
  type ProjectMetricStatus,
  type ProjectMetricType,
  type ProjectType,
} from '@/modules/project/types/project';

type ProjectMetricsSectionProps = Readonly<{
  completionPercentage: number;
  isError: boolean;
  isLoading: boolean;
  metrics: ProjectMetric[] | undefined;
  projectType: ProjectType;
}>;

const METRIC_ORDER: ProjectMetricType[] = [
  'COHENS_KAPPA',
  'KRIPPENDORFFS_ALPHA',
  'FLEISS_KAPPA',
  'XRR',
  'SPAN_OVERLAP_F1',
];

const STATUS_TRANSLATION_KEYS: Record<ProjectMetricStatus, string> = {
  CALCULABLE: 'project.detail.metrics.status.calculable',
  NO_ANNOTATIONS: 'project.detail.metrics.status.noAnnotations',
  INSUFFICIENT_ANNOTATORS: 'project.detail.metrics.status.insufficientAnnotators',
  INSUFFICIENT_ITEMS: 'project.detail.metrics.status.insufficientItems',
  NO_SHARED_ITEMS: 'project.detail.metrics.status.noSharedItems',
  NO_VALID_PAIRS: 'project.detail.metrics.status.noValidPairs',
  UNDEFINED: 'project.detail.metrics.status.undefined',
};

function metricLabelKey(metricType: ProjectMetricType): string {
  return `project.detail.metrics.labels.${metricType}`;
}

function metricReasonKey(metric: ProjectMetric): string {
  return `project.detail.metrics.reasons.${metric.metricType}.${metric.status}`;
}

function metricValue(metric: ProjectMetric): number | null {
  const rawValue = metric.value as unknown;
  if (rawValue == null || rawValue === 'NaN') {
    return null;
  }

  const numericValue = typeof rawValue === 'number' ? rawValue : Number(rawValue);
  return Number.isFinite(numericValue) ? numericValue : null;
}

function valueTone(value: number | null, calculable: boolean): string {
  if (!calculable || value == null) {
    return 'text-muted-foreground';
  }

  if (value >= 0.67) {
    return 'text-emerald-700';
  }

  if (value >= 0.33) {
    return 'text-sky-700';
  }

  if (value >= 0) {
    return 'text-amber-700';
  }

  return 'text-rose-700';
}

function metricCoverage(metrics: ProjectMetric[]): { annotators: number; items: number } | null {
  if (metrics.length === 0) {
    return null;
  }

  const annotators = Math.max(...metrics.map((metric) => metric.annotatorCount));
  const items = Math.max(...metrics.map((metric) => metric.itemCount));
  return annotators > 0 || items > 0 ? { annotators, items } : null;
}

function detailNumber(metric: ProjectMetric, key: string): number | null {
  const value = metric.details?.[key];
  const numericValue = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(numericValue) && numericValue > 0 ? numericValue : null;
}

function detailFiniteNumber(metric: ProjectMetric, key: string): number | null {
  const value = metric.details?.[key];
  const numericValue = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(numericValue) ? numericValue : null;
}

function detailBoolean(metric: ProjectMetric, key: string): boolean {
  return metric.details?.[key] === true;
}

function countKey(key: string, count: number): string {
  return `project.detail.metrics.counts.${key}.${count === 1 ? 'one' : 'other'}`;
}

function countedNonCalculableMessage(metric: ProjectMetric, t: TFunction): string | null {
  if (metric.metricType === 'XRR' && metric.status === 'UNDEFINED') {
    const groupAIrr = detailFiniteNumber(metric, 'groupXIrr');
    const groupBIrr = detailFiniteNumber(metric, 'groupYIrr');
    if (groupAIrr != null && groupAIrr <= 0) {
      return t('project.detail.metrics.reasons.XRR.IRR_NOT_POSITIVE', { group: 'A' });
    }
    if (groupBIrr != null && groupBIrr <= 0) {
      return t('project.detail.metrics.reasons.XRR.IRR_NOT_POSITIVE', { group: 'B' });
    }
  }

  if (metric.metricType === 'FLEISS_KAPPA' && metric.status === 'INSUFFICIENT_ITEMS') {
    const missingRatings = detailNumber(metric, 'missingRatings');
    if (missingRatings != null) {
      return t(countKey('missingAnnotation', missingRatings), { count: missingRatings });
    }
  }

  if (metric.metricType === 'XRR' && metric.status === 'INSUFFICIENT_ANNOTATORS') {
    const missingGroups = detailNumber(metric, 'missingRaterGroups');
    if (missingGroups != null) {
      return t(countKey('missingGroup', missingGroups), { count: missingGroups });
    }
  }

  if (metric.status === 'INSUFFICIENT_ANNOTATORS') {
    const missingAnnotators =
      detailNumber(metric, 'missingAnnotators') ?? Math.max(0, 2 - metric.annotatorCount);
    if (missingAnnotators > 0) {
      return t(countKey('missingAnnotator', missingAnnotators), { count: missingAnnotators });
    }
  }

  if (metric.status === 'INSUFFICIENT_ITEMS') {
    const missingItems =
      detailNumber(metric, 'missingCompleteUnits') ?? detailNumber(metric, 'skippedUnits');
    if (missingItems != null) {
      return t(countKey('missingItem', missingItems), { count: missingItems });
    }
  }

  return null;
}

function provisionalMessage(metric: ProjectMetric, t: TFunction): string | null {
  if (!detailBoolean(metric, 'provisional')) {
    return null;
  }

  const inactiveAnnotators = Number(metric.details?.inactiveAnnotators ?? 0);
  if (!Number.isFinite(inactiveAnnotators) || inactiveAnnotators <= 0) {
    return t('project.detail.metrics.provisional');
  }

  const activeAnnotators = detailNumber(metric, 'activeAnnotators');
  if (activeAnnotators == null) {
    return t('project.detail.metrics.provisional');
  }

  return t(countKey('activeAnnotator', activeAnnotators), { count: activeAnnotators });
}

function nonCalculableMessage(metric: ProjectMetric, t: TFunction): string {
  const countedMessage = countedNonCalculableMessage(metric, t);
  if (countedMessage != null) {
    return countedMessage;
  }

  const reasonKey = metricReasonKey(metric);
  const translatedReason = t(reasonKey, {
    annotators: metric.annotatorCount,
    defaultValue: '',
    items: metric.itemCount,
  });

  if (translatedReason.length > 0) {
    return translatedReason;
  }

  const genericReason = t(`project.detail.metrics.reasons.generic.${metric.status}`, {
    annotators: metric.annotatorCount,
    defaultValue: '',
    items: metric.itemCount,
  });
  if (genericReason.length > 0) {
    return genericReason;
  }

  const statusKey = STATUS_TRANSLATION_KEYS[metric.status] ?? STATUS_TRANSLATION_KEYS.UNDEFINED;
  return t(statusKey);
}

export function ProjectMetricsSection({
  completionPercentage,
  isError,
  isLoading,
  metrics,
  projectType,
}: ProjectMetricsSectionProps) {
  const { i18n, t } = useTranslation();
  const formatter = useMemo(
    () =>
      new Intl.NumberFormat(i18n.resolvedLanguage ?? i18n.language, {
        maximumFractionDigits: 3,
        minimumFractionDigits: 3,
      }),
    [i18n.language, i18n.resolvedLanguage],
  );

  const metricsSupported = projectType !== 'SEQ2SEQ';
  const sortedMetrics = useMemo(
    () =>
      [...(metrics ?? [])].sort(
        (left, right) =>
          METRIC_ORDER.indexOf(left.metricType) - METRIC_ORDER.indexOf(right.metricType),
      ),
    [metrics],
  );
  const coverage = metricCoverage(sortedMetrics);

  let emptyMessage = '';
  if (!metricsSupported) {
    emptyMessage = t('project.detail.metrics.unsupported');
  } else if (completionPercentage <= 0) {
    emptyMessage = t('project.detail.metrics.emptyNoProgress');
  } else if (isError) {
    emptyMessage = t('project.detail.metrics.unavailable');
  } else if (!isLoading && sortedMetrics.length === 0) {
    emptyMessage = t('project.detail.metrics.empty');
  }

  return (
    <div className="pt-6">
      <div className="mb-4 flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
        <div className="min-w-0">
          <div className="flex min-w-0 items-center gap-2">
            <Activity className="size-4 shrink-0 text-primary" />
            <p className="truncate text-xs font-semibold tracking-wider text-muted-foreground uppercase">
              {t('project.detail.metrics.title')}
            </p>
          </div>
          {coverage && !emptyMessage && (
            <p className="mt-2 text-sm font-medium text-muted-foreground">
              {t('project.detail.metrics.sample', coverage)}
            </p>
          )}
        </div>
        {isLoading && metricsSupported && completionPercentage > 0 && (
          <span className="inline-flex items-center gap-2 text-xs font-medium text-muted-foreground">
            <Spinner aria-hidden className="size-3.5" />
            {t('project.detail.metrics.loading')}
          </span>
        )}
      </div>

      {isLoading && metricsSupported && completionPercentage > 0 ? (
        <div className="grid gap-x-6 gap-y-4 sm:grid-cols-2 lg:grid-cols-4">
          {METRIC_ORDER.slice(0, projectType === 'NER' ? 2 : 4).map((metricType) => (
            <div className="min-w-0 animate-pulse" key={metricType}>
              <div className="mb-2 h-3 w-24 rounded-full bg-muted" />
              <div className="mb-2 h-7 w-16 rounded-full bg-muted" />
              <div className="h-3 w-28 rounded-full bg-muted" />
            </div>
          ))}
        </div>
      ) : emptyMessage.length > 0 ? (
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          {isError ? (
            <AlertCircle className="size-4 shrink-0 text-amber-600" />
          ) : (
            <CircleDashed className="size-4 shrink-0 text-primary" />
          )}
          <span>{emptyMessage}</span>
        </div>
      ) : (
        <dl className="grid gap-x-6 gap-y-4 sm:grid-cols-2 lg:grid-cols-4">
          {sortedMetrics.map((metric) => {
            const value = metricValue(metric);
            const calculable = metric.calculable && value != null;
            const provisional = calculable ? provisionalMessage(metric, t) : null;
            return (
              <div className="min-w-0" key={metric.metricType}>
                <dt className="mb-1 truncate text-xs font-semibold tracking-wider text-muted-foreground uppercase">
                  {t(metricLabelKey(metric.metricType))}
                </dt>
                <dd
                  className={cn(
                    'text-2xl font-black leading-none tabular-nums',
                    valueTone(value, calculable),
                  )}
                >
                  {calculable ? formatter.format(value) : t('project.detail.metrics.notAvailable')}
                </dd>
                {!calculable && (
                  <p className="mt-2 truncate text-xs leading-snug text-muted-foreground">
                    {nonCalculableMessage(metric, t)}
                  </p>
                )}
                {provisional != null && (
                  <p className="mt-2 truncate text-xs leading-snug text-muted-foreground">
                    {provisional}
                  </p>
                )}
              </div>
            );
          })}
        </dl>
      )}
    </div>
  );
}
