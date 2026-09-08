import { renderHook } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  PROJECT_METRIC_ORDER,
  useProjectMetricsSection,
} from '@/modules/project/metrics/hooks/useProjectMetricsSection';
import { type ProjectMetric } from '@/modules/project/shared/types/project';

// Controls whether a translation key that carries a `defaultValue` (i.e. one
// that only "exists" if the resource defines it) resolves or falls back, so
// tests can exercise nonCalculableMessage's per-status -> generic -> status
// fallback cascade without a real i18n resource bundle. Declared via
// vi.hoisted so the vi.mock factory below (which is itself hoisted above
// this module's own top-level code) can safely close over it.
const { resolvableKeys } = vi.hoisted(() => ({
  resolvableKeys: {
    has: (key: string): boolean => {
      void key;
      return false;
    },
  },
}));

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    i18n: { language: 'en-US', resolvedLanguage: 'en-US' },
    t: (key: string, opts?: Record<string, unknown>) => {
      if (opts && 'defaultValue' in opts) {
        return resolvableKeys.has(key) ? `${key}:${JSON.stringify(opts)}` : '';
      }
      return opts ? `${key}:${JSON.stringify(opts)}` : key;
    },
  }),
}));

beforeEach(() => {
  resolvableKeys.has = () => false;
});

function metric(overrides: Partial<ProjectMetric> = {}): ProjectMetric {
  return {
    metricType: 'COHENS_KAPPA',
    projectType: 'TEXT_CLASSIFICATION_SIMPLE',
    value: null,
    calculable: false,
    status: 'UNDEFINED',
    message: null,
    annotatorCount: 0,
    itemCount: 0,
    pairCount: 0,
    details: {},
    ...overrides,
  };
}

function baseParams(overrides: Partial<Parameters<typeof useProjectMetricsSection>[0]> = {}) {
  return {
    completionPercentage: 50,
    isError: false,
    isLoading: false,
    metrics: [] as ProjectMetric[],
    projectType: 'TEXT_CLASSIFICATION_SIMPLE' as const,
    ...overrides,
  };
}

describe('metricsSupported / emptyMessage', () => {
  it('is unsupported for SEQ2SEQ projects', () => {
    const { result } = renderHook(() =>
      useProjectMetricsSection(baseParams({ projectType: 'SEQ2SEQ' })),
    );

    expect(result.current.metricsSupported).toBe(false);
    expect(result.current.emptyMessage).toBe('project.detail.metrics.unsupported');
  });

  it('reports no progress when completion is 0', () => {
    const { result } = renderHook(() =>
      useProjectMetricsSection(baseParams({ completionPercentage: 0 })),
    );

    expect(result.current.emptyMessage).toBe('project.detail.metrics.emptyNoProgress');
  });

  it('reports unavailable on a query error once there is progress', () => {
    const { result } = renderHook(() => useProjectMetricsSection(baseParams({ isError: true })));

    expect(result.current.emptyMessage).toBe('project.detail.metrics.unavailable');
  });

  it('reports empty once loaded with progress but no metrics', () => {
    const { result } = renderHook(() => useProjectMetricsSection(baseParams()));

    expect(result.current.emptyMessage).toBe('project.detail.metrics.empty');
  });

  it('has no empty message while metrics are loading', () => {
    const { result } = renderHook(() => useProjectMetricsSection(baseParams({ isLoading: true })));

    expect(result.current.emptyMessage).toBe('');
  });
});

describe('showLoading', () => {
  it('is true only when loading, supported, and there is progress', () => {
    const loading = renderHook(() =>
      useProjectMetricsSection(baseParams({ isLoading: true, completionPercentage: 10 })),
    );
    expect(loading.result.current.showLoading).toBe(true);

    const noProgress = renderHook(() =>
      useProjectMetricsSection(baseParams({ isLoading: true, completionPercentage: 0 })),
    );
    expect(noProgress.result.current.showLoading).toBe(false);
  });
});

describe('skeletonMetricTypes', () => {
  it('shows 5 placeholders for NER and 4 for other project types', () => {
    const ner = renderHook(() => useProjectMetricsSection(baseParams({ projectType: 'NER' })));
    expect(ner.result.current.skeletonMetricTypes).toHaveLength(5);

    const classification = renderHook(() => useProjectMetricsSection(baseParams()));
    expect(classification.result.current.skeletonMetricTypes).toHaveLength(4);
  });
});

