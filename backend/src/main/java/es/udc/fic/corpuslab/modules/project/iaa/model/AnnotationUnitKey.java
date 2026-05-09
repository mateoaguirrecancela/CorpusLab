package es.udc.fic.corpuslab.modules.project.iaa.model;

import java.util.Objects;

public record AnnotationUnitKey(
        Long datasetItemId,
        int stepIndex) {

    public AnnotationUnitKey {
        Objects.requireNonNull(datasetItemId, "datasetItemId is required");
    }
}
