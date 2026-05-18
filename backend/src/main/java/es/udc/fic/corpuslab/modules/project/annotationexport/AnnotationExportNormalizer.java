package es.udc.fic.corpuslab.modules.project.annotationexport;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import es.udc.fic.corpuslab.common.utils.StringUtils;
import es.udc.fic.corpuslab.modules.project.shared.utils.ProjectAnnotationUtils;
import es.udc.fic.corpuslab.modules.project.shared.utils.ProjectConstants;
import es.udc.fic.corpuslab.modules.project.shared.utils.ProjectDatasetUtils;

@Component
public class AnnotationExportNormalizer {

    public String extractCommentValue(Object annotationPayload) {
        if (!(annotationPayload instanceof Map<?, ?> annotationAsMap)) {
            return "";
        }
        Map<String, Object> annotationMap = ProjectAnnotationUtils.toMutableStringObjectMap(annotationAsMap);
        String notes = StringUtils.trimToNull(
                ProjectDatasetUtils.valueAsString(annotationMap.get(ProjectConstants.ANNOTATION_KEY_NOTES)));
        return notes == null ? "" : notes;
    }

    public String normalizeListLikeValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String stringValue) {
            String normalized = stringValue.trim();
            return normalized.isEmpty() ? "" : normalized;
        }
        if (!(value instanceof List<?> listValue)) {
            return "";
        }
        List<String> normalized = listValue.stream()
                .filter(entry -> entry != null)
                .map(String::valueOf)
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .distinct()
                .toList();
        return String.join("|", normalized);
    }
}
