import { type ProjectParticipantRole } from '@/modules/project/types/project';

export type DashboardMetricKind =
  | 'active-projects'
  | 'annotations-today'
  | 'pending-annotations'
  | 'open-alerts';

export type DashboardMetricTrendDirection = 'up' | 'down' | 'neutral';

export type DashboardMetricTrend = Readonly<{
  direction: DashboardMetricTrendDirection;
  label: string;
  value: number;
}>;

export type DashboardMetric = Readonly<{
  kind: DashboardMetricKind;
  supportingText: string;
  trend?: DashboardMetricTrend;
  value: number;
}>;

export type DashboardMetricsResponse = Readonly<{
  activeProjects: number;
  activeResearchGroups: number;
  annotationsToday: number;
  annotationsYesterday: number;
  pendingAnnotations: number;
  pendingProjects: number;
  openAlerts: number;
  alertsSinceYesterday: number;
}>;

export type AnnotationTrendSeries = Readonly<{
  color: string;
  dataKey: string;
  name: string;
}>;

export type AnnotationTrendFilterOption = Readonly<{
  label: string;
  value: string;
}>;

export type AnnotationTrendDatum = Readonly<
  {
    dayLabel: string;
    isoDate: string;
  } & Record<string, number | string>
>;

export type AnnotationTrendApiDatum = Readonly<{
  isoDate: string;
  values: Record<string, number>;
}>;

export type AnnotationTrendsResponse = Readonly<{
  data: AnnotationTrendApiDatum[];
  series: AnnotationTrendSeries[];
}>;

export type DashboardProject = Readonly<{
  completionPercentage: number;
  description: string | null;
  id: number;
  pendingAnnotations: number;
  participantRole: ProjectParticipantRole;
  researchGroupName: string;
  updatedAt: string;
  name: string;
}>;

export type DashboardProjectsResponse = Readonly<{
  advancedProjects: DashboardProject[];
  recentProjects: DashboardProject[];
}>;
