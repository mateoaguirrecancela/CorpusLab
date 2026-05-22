package es.udc.fic.corpuslab.modules.dashboard;

import es.udc.fic.corpuslab.modules.dashboard.dtos.DashboardAnnotationTrendsDto;
import es.udc.fic.corpuslab.modules.dashboard.dtos.DashboardMetricsDto;
import es.udc.fic.corpuslab.modules.dashboard.dtos.DashboardProjectsDto;

public interface DashboardService {

    DashboardMetricsDto getMetrics(String authenticatedEmail);

    DashboardAnnotationTrendsDto getAnnotationTrends(String authenticatedEmail);

    DashboardProjectsDto getProjects(String authenticatedEmail);
}
