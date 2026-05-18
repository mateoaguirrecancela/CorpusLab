package es.udc.fic.corpuslab.modules.project.iaa.domain.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;

public record IaaCalculationContext(
        Long projectId,
        ProjectType projectType,
        List<IaaAnnotation> annotations,
        List<IaaDatasetItem> datasetItems,
        List<IaaAnnotator> annotators,
        Map<String, Object> metadata) {

    public IaaCalculationContext {
        Objects.requireNonNull(projectType, "projectType is required");
        annotations = List.copyOf(Objects.requireNonNull(annotations, "annotations is required"));
        datasetItems = List.copyOf(Objects.requireNonNull(datasetItems, "datasetItems is required"));
        annotators = List.copyOf(Objects.requireNonNull(annotators, "annotators is required"));
        metadata = immutableCopy(metadata);
    }

    private static Map<String, Object> immutableCopy(Map<String, Object> source) {
        Objects.requireNonNull(source, "metadata is required");
        if (source.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