describe('metricsGridClassName', () => {
  it('uses one column per metric so the five NER metrics fit in a single row', () => {
    const metrics = PROJECT_METRIC_ORDER.map((metricType) => metric({ metricType }));
    const { result } = renderHook(() =>
      useProjectMetricsSection(baseParams({ metrics, projectType: 'NER' })),
    );

    expect(result.current.metricItems).toHaveLength(5);
    expect(result.current.metricsGridClassName).toContain('xl:grid-cols-5');
  });

  it('keeps four columns for classification projects', () => {
    const metrics = PROJECT_METRIC_ORDER.slice(0, 4).map((metricType) => metric({ metricType }));
    const { result } = renderHook(() => useProjectMetricsSection(baseParams({ metrics })));

    expect(result.current.metricsGridClassName).toContain('lg:grid-cols-4');
  });

  it('sizes the grid from the skeleton while metrics are loading', () => {
    const { result } = renderHook(() =>
      useProjectMetricsSection(
        baseParams({ isLoading: true, metrics: undefined, projectType: 'NER' }),
      ),
    );

    expect(result.current.showLoading).toBe(true);
    expect(result.current.metricsGridClassName).toContain('xl:grid-cols-5');
  });
});

describe('metricItems ordering and formatting', () => {
  it('sorts metrics by the canonical metric order regardless of input order', () => {
    const metrics = [metric({ metricType: 'XRR' }), metric({ metricType: 'COHENS_KAPPA' })];
    const { result } = renderHook(() => useProjectMetricsSection(baseParams({ metrics })));

    expect(result.current.metricItems.map((item) => item.metricType)).toEqual([
      'COHENS_KAPPA',
      'XRR',
    ]);
  });

  it('formats a calculable value and tones it by magnitude', () => {
    const metrics = [
      metric({
        metricType: 'COHENS_KAPPA',
        value: 0.8,
        calculable: true,
        annotatorCount: 2,
        itemCount: 10,
      }),
      metric({ metricType: 'FLEISS_KAPPA', value: 0.5, calculable: true }),
      metric({ metricType: 'XRR', value: 0.1, calculable: true }),
      metric({ metricType: 'SPAN_OVERLAP_F1', value: -0.2, calculable: true }),
    ];
    const { result } = renderHook(() => useProjectMetricsSection(baseParams({ metrics })));

    const [kappa, fleiss, xrr, span] = result.current.metricItems;
    expect(kappa.valueText).toBe('0.800');
    expect(kappa.valueToneClassName).toBe('text-emerald-700');
    expect(fleiss.valueToneClassName).toBe('text-sky-700');
    expect(xrr.valueToneClassName).toBe('text-amber-700');
    expect(span.valueToneClassName).toBe('text-rose-700');
  });

  it('shows not-available and a reason for a non-calculable metric', () => {
    const metrics = [
      metric({
        metricType: 'COHENS_KAPPA',
        calculable: false,
        status: 'INSUFFICIENT_ANNOTATORS',
        annotatorCount: 1,
        details: { missingAnnotators: 1 },
      }),
    ];
    const { result } = renderHook(() => useProjectMetricsSection(baseParams({ metrics })));

    expect(result.current.metricItems[0].valueText).toBe('project.detail.metrics.notAvailable');
    expect(result.current.metricItems[0].nonCalculableMessage).toBe(
      'project.detail.metrics.counts.missingAnnotator.one:{"count":1}',
    );
    expect(result.current.metricItems[0].valueToneClassName).toBe('text-muted-foreground');
  });

  it('surfaces a generic provisional message when nothing is inactive yet', () => {
    const metrics = [
      metric({
        metricType: 'COHENS_KAPPA',
        value: 0.5,
        calculable: true,
        details: { provisional: true },
      }),
    ];
    const { result } = renderHook(() => useProjectMetricsSection(baseParams({ metrics })));

    expect(result.current.metricItems[0].provisionalMessage).toBe(
      'project.detail.metrics.provisional',
    );
  });

  it('surfaces the active-annotator count once some annotators are inactive', () => {
    const metrics = [
      metric({
        metricType: 'COHENS_KAPPA',
        value: 0.5,
        calculable: true,
        details: { provisional: true, inactiveAnnotators: 1, activeAnnotators: 2 },
      }),
    ];
    const { result } = renderHook(() => useProjectMetricsSection(baseParams({ metrics })));

    expect(result.current.metricItems[0].provisionalMessage).toBe(
      'project.detail.metrics.counts.activeAnnotator.other:{"count":2}',
    );
  });
});

