package es.udc.fic.corpuslab.modules.project.iaa.domain.calculators.pairwise;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record PairwiseMetricResult(
        double value,
        boolean calculable,
        String message,
        Map<String, Object> details) {

    public PairwiseMetricResult {
        calculable = calculable && Double.isFinite(value);
        value = calculable ? value : Double.NaN;
        message = message == null ? "" : message;
        details = immutableCopy(details);
    }

    public static PairwiseMetricResult calculable(double value) {
        return calculable(value, Map.of());
    }

    public static PairwiseMetricResult calculable(double value, Map<String, Object> details) {
        return new PairwiseMetricResult(value, true, "", details);
    }

    public static PairwiseMetricResult notCalculable(String message) {
        return notCalculable(message, Map.of());
    }

    public static PairwiseMetricResult notCalculable(String message, Map<String, Object> details) {
        return new PairwiseMetricResult(Double.NaN, false, message, details);
    }

    private static Map<String, Object> immutableCopy(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
