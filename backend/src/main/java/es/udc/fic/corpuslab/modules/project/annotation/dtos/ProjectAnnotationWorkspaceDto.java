package es.udc.fic.corpuslab.modules.project.annotation.dtos;

import java.util.List;

import es.udc.fic.corpuslab.modules.project.setup.dtos.ProjectSetupLabelDto;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;

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
