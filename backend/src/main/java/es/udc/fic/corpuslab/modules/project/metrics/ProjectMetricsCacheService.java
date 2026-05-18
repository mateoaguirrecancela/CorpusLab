package es.udc.fic.corpuslab.modules.project.metrics;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import es.udc.fic.corpuslab.modules.project.metrics.dtos.ProjectMetricsDto;
import es.udc.fic.corpuslab.modules.project.progress.ProjectProgressCalculator;

@Service
public class ProjectMetricsCacheService {

    private final ProjectMetricsCalculator projectMetricsCalculator;
    private final ProjectProgressCalculator projectProgressCalculator;

    @Autowired
    public ProjectMetricsCacheService(
            ProjectMetricsCalculator projectMetricsCalculator,
            ProjectProgressCalculator projectProgressCalculator) {
        this.projectMetricsCalculator = projectMetricsCalculator;
        this.projectProgressCalculator = projectProgressCalculator;
    }

    @Cacheable(cacheNames = "projectMetrics", key = "#projectId")
    public ProjectMetricsDto getProjectMetrics(Long projectId) {
        return projectMetricsCalculator.calculate(projectId);
    }

    @CacheEvict(cacheNames = "projectMetrics", key = "#projectId")
    public void evictProjectMetrics(Long projectId) {
        // Annotation mutations call this to force the next read to recalculate IAA.
    }

    @Cacheable(cacheNames = "projectProgress", key = "#projectId")
    public int getProjectCompletionPercentage(Long projectId) {
        return projectProgressCalculator.calculateProjectCompletionPercentage(projectId);
    }

    @CacheEvict(cacheNames = "projectProgress", key = "#projectId")
    public void evictProjectProgress(Long projectId) {
        // Project list summaries call this to avoid recalculating progress on every page load.
    }

    @CacheEvict(cacheNames = { "projectMetrics", "projectProgress" }, key = "#projectId")
    public void evictProjectReadCaches(Long projectId) {
        // Shared invalidation for annotation, dataset, and participant changes.
    }
}
