import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { getResolvedLanguage } from '@/app/config/i18n';
import { dashboardQueryKeys } from '@/modules/dashboard/queryKeys';
import {
  getAnnotationTrends,
  getAnnotationTrendsErrorMessage,
} from '@/modules/dashboard/services/dashboardService';
import {
  type AnnotationTrendDatum,
  type AnnotationTrendFilterOption,
  type AnnotationTrendSeries,
  type AnnotationTrendsResponse,
} from '@/modules/dashboard/types/dashboard';

type AnnotationTrendsState = Readonly<{
  data: AnnotationTrendDatum[];
  errorMessage: string;
  filterOptions: AnnotationTrendFilterOption[];
  isLoading: boolean;
  selectedGroupFilter: string;
  series: AnnotationTrendSeries[];
  setSelectedGroupFilter: (value: string) => void;
}>;

const ALL_GROUPS_FILTER = 'all';
const EMPTY_SERIES: AnnotationTrendSeries[] = [];

function formatTrendDayLabel(isoDate: string, locale: string): string {
  return new Intl.DateTimeFormat(locale, { weekday: 'short' }).format(
    new Date(`${isoDate}T00:00:00Z`),
  );
}

function toChartData(
  response: AnnotationTrendsResponse | undefined,
  locale: string,
): AnnotationTrendDatum[] {
  if (!response || response.series.length === 0) {
    return [];
  }

  return response.data.map((datum) => {
    const seriesValues = Object.fromEntries(
      response.series.map((item) => [item.dataKey, datum.values[item.dataKey] ?? 0]),
    );

    return {
      ...seriesValues,
      dayLabel: formatTrendDayLabel(datum.isoDate, locale),
      isoDate: datum.isoDate,
    };
  });
}

export function useAnnotationTrends(): AnnotationTrendsState {
  const { i18n, t } = useTranslation();
  const [selectedGroupFilter, setSelectedGroupFilter] = useState(ALL_GROUPS_FILTER);
  const query = useQuery({
    queryKey: dashboardQueryKeys.trends,
    queryFn: getAnnotationTrends,
  });
  const locale = getResolvedLanguage(i18n);

  const allSeries = query.data?.series ?? EMPTY_SERIES;
  const isSelectedGroupFilterAvailable =
    selectedGroupFilter === ALL_GROUPS_FILTER ||
    allSeries.some((item) => item.dataKey === selectedGroupFilter);
  const effectiveSelectedGroupFilter = isSelectedGroupFilterAvailable
    ? selectedGroupFilter
    : ALL_GROUPS_FILTER;

  const filterOptions = useMemo<AnnotationTrendFilterOption[]>(
    () => [
      {
        label: t('home.dashboard.trends.allGroups'),
        value: ALL_GROUPS_FILTER,
      },
      ...allSeries.map((item) => ({
        label: item.name,
        value: item.dataKey,
      })),
    ],
    [allSeries, t],
  );

  const series = useMemo(() => {
    if (effectiveSelectedGroupFilter === ALL_GROUPS_FILTER) {
      return allSeries;
    }

    return allSeries.filter((item) => item.dataKey === effectiveSelectedGroupFilter);
  }, [allSeries, effectiveSelectedGroupFilter]);
  const data = useMemo(() => toChartData(query.data, locale), [locale, query.data]);

  return {
    data,
    errorMessage: query.isError ? getAnnotationTrendsErrorMessage(query.error) : '',
    filterOptions,
    isLoading: query.isLoading,
    selectedGroupFilter: effectiveSelectedGroupFilter,
    series,
    setSelectedGroupFilter,
  };
}
