package es.udc.fic.corpuslab.modules.project.dtos;

public record ProjectAnnotationStepDto(
        Long datasetItemId,
        Integer datasetItemIndex,
        int stepIndex,
        int totalStepsForItem,
        String sourceName,
        String sourceMimeType,
        String preview,
        boolean completed,
        Object annotation) {
}
