package es.udc.fic.corpuslab.modules.dashboard.dtos;

public record DashboardMetricsDto(
        long activeProjects,
        long activeResearchGroups,
        long annotationsToday,
        long annotationsYesterday,
        long pendingAnnotations,
        long pendingProjects,
        long openAlerts,
        long alertsSinceYesterday) {
}
