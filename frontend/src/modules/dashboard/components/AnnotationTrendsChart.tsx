import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { useTranslation } from 'react-i18next';
import { Select } from '@/components/ui/select';
import { Spinner } from '@/components/ui/spinner';
import { DashboardPanel } from '@/modules/dashboard/components/DashboardPanel';
import {
  type AnnotationTrendDatum,
  type AnnotationTrendFilterOption,
  type AnnotationTrendSeries,
} from '@/modules/dashboard/types/dashboard';

type AnnotationTrendsChartProps = Readonly<{
  data: AnnotationTrendDatum[];
  errorMessage: string;
  filterOptions: AnnotationTrendFilterOption[];
  isLoading: boolean;
  selectedGroupFilter: string;
  series: AnnotationTrendSeries[];
  onGroupFilterChange: (value: string) => void;
}>;

type TrendTooltipPayload = Readonly<{
  color?: string;
  name?: string;
  payload?: AnnotationTrendDatum;
  value?: number | string;
}>;

type AnnotationTrendTooltipProps = Readonly<{
  active?: boolean;
  label?: string;
  payload?: TrendTooltipPayload[];
}>;

function AnnotationTrendTooltip({ active, label, payload }: AnnotationTrendTooltipProps) {
  if (!active || !payload || payload.length === 0) {
    return null;
  }

  const total = payload.reduce((sum, item) => {
    if (typeof item.value !== 'number') {
      return sum;
    }

    return sum + item.value;
  }, 0);

  return (
    <div className="min-w-48 rounded-lg border border-border bg-surface-base px-3 py-2 shadow-[var(--shadow-elevated-card)]">
      <p className="text-xs font-bold tracking-[0.1em] text-muted-foreground uppercase">{label}</p>
      <p className="mt-1 text-sm font-black text-primary">{total} anotaciones</p>
      <div className="mt-2 space-y-1">
        {payload.map((item) => (
          <div className="flex items-center justify-between gap-4 text-xs" key={item.name}>
            <span className="inline-flex min-w-0 items-center gap-2 text-muted-foreground">
              <span
                className="size-2 shrink-0 rounded-full"
                style={{ backgroundColor: item.color }}
              />
              <span className="truncate">{item.name}</span>
            </span>
            <span className="font-semibold tabular-nums text-foreground">{item.value}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

function ChartLoadingState() {
  const { t } = useTranslation();

  return (
    <div className="flex h-72 items-center justify-center rounded-lg border border-dashed border-border bg-background text-sm text-muted-foreground">
      <span className="inline-flex items-center gap-2">
        <Spinner aria-hidden className="size-4" />
        {t('home.dashboard.loading.trends')}
      </span>
    </div>
  );
}

function ChartEmptyState({ message }: Readonly<{ message: string }>) {
  return (
    <div className="flex h-72 items-center justify-center rounded-lg border border-dashed border-border bg-background px-4 text-center text-sm text-muted-foreground">
      <span className="inline-flex items-center gap-2">{message}</span>
    </div>
  );
}

function ResearchGroupFilter({
  options,
  value,
  onChange,
}: Readonly<{
  options: AnnotationTrendFilterOption[];
  value: string;
  onChange: (value: string) => void;
}>) {
  const { t } = useTranslation();

  return (
    <div className="w-full lg:w-64">
      <label className="sr-only" htmlFor="annotation-trends-group-filter">
        {t('home.dashboard.trends.filterLabel')}
      </label>
      <Select
        id="annotation-trends-group-filter"
        value={value}
        onChange={(event) => onChange(event.target.value)}
      >
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </Select>
    </div>
  );
}

type AnnotationTrendLineChartProps = Readonly<{
  data: AnnotationTrendDatum[];
  series: AnnotationTrendSeries[];
}>;

function AnnotationTrendLineChart({ data, series }: AnnotationTrendLineChartProps) {
  return (
    <div className="h-76 min-w-0">
      <ResponsiveContainer height="100%" width="100%">
        <LineChart data={data} margin={{ bottom: 0, left: -14, right: 8, top: 8 }}>
          <CartesianGrid stroke="var(--border)" strokeDasharray="3 3" vertical={false} />
          <XAxis
            axisLine={false}
            dataKey="dayLabel"
            tick={{ fill: 'var(--muted-foreground)', fontSize: 12 }}
            tickLine={false}
          />
          <YAxis
            allowDecimals={false}
            axisLine={false}
            tick={{ fill: 'var(--muted-foreground)', fontSize: 12 }}
            tickLine={false}
          />
          <Tooltip
            content={<AnnotationTrendTooltip />}
            cursor={{ stroke: 'var(--border)', strokeDasharray: '4 4' }}
          />
          <Legend
            iconSize={9}
            wrapperStyle={{
              color: 'var(--muted-foreground)',
              fontSize: '12px',
              paddingTop: '12px',
            }}
          />
          {series.map((item) => (
            <Line
              activeDot={{ r: 5, strokeWidth: 0 }}
              dataKey={item.dataKey}
              dot={{ r: 3, strokeWidth: 2 }}
              key={item.dataKey}
              name={item.name}
              stroke={item.color}
              strokeWidth={2.5}
              type="linear"
            />
          ))}
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}

export function AnnotationTrendsChart({
  data,
  errorMessage,
  filterOptions,
  isLoading,
  selectedGroupFilter,
  series,
  onGroupFilterChange,
}: AnnotationTrendsChartProps) {
  const { t } = useTranslation();
  const hasChartData = data.length > 0 && series.length > 0;

  function renderChartContent() {
    if (isLoading) {
      return <ChartLoadingState />;
    }

    if (errorMessage.length > 0) {
      return <ChartEmptyState message={errorMessage} />;
    }

    if (hasChartData) {
      return <AnnotationTrendLineChart data={data} series={series} />;
    }

    return <ChartEmptyState message={t('home.dashboard.empty.trends')} />;
  }

  return (
    <DashboardPanel
      action={
        <ResearchGroupFilter
          options={filterOptions}
          value={selectedGroupFilter}
          onChange={onGroupFilterChange}
        />
      }
      title={t('home.dashboard.trends.title')}
    >
      {renderChartContent()}
    </DashboardPanel>
  );
}
