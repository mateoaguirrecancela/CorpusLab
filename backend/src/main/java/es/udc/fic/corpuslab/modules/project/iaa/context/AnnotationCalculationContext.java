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
        annotations = annotations == null ? List.of() : List.copyOf(annotations);
        datasetItems = datasetItems == null ? List.of() : List.copyOf(datasetItems);
        annotators = annotators == null ? List.of() : List.copyOf(annotators);
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
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
