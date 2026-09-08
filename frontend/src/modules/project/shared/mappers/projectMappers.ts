import type { ProjectMetricsDto } from '@/modules/project/shared/api/projectDtos';
import type { ProjectMetric } from '@/modules/project/shared/types/project';

export function toProjectMetrics(dto: ProjectMetricsDto): ProjectMetric[] {
  return dto.metrics.map((metric) => ({
    ...metric,
    value: Number.isFinite(metric.value) ? metric.value : null,
    message: metric.message?.length ? metric.message : null,
  }));
}
