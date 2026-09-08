package es.udc.fic.corpuslab.modules.project.annotation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import es.udc.fic.corpuslab.common.utils.StringUtils;
import es.udc.fic.corpuslab.modules.project.shared.entities.Label;
import es.udc.fic.corpuslab.modules.project.shared.entities.Project;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.shared.exceptions.InvalidProjectAnnotationException;
import es.udc.fic.corpuslab.modules.project.shared.exceptions.InvalidProjectDatasetException;
import es.udc.fic.corpuslab.modules.project.shared.utils.ProjectAnnotationUtils;
import es.udc.fic.corpuslab.modules.project.shared.utils.ProjectConstants;

@Component
public class AnnotationPayloadNormalizer {

    public Object normalize(Object annotationPayload, Project project) {
        Object normalized = requirePayload(annotationPayload);
        ProjectType projectType = project.getProjectType();
        if (projectType == null) {
            throw new InvalidProjectAnnotationException("Project type is required");
        }

        Map<String, String> allowedLabelsByKey = buildAllowedLabelsByKey(project.getLabels());

        return switch (projectType) {
            case TEXT_CLASSIFICATION_SIMPLE -> normalizeSingleLabelPayload(normalized, allowedLabelsByKey);
            case TEXT_CLASSIFICATION_MULTILABEL -> normalizeMultilabelPayload(normalized, allowedLabelsByKey);
            case NER -> normalizeNerPayload(normalized, allowedLabelsByKey);
            case SEQ2SEQ -> normalizeSeq2SeqPayload(normalized);
        };
    }

    private Map<String, Object> normalizeSingleLabelPayload(Object annotationPayload,
            Map<String, String> allowedLabelsByKey) {
        Map<String, Object> payload = requireObjectPayload(annotationPayload,
                "Classification annotation payload must be an object with label");
        requireAllowedKeys(payload, Set.of(ProjectConstants.ANNOTATION_KEY_LABEL, ProjectConstants.ANNOTATION_KEY_NOTES));

        String label = normalizeAllowedLabel(payload.get(ProjectConstants.ANNOTATION_KEY_LABEL), allowedLabelsByKey);

        Map<String, Object> normalizedPayload = new LinkedHashMap<>();
        normalizedPayload.put(ProjectConstants.ANNOTATION_KEY_LABEL, label);
        addNotesIfPresent(normalizedPayload, payload);
        return normalizedPayload;
    }

    private Map<String, Object> normalizeMultilabelPayload(Object annotationPayload,
            Map<String, String> allowedLabelsByKey) {
        Map<String, Object> payload = requireObjectPayload(annotationPayload,
                "Multilabel annotation payload must be an object with labels");
        requireAllowedKeys(payload, Set.of(ProjectConstants.ANNOTATION_KEY_LABELS, ProjectConstants.ANNOTATION_KEY_NOTES));

        Object rawLabels = payload.get(ProjectConstants.ANNOTATION_KEY_LABELS);
        if (!(rawLabels instanceof List<?> rawLabelsList)) {
            throw new InvalidProjectAnnotationException("Multilabel annotation requires labels as a list");
        }

        Set<String> normalizedLabels = new LinkedHashSet<>();
        for (Object rawLabel : rawLabelsList) {
            normalizedLabels.add(normalizeAllowedLabel(rawLabel, allowedLabelsByKey));
        }

        if (normalizedLabels.isEmpty()) {
            throw new InvalidProjectAnnotationException("Multilabel annotation requires at least one label");
        }

        Map<String, Object> normalizedPayload = new LinkedHashMap<>();
        normalizedPayload.put(ProjectConstants.ANNOTATION_KEY_LABELS, new ArrayList<>(normalizedLabels));
        addNotesIfPresent(normalizedPayload, payload);
        return normalizedPayload;
    }

