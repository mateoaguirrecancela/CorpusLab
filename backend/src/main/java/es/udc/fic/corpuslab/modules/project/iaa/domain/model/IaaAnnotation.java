package es.udc.fic.corpuslab.modules.project.iaa.domain.model;

public record IaaAnnotation(
        Long datasetItemId,
        Long annotatorId,
        Integer stepIndex,
        Object payload) {
}
