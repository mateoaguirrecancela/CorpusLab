package es.udc.fic.corpuslab.modules.project.iaa.domain.transformers;

import java.util.LinkedHashMap;
import java.util.Map;

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
}
