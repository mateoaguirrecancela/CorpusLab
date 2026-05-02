package es.udc.fic.corpuslab.modules.project.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import es.udc.fic.corpuslab.common.utils.StringUtils;
import es.udc.fic.corpuslab.modules.project.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.exceptions.InvalidProjectDatasetException;

public class ProjectAnnotationUtils {

    private ProjectAnnotationUtils() {
    }

    public static Map<String, Object> normalizeNerAnnotationPayload(Object annotationPayload) {
        if (!(annotationPayload instanceof Map<?, ?> rawPayload)) {
            throw new InvalidProjectDatasetException("NER annotation payload must be an object with entities");
        }

        Map<String, Object> payload = toMutableStringObjectMap(rawPayload);
        Object rawEntities = payload.get(ProjectConstants.NER_ANNOTATION_KEY_ENTITIES);

        if (!(rawEntities instanceof List<?> entitiesList) || entitiesList.isEmpty()) {
            throw new InvalidProjectDatasetException("NER annotation requires at least one entity");
        }

        List<Map<String, Object>> normalizedEntities = new ArrayList<>();
        Set<String> seenOffsets = new HashSet<>();

        for (Object rawEntity : entitiesList) {
            if (!(rawEntity instanceof Map<?, ?> rawEntityMap)) {
                throw new InvalidProjectDatasetException("Each NER entity must be an object");
            }

            Map<String, Object> entity = toMutableStringObjectMap(rawEntityMap);

            String label = trimStringValue(entity.get(ProjectConstants.NER_ANNOTATION_KEY_LABEL));
            String text = trimStringValue(entity.get(ProjectConstants.NER_ANNOTATION_KEY_TEXT));
            Integer startOffset = parseOffsetValue(entity.get(ProjectConstants.NER_ANNOTATION_KEY_START_OFFSET));
            Integer endOffset = parseOffsetValue(entity.get(ProjectConstants.NER_ANNOTATION_KEY_END_OFFSET));

            if (label == null || text == null || startOffset == null || endOffset == null) {
                throw new InvalidProjectDatasetException(
                        "Each NER entity requires label, text, startOffset and endOffset");
            }

            if (startOffset < 0 || endOffset <= startOffset) {
                throw new InvalidProjectDatasetException("NER entity offsets are invalid");
            }

            if (endOffset - startOffset != text.length()) {
                throw new InvalidProjectDatasetException("NER entity offsets must match selected text length");
            }

            String key = startOffset + ":" + endOffset + ":" + label.toLowerCase();
            if (!seenOffsets.add(key)) {
                continue;
            }

            Map<String, Object> normalizedEntity = new LinkedHashMap<>();
            normalizedEntity.put(ProjectConstants.NER_ANNOTATION_KEY_LABEL, label);
            normalizedEntity.put(ProjectConstants.NER_ANNOTATION_KEY_TEXT, text);
            normalizedEntity.put(ProjectConstants.NER_ANNOTATION_KEY_START_OFFSET, startOffset);
            normalizedEntity.put(ProjectConstants.NER_ANNOTATION_KEY_END_OFFSET, endOffset);
            normalizedEntities.add(normalizedEntity);
        }

        if (normalizedEntities.isEmpty()) {
            throw new InvalidProjectDatasetException("NER annotation requires at least one entity");
        }

        Map<String, Object> normalizedPayload = new LinkedHashMap<>();
        normalizedPayload.put(ProjectConstants.NER_ANNOTATION_KEY_ENTITIES, normalizedEntities);

        String notes = trimStringValue(payload.get(ProjectConstants.ANNOTATION_KEY_NOTES));
        if (notes != null) {
            normalizedPayload.put(ProjectConstants.ANNOTATION_KEY_NOTES, notes);
        }

        return normalizedPayload;
    }

    public static String trimStringValue(Object value) {
        if (!(value instanceof String stringValue)) {
            return null;
        }

        return StringUtils.trimToNull(stringValue);
    }

    public static Integer parseOffsetValue(Object value) {
        if (value instanceof Number numberValue) {
            double rawValue = numberValue.doubleValue();
            if (!Double.isFinite(rawValue) || rawValue % 1 != 0) {
                return null;
            }

            return (int) rawValue;
        }

        if (value instanceof String stringValue) {
            String normalizedValue = stringValue.trim();
            if (normalizedValue.isEmpty()) {
                return null;
            }

            try {
                return Integer.parseInt(normalizedValue);
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        return null;
    }

    public static Map<String, Object> normalizeAnnotationAsMap(Object annotationPayload) {
        if (annotationPayload instanceof Map<?, ?> payloadMap) {
            return toMutableStringObjectMap(payloadMap);
        }

        Map<String, Object> wrappedPayload = new LinkedHashMap<>();
        wrappedPayload.put(ProjectConstants.ANNOTATION_WRAPPED_VALUE_KEY, annotationPayload);
        return wrappedPayload;
    }

    public static Map<String, Object> toMutableStringObjectMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() != null) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return result;
    }

