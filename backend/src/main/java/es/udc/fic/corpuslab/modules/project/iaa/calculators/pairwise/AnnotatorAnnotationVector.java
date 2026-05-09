package es.udc.fic.corpuslab.modules.project.iaa.calculators.pairwise;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record AnnotatorAnnotationVector<K, V>(
        Long annotatorId,
        Map<K, V> annotationsByUnit) {

    public AnnotatorAnnotationVector {
        Objects.requireNonNull(annotatorId, "annotatorId is required");
        annotationsByUnit = immutableNonNullEntries(annotationsByUnit);
    }

    public Set<K> unitKeys() {
        return annotationsByUnit.keySet();
    }

    public V annotationFor(K unitKey) {
        return annotationsByUnit.get(unitKey);
    }

    public boolean hasAnnotationFor(K unitKey) {
        return annotationsByUnit.containsKey(unitKey);
    }

    private static <K, V> Map<K, V> immutableNonNullEntries(Map<K, V> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }

        Map<K, V> normalized = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key != null && value != null) {
                normalized.put(key, value);
            }
        });

        if (normalized.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(normalized);
    }
}
