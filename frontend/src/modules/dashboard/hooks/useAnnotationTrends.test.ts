import { act, renderHook } from '@testing-library/react';
import { useQuery } from '@tanstack/react-query';
import { describe, expect, it, vi } from 'vitest';
import { useAnnotationTrends } from '@/modules/dashboard/hooks/useAnnotationTrends';
import { getAnnotationTrendsErrorMessage } from '@/modules/dashboard/services/dashboardService';
import { type AnnotationTrendsResponse } from '@/modules/dashboard/types/dashboard';

vi.mock('@tanstack/react-query', () => ({
  useQuery: vi.fn(),
}));

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    i18n: {},
    t: (key: string) => key,
  }),
}));

vi.mock('@/app/config/i18n', () => ({
  getResolvedLanguage: () => 'en-US',
}));

vi.mock('@/modules/dashboard/services/dashboardService', () => ({
  getAnnotationTrends: vi.fn(),
  getAnnotationTrendsErrorMessage: vi.fn(() => 'trends-error'),
}));

function mockQuery(overrides: Partial<{ data: AnnotationTrendsResponse; isLoading: boolean; isError: boolean; error: unknown }>) {
  vi.mocked(useQuery).mockReturnValue({
    data: undefined,
    isLoading: false,
    isError: false,
    error: null,
    ...overrides,
  } as never);
}

const response: AnnotationTrendsResponse = {
  data: [
    { isoDate: '2026-01-05', values: { groupA: 3, groupB: 1 } },
    { isoDate: '2026-01-06', values: { groupA: 5, groupB: 2 } },
  ],
  series: [
    { color: '#111', dataKey: 'groupA', name: 'Group A' },
    { color: '#222', dataKey: 'groupB', name: 'Group B' },
  ],
};

describe('useAnnotationTrends', () => {
  it('returns empty data and a single "all groups" filter option while there is no data', () => {
    mockQuery({});

    const { result } = renderHook(() => useAnnotationTrends());

    expect(result.current.data).toEqual([]);
    expect(result.current.filterOptions).toEqual([
      { label: 'home.dashboard.trends.allGroups', value: 'all' },
    ]);
    expect(result.current.series).toEqual([]);
  });

  it('builds chart data keyed by series with a formatted day label', () => {
    mockQuery({ data: response });

    const { result } = renderHook(() => useAnnotationTrends());

    expect(result.current.data).toEqual([
      { groupA: 3, groupB: 1, dayLabel: expect.any(String), isoDate: '2026-01-05' },
      { groupA: 5, groupB: 2, dayLabel: expect.any(String), isoDate: '2026-01-06' },
    ]);
    expect(result.current.filterOptions).toEqual([
      { label: 'home.dashboard.trends.allGroups', value: 'all' },
      { label: 'Group A', value: 'groupA' },
      { label: 'Group B', value: 'groupB' },
    ]);
    expect(result.current.series).toEqual(response.series);
  });

  it('filters the series down to the selected group', () => {
    mockQuery({ data: response });
    const { result } = renderHook(() => useAnnotationTrends());

    act(() => result.current.setSelectedGroupFilter('groupB'));

    expect(result.current.series).toEqual([response.series[1]]);
    expect(result.current.selectedGroupFilter).toBe('groupB');
  });

  it('falls back to "all groups" when the selected filter no longer exists in the data', () => {
    mockQuery({ data: response });
    const { result, rerender } = renderHook(() => useAnnotationTrends());

    act(() => result.current.setSelectedGroupFilter('groupB'));
    expect(result.current.selectedGroupFilter).toBe('groupB');

    mockQuery({ data: { ...response, series: [response.series[0]] } });
    rerender();

    expect(result.current.selectedGroupFilter).toBe('all');
    expect(result.current.series).toEqual([response.series[0]]);
  });

  it('maps a query error to the annotation trends error message', () => {
    mockQuery({ isError: true, error: new Error('boom') });

    const { result } = renderHook(() => useAnnotationTrends());

    expect(getAnnotationTrendsErrorMessage).toHaveBeenCalled();
    expect(result.current.errorMessage).toBe('trends-error');
  });
});
