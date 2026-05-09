package es.udc.fic.corpuslab.modules.project.services;

import es.udc.fic.corpuslab.modules.project.dtos.ProjectMetricsDto;

public interface ProjectMetricsService {

    ProjectMetricsDto getProjectMetrics(String authenticatedEmail, Long projectId);
}