describe('coverage', () => {
  it('is null when there are no metrics', () => {
    const { result } = renderHook(() => useProjectMetricsSection(baseParams()));
    expect(result.current.coverage).toBeNull();
  });

  it('reports the maximum annotator and item counts across metrics', () => {
    const metrics = [
      metric({ metricType: 'COHENS_KAPPA', annotatorCount: 2, itemCount: 5 }),
      metric({ metricType: 'XRR', annotatorCount: 4, itemCount: 3 }),
    ];
    const { result } = renderHook(() => useProjectMetricsSection(baseParams({ metrics })));

    expect(result.current.coverage).toEqual({ annotators: 4, items: 5 });
  });

  it('is null when every metric reports zero annotators and items', () => {
    const metrics = [metric({ annotatorCount: 0, itemCount: 0 })];
    const { result } = renderHook(() => useProjectMetricsSection(baseParams({ metrics })));

    expect(result.current.coverage).toBeNull();
  });
});

describe('metricValue edge cases', () => {
  it('parses a numeric string value', () => {
    const metrics = [metric({ value: '0.5' as unknown as number, calculable: true })];
    const { result } = renderHook(() => useProjectMetricsSection(baseParams({ metrics })));

    expect(result.current.metricItems[0].valueText).toBe('0.500');
  });

  it('treats the literal string "NaN" as no value', () => {
    const metrics = [metric({ value: 'NaN' as unknown as number, calculable: true })];
    const { result } = renderHook(() => useProjectMetricsSection(baseParams({ metrics })));

    expect(result.current.metricItems[0].calculable).toBe(false);
  });
});

describe('countedNonCalculableMessage per metric type', () => {
  it('flags XRR as undefined when group A has non-positive IRR', () => {
    const metrics = [
      metric({
        metricType: 'XRR',
        status: 'UNDEFINED',
        details: { groupXIrr: 0 },
      }),
    ];
    const { result } = renderHook(() => useProjectMetricsSection(baseParams({ metrics })));

    expect(result.current.metricItems[0].nonCalculableMessage).toBe(
      'project.detail.metrics.reasons.XRR.IRR_NOT_POSITIVE:{"group":"A"}',
    );
  });

  it('flags XRR as undefined when only group B has non-positive IRR', () => {
    const metrics = [
      metric({
        metricType: 'XRR',
        status: 'UNDEFINED',
        details: { groupXIrr: 0.5, groupYIrr: -0.1 },
      }),
    ];
    const { result } = renderHook(() => useProjectMetricsSection(baseParams({ metrics })));

    expect(result.current.metricItems[0].nonCalculableMessage).toBe(
      'project.detail.metrics.reasons.XRR.IRR_NOT_POSITIVE:{"group":"B"}',
    );
  });

  it('falls through to the generic reason cascade when both XRR groups have positive IRR', () => {
    resolvableKeys.has = (key) => key === 'project.detail.metrics.reasons.generic.UNDEFINED';
    const metrics = [
      metric({
        metricType: 'XRR',
        status: 'UNDEFINED',
        details: { groupXIrr: 0.5, groupYIrr: 0.5 },
      }),
    ];
    const { result } = renderHook(() => useProjectMetricsSection(baseParams({ metrics })));

    expect(result.current.metricItems[0].nonCalculableMessage).toBe(
      'project.detail.metrics.reasons.generic.UNDEFINED:{"annotators":0,"defaultValue":"","items":0}',
    );
  });

  it('reports the missing-ratings count for a Fleiss Kappa with insufficient items', () => {
    const metrics = [
      metric({
        metricType: 'FLEISS_KAPPA',
        status: 'INSUFFICIENT_ITEMS',
        details: { missingRatings: 3 },
      }),
    ];
    const { result } = renderHook(() => useProjectMetricsSection(baseParams({ metrics })));

    expect(result.current.metricItems[0].nonCalculableMessage).toBe(
      'project.detail.metrics.counts.missingAnnotation.other:{"count":3}',
    );
  });

  it('falls back to the generic insufficient-items message when Fleiss has no missingRatings detail', () => {
    const metrics = [
      metric({
        metricType: 'FLEISS_KAPPA',
        status: 'INSUFFICIENT_ITEMS',
        details: { skippedUnits: 2 },
      }),
    ];
    const { result } = renderHook(() => useProjectMetricsSection(baseParams({ metrics })));

    expect(result.current.metricItems[0].nonCalculableMessage).toBe(
      'project.detail.metrics.counts.missingItem.other:{"count":2}',
    );
  });

  it('reports the missing-groups count for an XRR with insufficient annotators', () => {
    const metrics = [
      metric({
        metricType: 'XRR',
        status: 'INSUFFICIENT_ANNOTATORS',
        details: { missingRaterGroups: 1 },
      }),
    ];
    const { result } = renderHook(() => useProjectMetricsSection(baseParams({ metrics })));

    expect(result.current.metricItems[0].nonCalculableMessage).toBe(
      'project.detail.metrics.counts.missingGroup.one:{"count":1}',
    );
  });

  it('falls back to the generic missing-annotators message when XRR has no missingRaterGroups detail', () => {
    const metrics = [
      metric({
        metricType: 'XRR',
        status: 'INSUFFICIENT_ANNOTATORS',
        annotatorCount: 1,
        details: {},
      }),
    ];
    const { result } = renderHook(() => useProjectMetricsSection(baseParams({ metrics })));

    expect(result.current.metricItems[0].nonCalculableMessage).toBe(
      'project.detail.metrics.counts.missingAnnotator.one:{"count":1}',
    );
  });

  it('falls through to the reason cascade when the annotator shortfall cannot be determined', () => {
    resolvableKeys.has = (key) =>
      key === 'project.detail.metrics.reasons.COHENS_KAPPA.INSUFFICIENT_ANNOTATORS';
    const metrics = [
      metric({
        metricType: 'COHENS_KAPPA',
        status: 'INSUFFICIENT_ANNOTATORS',
        annotatorCount: 2,
        details: {},
      }),
    ];
    const { result } = renderHook(() => useProjectMetricsSection(baseParams({ metrics })));

    expect(result.current.metricItems[0].nonCalculableMessage).toBe(
      'project.detail.metrics.reasons.COHENS_KAPPA.INSUFFICIENT_ANNOTATORS:{"annotators":2,"defaultValue":"","items":0}',
    );
  });

  it('reports the missing-items count via missingCompleteUnits before falling back to skippedUnits', () => {
    const metrics = [
      metric({
        metricType: 'COHENS_KAPPA',
        status: 'INSUFFICIENT_ITEMS',
        details: { missingCompleteUnits: 4, skippedUnits: 9 },
      }),
    ];
    const { result } = renderHook(() => useProjectMetricsSection(baseParams({ metrics })));

    expect(result.current.metricItems[0].nonCalculableMessage).toBe(
      'project.detail.metrics.counts.missingItem.other:{"count":4}',
    );
  });

  it('falls through to the reason cascade when no item-shortfall detail is present', () => {
    resolvableKeys.has = (key) =>
      key === 'project.detail.metrics.reasons.COHENS_KAPPA.INSUFFICIENT_ITEMS';
    const metrics = [
      metric({ metricType: 'COHENS_KAPPA', status: 'INSUFFICIENT_ITEMS', details: {} }),
    ];
    const { result } = renderHook(() => useProjectMetricsSection(baseParams({ metrics })));

    expect(result.current.metricItems[0].nonCalculableMessage).toBe(
      'project.detail.metrics.reasons.COHENS_KAPPA.INSUFFICIENT_ITEMS:{"annotators":0,"defaultValue":"","items":0}',
    );
  });
});

