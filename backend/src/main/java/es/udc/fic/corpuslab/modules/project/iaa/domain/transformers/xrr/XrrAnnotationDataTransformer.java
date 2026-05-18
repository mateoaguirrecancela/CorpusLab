package es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.xrr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import es.udc.fic.corpuslab.common.utils.StringUtils;
import es.udc.fic.corpuslab.modules.project.shared.enums.MetricType;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.AnnotationUnitKey;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaAnnotation;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaAnnotator;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaCalculationContext;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaDatasetItem;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaDatasetStep;
import es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.AnnotationDataTransformer;
import es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.IaaPayloadUtils;

public class XrrAnnotationDataTransformer implements AnnotationDataTransformer<XrrAnnotationData> {

    public static final String METADATA_KEY_GROUP_X_COLUMNS = "xrrGroupXColumns";
    public static final String METADATA_KEY_GROUP_Y_COLUMNS = "xrrGroupYColumns";
    public static final String METADATA_KEY_GROUP_X_ANNOTATOR_IDS = "xrrGroupXAnnotatorIds";
    public static final String METADATA_KEY_GROUP_Y_ANNOTATOR_IDS = "xrrGroupYAnnotatorIds";
    public static final String METADATA_KEY_HUMAN_LABEL_COLUMNS = "humanLabelColumns";
    public static final String METADATA_KEY_LLM_LABEL_COLUMNS = "llmLabelColumns";

    @Override
    public Set<ProjectType> supportedProjectTypes() {
        return Set.of(
                ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                ProjectType.TEXT_CLASSIFICATION_MULTILABEL,
                ProjectType.NER);
    }

    @Override
    public Set<MetricType> supportedMetricTypes() {
        return Set.of(MetricType.XRR);
    }

    @Override
    public XrrAnnotationData transform(IaaCalculationContext context) {
        List<AnnotationUnitKey> unitKeys = resolveUnitKeys(context);
        GroupValues rawGroupX = resolveGroupX(context, unitKeys);
        GroupValues rawGroupY = resolveGroupY(context, unitKeys);
        GroupValues groupX = withoutInactiveSources(rawGroupX, unitKeys);
        GroupValues groupY = withoutInactiveSources(rawGroupY, unitKeys);

        List<List<String>> groupXRows = new ArrayList<>();
        List<List<String>> groupYRows = new ArrayList<>();
        int skippedUnitCount = 0;

        for (AnnotationUnitKey unitKey : unitKeys) {
            List<String> groupXValues = groupX.valuesByUnit().get(unitKey);
            List<String> groupYValues = groupY.valuesByUnit().get(unitKey);
            if (isCompleteRow(groupXValues, groupX.sources().size())
                    && isCompleteRow(groupYValues, groupY.sources().size())) {
                groupXRows.add(groupXValues);
                groupYRows.add(groupYValues);
            } else {
                skippedUnitCount++;
            }
        }

        return new XrrAnnotationData(
                context.projectType(),
                groupXRows,
                groupYRows,
                groupX.sources(),
                groupY.sources(),
                unitKeys.size(),
                skippedUnitCount,
                rawGroupX.sources().size(),
                rawGroupY.sources().size());
    }

    private GroupValues resolveGroupX(IaaCalculationContext context, List<AnnotationUnitKey> unitKeys) {
        List<String> groupXColumns = metadataStringList(
                context.metadata(),
                METADATA_KEY_GROUP_X_COLUMNS,
                METADATA_KEY_HUMAN_LABEL_COLUMNS);
        if (!groupXColumns.isEmpty()) {
            return buildColumnGroup(context, unitKeys, groupXColumns);
        }

        boolean groupXAnnotatorsConfigured = context.metadata().containsKey(METADATA_KEY_GROUP_X_ANNOTATOR_IDS);
        Set<Long> annotatorIds = metadataLongSet(context.metadata(), METADATA_KEY_GROUP_X_ANNOTATOR_IDS);
        if (!groupXAnnotatorsConfigured && annotatorIds.isEmpty()) {
            annotatorIds = context.annotators().stream()
                    .filter(Objects::nonNull)
                    .map(IaaAnnotator::userId)
                    .filter(Objects::nonNull)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        }
        if (annotatorIds.isEmpty()) {
            annotatorIds = context.annotations().stream()
                    .filter(this::isUsableAnnotation)
                    .map(IaaAnnotation::annotatorId)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        }
        return buildAnnotationGroup(context, unitKeys, annotatorIds);
    }

