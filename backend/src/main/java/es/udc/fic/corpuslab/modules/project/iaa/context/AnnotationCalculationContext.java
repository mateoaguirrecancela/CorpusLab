package es.udc.fic.corpuslab.modules.project.iaa.context;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import es.udc.fic.corpuslab.modules.project.entities.Annotation;
import es.udc.fic.corpuslab.modules.project.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.entities.Project;
import es.udc.fic.corpuslab.modules.project.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.enums.ProjectType;

public record AnnotationCalculationContext(
        Long projectId,
        ProjectType projectType,
        List<Annotation> annotations,
        List<DatasetItem> datasetItems,
        List<ProjectParticipant> annotators,
        Map<String, Object> metadata) {

    public AnnotationCalculationContext {
        Objects.requireNonNull(projectType, "projectType is required");
        annotations = List.copyOf(Objects.requireNonNull(annotations, "annotations is required"));
        datasetItems = List.copyOf(Objects.requireNonNull(datasetItems, "datasetItems is required"));
        annotators = List.copyOf(Objects.requireNonNull(annotators, "annotators is required"));
        metadata = immutableCopy(metadata);
    }

    public static AnnotationCalculationContext fromProject(
            Project project,
            List<Annotation> annotations,
            List<DatasetItem> datasetItems,
            List<ProjectParticipant> annotators,
            Map<String, Object> metadata) {
        Objects.requireNonNull(project, "project is required");
        return new AnnotationCalculationContext(
                project.getId(),
                project.getProjectType(),
                annotations,
                datasetItems,
                annotators,
                metadata);
    }

    private static Map<String, Object> immutableCopy(Map<String, Object> source) {
        Objects.requireNonNull(source, "metadata is required");
        if (source.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