    private Map<String, Object> normalizeNerPayload(Object annotationPayload,
            Map<String, String> allowedLabelsByKey) {
        Map<String, Object> payload = requireObjectPayload(annotationPayload,
                "NER annotation payload must be an object with entities");
        requireAllowedKeys(payload, Set.of(ProjectConstants.NER_ANNOTATION_KEY_ENTITIES, ProjectConstants.ANNOTATION_KEY_NOTES));
        requireNerEntitiesWithoutUnsupportedFields(payload.get(ProjectConstants.NER_ANNOTATION_KEY_ENTITIES));
        if (payload.containsKey(ProjectConstants.ANNOTATION_KEY_NOTES)
                && payload.get(ProjectConstants.ANNOTATION_KEY_NOTES) != null
                && !(payload.get(ProjectConstants.ANNOTATION_KEY_NOTES) instanceof String)) {
            throw new InvalidProjectAnnotationException("Annotation notes must be text");
        }

        Map<String, Object> normalizedPayload;
        try {
            normalizedPayload = ProjectAnnotationUtils.normalizeNerAnnotationPayload(payload);
        } catch (InvalidProjectDatasetException ex) {
            throw new InvalidProjectAnnotationException(ex.getMessage());
        }

        Object rawEntities = normalizedPayload.get(ProjectConstants.NER_ANNOTATION_KEY_ENTITIES);
        if (!(rawEntities instanceof List<?> entities)) {
            throw new InvalidProjectAnnotationException("NER annotation requires entities as a list");
        }

        List<Map<String, Object>> normalizedEntities = new ArrayList<>();
        for (Object rawEntity : entities) {
            if (!(rawEntity instanceof Map<?, ?> rawEntityMap)) {
                throw new InvalidProjectAnnotationException("Each NER entity must be an object");
            }

            Map<String, Object> entity = ProjectAnnotationUtils.toMutableStringObjectMap(rawEntityMap);
            String label = normalizeAllowedLabel(
                    entity.get(ProjectConstants.NER_ANNOTATION_KEY_LABEL),
                    allowedLabelsByKey);

            Map<String, Object> normalizedEntity = new LinkedHashMap<>();
            normalizedEntity.put(ProjectConstants.NER_ANNOTATION_KEY_LABEL, label);
            normalizedEntity.put(ProjectConstants.NER_ANNOTATION_KEY_TEXT,
                    entity.get(ProjectConstants.NER_ANNOTATION_KEY_TEXT));
            normalizedEntity.put(ProjectConstants.NER_ANNOTATION_KEY_START_OFFSET,
                    entity.get(ProjectConstants.NER_ANNOTATION_KEY_START_OFFSET));
            normalizedEntity.put(ProjectConstants.NER_ANNOTATION_KEY_END_OFFSET,
                    entity.get(ProjectConstants.NER_ANNOTATION_KEY_END_OFFSET));
            normalizedEntities.add(normalizedEntity);
        }

        normalizedPayload.put(ProjectConstants.NER_ANNOTATION_KEY_ENTITIES, normalizedEntities);
        return normalizedPayload;
    }

    private void requireNerEntitiesWithoutUnsupportedFields(Object rawEntities) {
        if (!(rawEntities instanceof List<?> entities)) {
            return;
        }

        Set<String> allowedEntityKeys = Set.of(
                ProjectConstants.NER_ANNOTATION_KEY_LABEL,
                ProjectConstants.NER_ANNOTATION_KEY_TEXT,
                ProjectConstants.NER_ANNOTATION_KEY_START_OFFSET,
                ProjectConstants.NER_ANNOTATION_KEY_END_OFFSET);

        for (Object rawEntity : entities) {
            if (!(rawEntity instanceof Map<?, ?> rawEntityMap)) {
                continue;
            }

            Map<String, Object> entity = ProjectAnnotationUtils.toMutableStringObjectMap(rawEntityMap);
            requireAllowedKeys(entity, allowedEntityKeys);
        }
    }

