package es.udc.fic.corpuslab.modules.project.metrics;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import es.udc.fic.corpuslab.modules.project.metrics.dtos.ProjectMetricsDto;

@RestController
@RequestMapping("/api/projects")
public class ProjectMetricsController {

    private final ProjectMetricsService projectMetricsService;

    public ProjectMetricsController(ProjectMetricsService projectMetricsService) {
        this.projectMetricsService = projectMetricsService;
    }

    @GetMapping("/{projectId}/metrics")
    @ResponseStatus(HttpStatus.OK)
    public ProjectMetricsDto getProjectMetrics(
            Authentication authentication,
            @PathVariable Long projectId) {
        return projectMetricsService.getProjectMetrics(authentication.getName(), projectId);
    }
}
