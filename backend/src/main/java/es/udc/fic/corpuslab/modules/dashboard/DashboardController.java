package es.udc.fic.corpuslab.modules.dashboard;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import es.udc.fic.corpuslab.modules.dashboard.dtos.DashboardAnnotationTrendsDto;
import es.udc.fic.corpuslab.modules.dashboard.dtos.DashboardMetricsDto;
import es.udc.fic.corpuslab.modules.dashboard.dtos.DashboardProjectsDto;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/metrics")
    @ResponseStatus(HttpStatus.OK)
    public DashboardMetricsDto metrics(Authentication authentication) {
        return dashboardService.getMetrics(authentication.getName());
    }

    @GetMapping("/annotation-trends")
    @ResponseStatus(HttpStatus.OK)
    public DashboardAnnotationTrendsDto annotationTrends(Authentication authentication) {
        return dashboardService.getAnnotationTrends(authentication.getName());
    }

    @GetMapping("/projects")
    @ResponseStatus(HttpStatus.OK)
    public DashboardProjectsDto projects(Authentication authentication) {
        return dashboardService.getProjects(authentication.getName());
    }
}