    private Map<String, Object> normalizeSeq2SeqPayload(Object annotationPayload) {
        Map<String, Object> payload = requireObjectPayload(annotationPayload,
                "Seq2Seq annotation payload must be an object with text");
        requireAllowedKeys(payload, Set.of(ProjectConstants.ANNOTATION_KEY_TEXT, ProjectConstants.ANNOTATION_KEY_NOTES));

        String text = trimStringPayload(payload.get(ProjectConstants.ANNOTATION_KEY_TEXT),
                "Seq2Seq annotation text is required");

        Map<String, Object> normalizedPayload = new LinkedHashMap<>();
        normalizedPayload.put(ProjectConstants.ANNOTATION_KEY_TEXT, text);
        addNotesIfPresent(normalizedPayload, payload);
        return normalizedPayload;
    }

    private Map<String, Object> requireObjectPayload(Object annotationPayload, String message) {
        if (!(annotationPayload instanceof Map<?, ?> rawPayload)) {
            throw new InvalidProjectAnnotationException(message);
        }
        return ProjectAnnotationUtils.toMutableStringObjectMap(rawPayload);
    }

    private void requireAllowedKeys(Map<String, Object> payload, Set<String> allowedKeys) {
        for (String key : payload.keySet()) {
            if (!allowedKeys.contains(key)) {
                throw new InvalidProjectAnnotationException("Annotation payload contains unsupported fields");
            }
        }
    }

    private void addNotesIfPresent(Map<String, Object> normalizedPayload, Map<String, Object> payload) {
        if (!payload.containsKey(ProjectConstants.ANNOTATION_KEY_NOTES)) {
            return;
        }

        if (payload.get(ProjectConstants.ANNOTATION_KEY_NOTES) == null) {
            return;
        }

        String notes = trimOptionalStringPayload(payload.get(ProjectConstants.ANNOTATION_KEY_NOTES),
                "Annotation notes must be text");
        if (notes != null) {
            normalizedPayload.put(ProjectConstants.ANNOTATION_KEY_NOTES, notes);
        }
    }

    private String normalizeAllowedLabel(Object labelValue, Map<String, String> allowedLabelsByKey) {
        String label = trimStringPayload(labelValue, "Annotation label is required");
        String allowedLabel = allowedLabelsByKey.get(label.toLowerCase(Locale.ROOT));
        if (allowedLabel == null) {
            throw new InvalidProjectAnnotationException("Annotation label does not belong to this project");
        }
        return allowedLabel;
    }

    private String trimStringPayload(Object value, String message) {
        String normalizedValue = StringUtils.trimToNull(value instanceof String stringValue ? stringValue : null);
        if (normalizedValue == null) {
            throw new InvalidProjectAnnotationException(message);
        }
        return normalizedValue;
    }

    private String trimOptionalStringPayload(Object value, String message) {
        if (!(value instanceof String stringValue)) {
            throw new InvalidProjectAnnotationException(message);
        }
        return StringUtils.trimToNull(stringValue);
    }

    private Map<String, String> buildAllowedLabelsByKey(List<Label> labels) {
        Map<String, String> allowedLabelsByKey = new LinkedHashMap<>();
        for (Label label : labels) {
            String labelName = StringUtils.trimToNull(label.getName());
            if (labelName == null) {
                continue;
            }
            allowedLabelsByKey.putIfAbsent(labelName.toLowerCase(Locale.ROOT), labelName);
        }
        return allowedLabelsByKey;
    }

    private Object requirePayload(Object annotationPayload) {
        if (annotationPayload == null) {
            throw new InvalidProjectAnnotationException("Annotation payload is required");
        }
        if (annotationPayload instanceof String stringValue && stringValue.trim().isEmpty()) {
            throw new InvalidProjectAnnotationException("Annotation payload is required");
        }
        if (annotationPayload instanceof List<?> listValue && listValue.isEmpty()) {
            throw new InvalidProjectAnnotationException("Annotation payload is required");
        }
        if (annotationPayload instanceof Map<?, ?> mapValue && mapValue.isEmpty()) {
            throw new InvalidProjectAnnotationException("Annotation payload is required");
        }
        return annotationPayload;
    }
}
