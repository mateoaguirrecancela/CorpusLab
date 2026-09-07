package es.udc.fic.corpuslab.modules.project.iaa.domain.transformers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import es.udc.fic.corpuslab.common.utils.StringUtils;

public final class IaaPayloadUtils {

    public static final String ANNOTATION_KEY_BINARY_VALUE = "isExplanationCorrect";
    public static final String ANNOTATION_KEY_LABEL = "label";
    public static final String ANNOTATION_KEY_LABELS = "labels";
    public static final String ANNOTATION_WRAPPED_VALUE_KEY = "value";
    public static final String NER_ANNOTATION_KEY_ENTITIES = "entities";
    public static final String NER_ANNOTATION_KEY_LABEL = "label";
    public static final String NER_ANNOTATION_KEY_TEXT = "text";
    public static final String NER_ANNOTATION_KEY_START_OFFSET = "startOffset";
    public static final String NER_ANNOTATION_KEY_END_OFFSET = "endOffset";

    private IaaPayloadUtils() {
    }

    public static String valueAsString(Object value) {
        return value == null ? "" : String.valueOf(value);
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

    public static Map<String, Object> toMutableStringObjectMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() != null) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return result;
    }

    /**
     * Reduces the entities of a NER payload to a single canonical, order-independent
     * string ("start:end:label" per entity, joined with "||"), so that two annotations
     * can be compared as a single nominal category by strict span matching. Shared by
     * the xRR transformer and by the nominal transformer when applied to NER projects.
     */
    public static String canonicalizeNerEntities(Object payload) {
        if (!(payload instanceof Map<?, ?> rawMap)) {
            return null;
        }

        Map<String, Object> payloadMap = toMutableStringObjectMap(rawMap);
        Object rawEntities = payloadMap.get(NER_ANNOTATION_KEY_ENTITIES);
        if (!(rawEntities instanceof List<?> entities)) {
            return null;
        }

        List<String> normalizedEntities = new ArrayList<>();
        for (Object rawEntity : entities) {
            if (!(rawEntity instanceof Map<?, ?> rawEntityMap)) {
                continue;
            }

            Map<String, Object> entity = toMutableStringObjectMap(rawEntityMap);
            String label = normalizeEntityLabel(entity.get(NER_ANNOTATION_KEY_LABEL));
            Integer startOffset = parseOffsetValue(entity.get(NER_ANNOTATION_KEY_START_OFFSET));
            Integer endOffset = parseOffsetValue(entity.get(NER_ANNOTATION_KEY_END_OFFSET));
            if (label != null && startOffset != null && endOffset != null && endOffset > startOffset) {
                normalizedEntities.add(startOffset + ":" + endOffset + ":" + label);
            }
        }

        return normalizedEntities.stream().sorted().reduce((left, right) -> left + "||" + right).orElse(null);
    }

    private static String normalizeEntityLabel(Object value) {
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
}
