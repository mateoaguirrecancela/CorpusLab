package es.udc.fic.corpuslab.modules.project.annotation;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.shared.exceptions.InvalidProjectDatasetException;
import es.udc.fic.corpuslab.modules.project.shared.utils.ProjectAnnotationUtils;

@Component
public class AnnotationPayloadNormalizer {

    public Object normalize(Object annotationPayload, ProjectType projectType) {
        Object normalized = requirePayload(annotationPayload);
        if (projectType == ProjectType.NER) {
            return ProjectAnnotationUtils.normalizeNerAnnotationPayload(normalized);
        }
        return normalized;
    }

    private Object requirePayload(Object annotationPayload) {
        if (annotationPayload == null) {
            throw new InvalidProjectDatasetException("Annotation payload is required");
        }
        if (annotationPayload instanceof String stringValue && stringValue.trim().isEmpty()) {
            throw new InvalidProjectDatasetException("Annotation payload is required");
        }
        if (annotationPayload instanceof List<?> listValue && listValue.isEmpty()) {
            throw new InvalidProjectDatasetException("Annotation payload is required");
        }
        if (annotationPayload instanceof Map<?, ?> mapValue && mapValue.isEmpty()) {
            throw new InvalidProjectDatasetException("Annotation payload is required");
        }
        return annotationPayload;
    }
}