    public static boolean hasAnnotationPayload(Object annotationPayload) {
        if (annotationPayload == null) {
            return false;
        }

        if (annotationPayload instanceof String stringValue) {
            return !stringValue.isBlank();
        }

        if (annotationPayload instanceof Map<?, ?> mapValue) {
            return !mapValue.isEmpty();
        }

        if (annotationPayload instanceof List<?> listValue) {
            return !listValue.isEmpty();
        }

        return true;
    }

    public static ProjectProgressSnapshot buildProjectProgressSnapshot(
            List<ProjectParticipant> participants,
            List<DatasetItem> datasetItems,
            Map<Long, Map<Long, Map<Integer, Object>>> annotationLookup) {
        Map<Long, Long> completedStepsByUser = new LinkedHashMap<>();
        for (ProjectParticipant participant : participants) {
            completedStepsByUser.put(participant.getUser().getId(), 0L);
        }

        long totalSteps = 0L;

        for (DatasetItem datasetItem : datasetItems) {
            ProjectDatasetUtils.DatasetStepDefinition stepDefinition = ProjectDatasetUtils.resolveStepDefinition(datasetItem);
            int itemTotalSteps = stepDefinition.totalSteps();
            totalSteps += itemTotalSteps;

            if (itemTotalSteps <= 0) {
                continue;
            }

            Map<Long, Map<Integer, Object>> annotationsByUser = annotationLookup
                    .getOrDefault(datasetItem.getId(), Map.of());

            for (Long userId : completedStepsByUser.keySet()) {
                long itemCompletedSteps = countCompletedSteps(annotationsByUser.get(userId), itemTotalSteps);
                completedStepsByUser.put(userId, completedStepsByUser.get(userId) + itemCompletedSteps);
            }
        }

        Map<Long, Integer> completionPercentageByUser = new LinkedHashMap<>();
        for (Map.Entry<Long, Long> entry : completedStepsByUser.entrySet()) {
            completionPercentageByUser.put(entry.getKey(), toPercentage(entry.getValue(), totalSteps));
        }

        long totalCompletedSteps = completedStepsByUser.values().stream()
                .mapToLong(Long::longValue)
                .sum();
        long totalPossibleSteps = totalSteps * completedStepsByUser.size();

        return new ProjectProgressSnapshot(
                totalSteps,
                toPercentage(totalCompletedSteps, totalPossibleSteps),
                completedStepsByUser,
                completionPercentageByUser);
    }

    public static long countCompletedSteps(Map<Integer, Object> steps, int maxSteps) {
        if (steps == null || steps.isEmpty()) {
            return 0L;
        }

        Set<Integer> validStepIndexes = new HashSet<>();
        for (Map.Entry<Integer, Object> stepEntry : steps.entrySet()) {
            Integer stepIndex = stepEntry.getKey();
            if (stepIndex == null || stepIndex < 0 || stepIndex >= maxSteps) {
                continue;
            }

            if (!hasAnnotationPayload(stepEntry.getValue())) {
                continue;
            }

            validStepIndexes.add(stepIndex);
        }

        return validStepIndexes.size();
    }

    public static int toPercentage(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0;
        }

        long boundedNumerator = Math.max(0L, Math.min(numerator, denominator));
        return (int) Math.round((boundedNumerator * 100.0) / denominator);
    }

    public record ProjectProgressSnapshot(
            long totalSteps,
            int projectCompletionPercentage,
            Map<Long, Long> completedStepsByUser,
            Map<Long, Integer> completionPercentageByUser) {

        public long completedStepsForUser(Long userId) {
            return completedStepsByUser.getOrDefault(userId, 0L);
        }

        public int completionPercentageForUser(Long userId) {
            return completionPercentageByUser.getOrDefault(userId, 0);
        }
    }

    public record ExportStepRow(
            DatasetItem datasetItem,
            int stepIndex,
            String preview,
            Map<String, String> rowValues,
            Map<Long, Object> annotationsByUser) {
    }

    public record AnnotatorExportColumn(
            Long userId,
            String annotationHeader,
            String commentHeader,
            boolean hasAnnotation,
            boolean hasComment) {
    }
}