    private GroupValues resolveGroupY(IaaCalculationContext context, List<AnnotationUnitKey> unitKeys) {
        List<String> groupYColumns = metadataStringList(
                context.metadata(),
                METADATA_KEY_GROUP_Y_COLUMNS,
                METADATA_KEY_LLM_LABEL_COLUMNS);
        if (!groupYColumns.isEmpty()) {
            return buildColumnGroup(context, unitKeys, groupYColumns);
        }

        Set<Long> annotatorIds = metadataLongSet(context.metadata(), METADATA_KEY_GROUP_Y_ANNOTATOR_IDS);
        return buildAnnotationGroup(context, unitKeys, annotatorIds);
    }

    private GroupValues buildAnnotationGroup(
            IaaCalculationContext context,
            List<AnnotationUnitKey> unitKeys,
            Set<Long> annotatorIds) {
        List<Long> orderedAnnotatorIds = annotatorIds.stream().toList();
        Map<AnnotationUnitKey, List<String>> valuesByUnit = initializeRows(unitKeys, orderedAnnotatorIds.size());

        Map<AnnotationUnitKey, Map<Long, String>> annotationValuesByUnit = new LinkedHashMap<>();
        for (IaaAnnotation annotation : context.annotations()) {
            if (!isUsableAnnotation(annotation) || !annotatorIds.contains(annotation.annotatorId())) {
                continue;
            }

            String value = normalizeAnnotationValue(context.projectType(), annotation.payload());
            if (value == null) {
                continue;
            }

            AnnotationUnitKey unitKey = new AnnotationUnitKey(
                    annotation.datasetItemId(),
                    annotation.stepIndex());
            annotationValuesByUnit
                    .computeIfAbsent(unitKey, ignored -> new LinkedHashMap<>())
                    .put(annotation.annotatorId(), value);
        }

        for (AnnotationUnitKey unitKey : unitKeys) {
            Map<Long, String> valuesForUnit = annotationValuesByUnit.getOrDefault(unitKey, Map.of());
            List<String> row = valuesByUnit.get(unitKey);
            for (int sourceIndex = 0; sourceIndex < orderedAnnotatorIds.size(); sourceIndex++) {
                row.set(sourceIndex, valuesForUnit.get(orderedAnnotatorIds.get(sourceIndex)));
            }
        }

        List<String> sources = orderedAnnotatorIds.stream()
                .map(id -> "annotator:" + id)
                .toList();
        return new GroupValues(sources, valuesByUnit);
    }

    private GroupValues buildColumnGroup(
            IaaCalculationContext context,
            List<AnnotationUnitKey> unitKeys,
            List<String> columns) {
        Map<AnnotationUnitKey, List<String>> valuesByUnit = initializeRows(unitKeys, columns.size());
        Map<AnnotationUnitKey, Map<String, String>> rowValuesByUnit = buildRowValuesByUnit(context);

        for (AnnotationUnitKey unitKey : unitKeys) {
            Map<String, String> rowValues = rowValuesByUnit.getOrDefault(unitKey, Map.of());
            List<String> row = valuesByUnit.get(unitKey);
            for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                row.set(columnIndex, normalizeColumnValue(rowValues, columns.get(columnIndex)));
            }
        }

