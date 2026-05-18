import { useMemo } from 'react';
import { type TFunction } from 'i18next';
import { useTranslation } from 'react-i18next';
import {
  type ProjectMetric,
  type ProjectMetricStatus,
  type ProjectMetricType,
  type ProjectType,
} from '@/modules/project/types/project';

export const PROJECT_METRIC_ORDER: ProjectMetricType[] = [
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

type UseProjectMetricsSectionParams = Readonly<{
  completionPercentage: number;
  isError: boolean;
  isLoading: boolean;
  metrics: ProjectMetric[] | undefined;
  projectType: ProjectType;
}>;

export type ProjectMetricCoverage = Readonly<{
  annotators: number;
  items: number;
}>;

export type ProjectMetricDisplayItem = Readonly<{
  calculable: boolean;
  labelKey: string;
  metricType: ProjectMetricType;
  nonCalculableMessage: string | null;
  provisionalMessage: string | null;
  valueText: string;
  valueToneClassName: string;
}>;

type UseProjectMetricsSectionResult = Readonly<{
  coverage: ProjectMetricCoverage | null;
  emptyMessage: string;
  metricItems: ProjectMetricDisplayItem[];
  metricsSupported: boolean;
  showLoading: boolean;
  skeletonMetricTypes: ProjectMetricType[];
}>;

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

function metricCoverage(metrics: ProjectMetric[]): ProjectMetricCoverage | null {
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

  const translatedReason = t(metricReasonKey(metric), {
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

export function useProjectMetricsSection({
  completionPercentage,
  isError,
  isLoading,
  metrics,
  projectType,
}: UseProjectMetricsSectionParams): UseProjectMetricsSectionResult {
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
          PROJECT_METRIC_ORDER.indexOf(left.metricType) -
          PROJECT_METRIC_ORDER.indexOf(right.metricType),
      ),
    [metrics],
  );
  const coverage = metricCoverage(sortedMetrics);
  const showLoading = isLoading && metricsSupported && completionPercentage > 0;
  const skeletonMetricTypes = PROJECT_METRIC_ORDER.slice(0, projectType === 'NER' ? 2 : 4);

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

  const metricItems = sortedMetrics.map<ProjectMetricDisplayItem>((metric) => {
    const value = metricValue(metric);
    const calculable = metric.calculable && value != null;

    return {
      calculable,
      labelKey: metricLabelKey(metric.metricType),
      metricType: metric.metricType,
      nonCalculableMessage: calculable ? null : nonCalculableMessage(metric, t),
      provisionalMessage: calculable ? provisionalMessage(metric, t) : null,
      valueText: calculable ? formatter.format(value) : t('project.detail.metrics.notAvailable'),
      valueToneClassName: valueTone(value, calculable),
    };
  });

  return {
    coverage,
    emptyMessage,
    metricItems,
    metricsSupported,
    showLoading,
    skeletonMetricTypes,
  };
}
