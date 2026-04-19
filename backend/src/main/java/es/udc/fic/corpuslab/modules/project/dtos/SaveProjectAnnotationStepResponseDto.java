package es.udc.fic.corpuslab.modules.project.dtos;

public record SaveProjectAnnotationStepResponseDto(
        Long projectId,
        Long datasetItemId,
        int stepIndex,
        long participantCompletedSteps,
        long participantTotalSteps,
        int participantCompletionPercentage,
        int projectCompletionPercentage) {
}
