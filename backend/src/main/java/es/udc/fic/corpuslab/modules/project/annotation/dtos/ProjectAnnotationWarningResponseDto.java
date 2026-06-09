package es.udc.fic.corpuslab.modules.project.annotation.dtos;

public record ProjectAnnotationWarningResponseDto(
        Long projectId,
        Long datasetItemId,
        Integer stepIndex,
        boolean warning) {
}