        List<String> sources = columns.stream()
                .map(column -> "column:" + column)
                .toList();
        return new GroupValues(sources, valuesByUnit);
    }

    private Map<AnnotationUnitKey, List<String>> initializeRows(List<AnnotationUnitKey> unitKeys, int width) {
        Map<AnnotationUnitKey, List<String>> valuesByUnit = new LinkedHashMap<>();
        for (AnnotationUnitKey unitKey : unitKeys) {
            List<String> row = new ArrayList<>();
            for (int index = 0; index < width; index++) {
                row.add(null);
            }
            valuesByUnit.put(unitKey, row);
        }
        return valuesByUnit;
    }

    private GroupValues withoutInactiveSources(GroupValues groupValues, List<AnnotationUnitKey> unitKeys) {
        if (groupValues.sources().isEmpty()) {
            return groupValues;
        }

        List<Integer> activeSourceIndexes = new ArrayList<>();
        for (int sourceIndex = 0; sourceIndex < groupValues.sources().size(); sourceIndex++) {
            if (hasAnyValue(groupValues, unitKeys, sourceIndex)) {
                activeSourceIndexes.add(sourceIndex);
            }
        }

        if (activeSourceIndexes.size() == groupValues.sources().size()) {
            return groupValues;
        }

        List<String> activeSources = activeSourceIndexes.stream()
                .map(index -> groupValues.sources().get(index))
                .toList();
        Map<AnnotationUnitKey, List<String>> activeValuesByUnit = new LinkedHashMap<>();
        for (AnnotationUnitKey unitKey : unitKeys) {
            List<String> row = groupValues.valuesByUnit().getOrDefault(unitKey, List.of());
            List<String> activeRow = new ArrayList<>();
            for (Integer sourceIndex : activeSourceIndexes) {
                activeRow.add(sourceIndex < row.size() ? row.get(sourceIndex) : null);
            }
            activeValuesByUnit.put(unitKey, activeRow);
        }

        return new GroupValues(activeSources, activeValuesByUnit);
    }

    private boolean hasAnyValue(GroupValues groupValues, List<AnnotationUnitKey> unitKeys, int sourceIndex) {
        for (AnnotationUnitKey unitKey : unitKeys) {
            List<String> row = groupValues.valuesByUnit().get(unitKey);
            if (row != null && sourceIndex < row.size()) {
                String value = row.get(sourceIndex);
                if (value != null && !value.isBlank()) {
                    return true;
                }
            }
        }

        return false;
    }

    private List<AnnotationUnitKey> resolveUnitKeys(IaaCalculationContext context) {
        if (!context.datasetItems().isEmpty()) {
            List<AnnotationUnitKey> unitKeys = new ArrayList<>();
            for (IaaDatasetItem datasetItem : context.datasetItems()) {
                if (datasetItem == null || datasetItem.id() == null) {
                    continue;
                }

                for (IaaDatasetStep step : datasetItem.steps()) {
                    unitKeys.add(new AnnotationUnitKey(datasetItem.id(), step.stepIndex()));
                }
            }
            return List.copyOf(unitKeys);
        }

        return context.annotations().stream()
                .filter(this::isUsableAnnotation)
                .map(annotation -> new AnnotationUnitKey(annotation.datasetItemId(), annotation.stepIndex()))
                .distinct()
                .sorted(Comparator.comparing(AnnotationUnitKey::datasetItemId)
                        .thenComparing(AnnotationUnitKey::stepIndex))
                .toList();
    }

    private Map<AnnotationUnitKey, Map<String, String>> buildRowValuesByUnit(IaaCalculationContext context) {
        Map<AnnotationUnitKey, Map<String, String>> rowValuesByUnit = new LinkedHashMap<>();
        for (IaaDatasetItem datasetItem : context.datasetItems()) {
            if (datasetItem == null || datasetItem.id() == null) {
                continue;
            }

            for (IaaDatasetStep step : datasetItem.steps()) {
                Map<String, String> rowValues = step.rowValues();
                if (rowValues != null) {
                    rowValuesByUnit.put(new AnnotationUnitKey(datasetItem.id(), step.stepIndex()), rowValues);
                }
            }
        }
        return rowValuesByUnit;
    }

    private boolean isCompleteRow(List<String> row, int expectedSize) {
        return expectedSize > 0
                && row != null
                && row.size() == expectedSize
                && row.stream().allMatch(value -> value != null && !value.isBlank());
    }

    private String normalizeColumnValue(Map<String, String> rowValues, String column) {
        if (rowValues == null || rowValues.isEmpty() || column == null) {
            return null;
        }

        for (Map.Entry<String, String> entry : rowValues.entrySet()) {
            if (entry.getKey() != null && entry.getKey().trim().equalsIgnoreCase(column.trim())) {
                return StringUtils.trimToNull(entry.getValue());
            }
        }
        return null;
    }

    private boolean isUsableAnnotation(IaaAnnotation annotation) {
        return annotation != null
                && annotation.datasetItemId() != null
                && annotation.annotatorId() != null
                && annotation.stepIndex() != null
                && annotation.stepIndex() >= 0;
    }

    private String normalizeAnnotationValue(ProjectType projectType, Object payload) {
        if (payload == null) {
            return null;
        }

        if (projectType == ProjectType.NER) {
            return normalizeNerAnnotationValue(payload);
        }
        if (projectType == ProjectType.TEXT_CLASSIFICATION_MULTILABEL) {
            return normalizeMultiLabelAnnotationValue(payload);
        }
        return normalizeSingleLabelAnnotationValue(payload);
    }

    private String normalizeSingleLabelAnnotationValue(Object payload) {
        if (payload instanceof Map<?, ?> rawMap) {
            Map<String, Object> payloadMap = IaaPayloadUtils.toMutableStringObjectMap(rawMap);
            String label = normalizeScalar(payloadMap.get(IaaPayloadUtils.ANNOTATION_KEY_LABEL));
            if (label != null) {
                return label;
            }

            String binaryValue = normalizeScalar(payloadMap.get(IaaPayloadUtils.ANNOTATION_KEY_BINARY_VALUE));
            if (binaryValue != null) {
                return binaryValue;
            }

            return normalizeScalar(payloadMap.get(IaaPayloadUtils.ANNOTATION_WRAPPED_VALUE_KEY));
        }
        return normalizeScalar(payload);
    }

    private String normalizeMultiLabelAnnotationValue(Object payload) {
        Object rawLabels = payload;
        if (payload instanceof Map<?, ?> rawMap) {
            Map<String, Object> payloadMap = IaaPayloadUtils.toMutableStringObjectMap(rawMap);
            rawLabels = payloadMap.get(IaaPayloadUtils.ANNOTATION_KEY_LABELS);
            if (rawLabels == null) {
                rawLabels = payloadMap.get(IaaPayloadUtils.ANNOTATION_KEY_LABEL);
            }
            if (rawLabels == null) {
                rawLabels = payloadMap.get(IaaPayloadUtils.ANNOTATION_WRAPPED_VALUE_KEY);
            }
        }

        List<String> labels = normalizeStringList(rawLabels);
        return labels.isEmpty() ? null : String.join("||", labels);
    }

    private String normalizeNerAnnotationValue(Object payload) {
        if (!(payload instanceof Map<?, ?> rawMap)) {
            return null;
        }

        Map<String, Object> payloadMap = IaaPayloadUtils.toMutableStringObjectMap(rawMap);
        Object rawEntities = payloadMap.get(IaaPayloadUtils.NER_ANNOTATION_KEY_ENTITIES);
        if (!(rawEntities instanceof List<?> entities)) {
            return null;
        }

        List<String> normalizedEntities = new ArrayList<>();
        for (Object rawEntity : entities) {
            if (!(rawEntity instanceof Map<?, ?> rawEntityMap)) {
                continue;
            }

            Map<String, Object> entity = IaaPayloadUtils.toMutableStringObjectMap(rawEntityMap);
            String label = normalizeScalar(entity.get(IaaPayloadUtils.NER_ANNOTATION_KEY_LABEL));
            Integer startOffset = IaaPayloadUtils.parseOffsetValue(
                    entity.get(IaaPayloadUtils.NER_ANNOTATION_KEY_START_OFFSET));
            Integer endOffset = IaaPayloadUtils.parseOffsetValue(
                    entity.get(IaaPayloadUtils.NER_ANNOTATION_KEY_END_OFFSET));
            if (label != null && startOffset != null && endOffset != null && endOffset > startOffset) {
                normalizedEntities.add(startOffset + ":" + endOffset + ":" + label);
            }
        }

        return normalizedEntities.stream().sorted().reduce((left, right) -> left + "||" + right).orElse(null);
    }

    private List<String> normalizeStringList(Object rawValue) {
        List<String> values = new ArrayList<>();
        if (rawValue instanceof List<?> listValue) {
            for (Object entry : listValue) {
                String normalized = normalizeScalar(entry);
                if (normalized != null) {
                    values.add(normalized);
                }
            }
        } else if (rawValue instanceof String stringValue && stringValue.contains(",")) {
            for (String entry : stringValue.split(",")) {
                String normalized = normalizeScalar(entry);
                if (normalized != null) {
                    values.add(normalized);
                }
            }
        } else {
            String normalized = normalizeScalar(rawValue);
            if (normalized != null) {
                values.add(normalized);
            }
        }

        return values.stream().distinct().sorted(String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder()))
                .toList();
    }

    private String normalizeScalar(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean booleanValue) {
            return String.valueOf(booleanValue);
        }
        if (value instanceof Number numberValue && !Double.isFinite(numberValue.doubleValue())) {
            return null;
        }
        return StringUtils.trimToNull(String.valueOf(value));
    }

    private List<String> metadataStringList(Map<String, Object> metadata, String... keys) {
        for (String key : keys) {
            List<String> values = normalizeStringList(metadata.get(key));
            if (!values.isEmpty()) {
                return values;
            }
        }
        return List.of();
    }

    private Set<Long> metadataLongSet(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value == null) {
            return Set.of();
        }

        Collection<?> rawValues = value instanceof Collection<?> collection ? collection : List.of(value);
        Set<Long> values = new LinkedHashSet<>();
        for (Object rawValue : rawValues) {
            Long parsed = parseLong(rawValue);
            if (parsed != null) {
                values.add(parsed);
            }
        }
        return values;
    }

    private Long parseLong(Object value) {
        if (value instanceof Number numberValue) {
            return numberValue.longValue();
        }

        try {
            return value == null ? null : Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private record GroupValues(
            List<String> sources,
            Map<AnnotationUnitKey, List<String>> valuesByUnit) {
    }
}
