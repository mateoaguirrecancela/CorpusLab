package es.udc.fic.corpuslab.modules.project.dtos;

import java.util.List;

import es.udc.fic.corpuslab.modules.project.enums.ProjectType;

public record ProjectAnnotationWorkspaceDto(
        Long projectId,
        ProjectType projectType,
        List<ProjectSetupLabelDto> labels,
        int offset,
        int limit,
        long totalSteps,
        long completedSteps,
        int completionPercentage,
        List<ProjectAnnotationStepDto> steps) {
}