describe('nonCalculableMessage fallback cascade for an uncounted status', () => {
  it('uses the per-metric-status reason when the resource defines it', () => {
    resolvableKeys.has = (key) =>
      key === 'project.detail.metrics.reasons.COHENS_KAPPA.NO_ANNOTATIONS';
    const metrics = [metric({ metricType: 'COHENS_KAPPA', status: 'NO_ANNOTATIONS' })];
    const { result } = renderHook(() => useProjectMetricsSection(baseParams({ metrics })));

    expect(result.current.metricItems[0].nonCalculableMessage).toBe(
      'project.detail.metrics.reasons.COHENS_KAPPA.NO_ANNOTATIONS:{"annotators":0,"defaultValue":"","items":0}',
    );
  });

  it('falls back to the generic status reason when the specific one is undefined', () => {
    resolvableKeys.has = (key) => key === 'project.detail.metrics.reasons.generic.NO_ANNOTATIONS';
    const metrics = [metric({ metricType: 'COHENS_KAPPA', status: 'NO_ANNOTATIONS' })];
    const { result } = renderHook(() => useProjectMetricsSection(baseParams({ metrics })));

    expect(result.current.metricItems[0].nonCalculableMessage).toBe(
      'project.detail.metrics.reasons.generic.NO_ANNOTATIONS:{"annotators":0,"defaultValue":"","items":0}',
    );
  });

  it('falls back to the plain status label when neither reason resolves', () => {
    const metrics = [metric({ metricType: 'COHENS_KAPPA', status: 'NO_ANNOTATIONS' })];
    const { result } = renderHook(() => useProjectMetricsSection(baseParams({ metrics })));

    expect(result.current.metricItems[0].nonCalculableMessage).toBe(
      'project.detail.metrics.status.noAnnotations',
    );
  });
});
