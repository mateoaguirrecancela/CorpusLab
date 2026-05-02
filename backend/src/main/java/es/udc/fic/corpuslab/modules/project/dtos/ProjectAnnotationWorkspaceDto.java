package es.udc.fic.corpuslab.modules.project.dtos;

import java.util.List;

import es.udc.fic.corpuslab.modules.project.enums.ProjectType;

public record ProjectAnnotationWorkspaceDto(
        Long projectId,
        ProjectType projectType,
        String annotationTargetColumn,
        List<ProjectSetupLabelDto> labels,
        int offset,
        int limit,
        long totalSteps,
        long completedSteps,
        int completionPercentage,
        int firstPendingStepIndex,
        List<ProjectAnnotationStepDto> steps) {
}
