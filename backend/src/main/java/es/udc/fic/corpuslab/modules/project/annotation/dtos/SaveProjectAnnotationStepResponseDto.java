package es.udc.fic.corpuslab.modules.project.annotation.dtos;

public record SaveProjectAnnotationStepResponseDto(
        Long projectId,
        Long datasetItemId,
        int stepIndex,
        long participantCompletedSteps,
        long participantTotalSteps,
        int participantCompletionPercentage,
        int projectCompletionPercentage) {
}
