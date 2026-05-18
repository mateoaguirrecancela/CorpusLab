package es.udc.fic.corpuslab.modules.project.metrics;

import es.udc.fic.corpuslab.modules.project.metrics.dtos.ProjectMetricsDto;

public interface ProjectMetricsService {

    ProjectMetricsDto getProjectMetrics(String authenticatedEmail, Long projectId);
}
