package es.udc.fic.corpuslab.modules.project.metrics;

import org.springframework.stereotype.Service;

import es.udc.fic.corpuslab.modules.project.metrics.dtos.ProjectMetricsDto;
import es.udc.fic.corpuslab.modules.project.iaa.adapter.in.project.ProjectIaaMetricsAdapter;

@Service
public class ProjectMetricsCalculator {

    private final ProjectIaaMetricsAdapter projectIaaMetricsAdapter;

    public ProjectMetricsCalculator(ProjectIaaMetricsAdapter projectIaaMetricsAdapter) {
        this.projectIaaMetricsAdapter = projectIaaMetricsAdapter;
    }

    public ProjectMetricsDto calculate(Long projectId) {
        return projectIaaMetricsAdapter.calculate(projectId);
    }
}
