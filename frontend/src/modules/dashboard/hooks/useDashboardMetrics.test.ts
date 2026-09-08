import { renderHook } from '@testing-library/react';
import { useQuery } from '@tanstack/react-query';
import { describe, expect, it, vi } from 'vitest';
import { useDashboardMetrics } from '@/modules/dashboard/hooks/useDashboardMetrics';
import { getDashboardMetricsErrorMessage } from '@/modules/dashboard/services/dashboardService';
import { type DashboardMetricsResponse } from '@/modules/dashboard/types/dashboard';

vi.mock('@tanstack/react-query', () => ({
  useQuery: vi.fn(),
}));

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string, opts?: Record<string, unknown>) =>
      opts ? `${key}:${JSON.stringify(opts)}` : key,
  }),
}));

vi.mock('@/modules/dashboard/services/dashboardService', () => ({
  getDashboardMetrics: vi.fn(),
  getDashboardMetricsErrorMessage: vi.fn(() => 'dashboard-metrics-error'),
}));

function mockQuery(overrides: Partial<{ data: DashboardMetricsResponse; isLoading: boolean; isError: boolean; error: unknown }>) {
  vi.mocked(useQuery).mockReturnValue({
    data: undefined,
    isLoading: false,
    isError: false,
    error: null,
    ...overrides,
  } as never);
}

describe('useDashboardMetrics', () => {
  it('defaults every metric to 0 while there is no data', () => {
    mockQuery({});

    const { result } = renderHook(() => useDashboardMetrics());

    expect(result.current.metrics.map((metric) => metric.value)).toEqual([0, 0, 0, 0]);
    expect(result.current.isLoading).toBe(false);
  });

  it('shows an upward trend when annotations increased since yesterday', () => {
    mockQuery({ data: { annotationsToday: 12, annotationsYesterday: 5 } as DashboardMetricsResponse });

    const { result } = renderHook(() => useDashboardMetrics());

    const annotationsMetric = result.current.metrics.find(
      (metric) => metric.kind === 'annotations-today',
    );
    expect(annotationsMetric?.trend?.direction).toBe('up');
    expect(annotationsMetric?.trend?.value).toBe(7);
  });

  it('shows a downward trend when annotations dropped since yesterday', () => {
    mockQuery({ data: { annotationsToday: 3, annotationsYesterday: 10 } as DashboardMetricsResponse });

    const { result } = renderHook(() => useDashboardMetrics());

    const annotationsMetric = result.current.metrics.find(
      (metric) => metric.kind === 'annotations-today',
    );
    expect(annotationsMetric?.trend?.direction).toBe('down');
    expect(annotationsMetric?.trend?.value).toBe(7);
  });

  it('only flags open alerts as trending up when there are new alerts since yesterday', () => {
    mockQuery({ data: { alertsSinceYesterday: 0 } as DashboardMetricsResponse });
    const flat = renderHook(() => useDashboardMetrics());
    expect(flat.result.current.metrics.find((m) => m.kind === 'open-alerts')?.trend?.direction).toBe(
      'neutral',
    );

    mockQuery({ data: { alertsSinceYesterday: 2 } as DashboardMetricsResponse });
    const rising = renderHook(() => useDashboardMetrics());
    expect(
      rising.result.current.metrics.find((m) => m.kind === 'open-alerts')?.trend?.direction,
    ).toBe('up');
  });

  it('maps a query error to the dashboard metrics error message', () => {
    mockQuery({ isError: true, error: new Error('boom') });

    const { result } = renderHook(() => useDashboardMetrics());

    expect(getDashboardMetricsErrorMessage).toHaveBeenCalled();
    expect(result.current.errorMessage).toBe('dashboard-metrics-error');
  });
});
