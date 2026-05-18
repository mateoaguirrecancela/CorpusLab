package es.udc.fic.corpuslab.modules.project.iaa.domain.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import es.udc.fic.corpuslab.modules.project.shared.enums.IaaResultStatus;
import es.udc.fic.corpuslab.modules.project.shared.enums.MetricType;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;

public record IaaResult(
        MetricType metricType,
        ProjectType projectType,
        double value,
        boolean calculable,
        IaaResultStatus status,
        String message,
        int annotatorCount,
        int itemCount,
        int pairCount,
        Map<String, Object> details) {

    public IaaResult {
        Objects.requireNonNull(metricType, "metricType is required");
        Objects.requireNonNull(projectType, "projectType is required");

        status = status == null ? inferStatus(calculable, value) : status;
        calculable = status == IaaResultStatus.CALCULABLE && Double.isFinite(value);
        if (!calculable && status == IaaResultStatus.CALCULABLE) {
            status = IaaResultStatus.UNDEFINED;
        }

        value = calculable ? value : Double.NaN;
        message = message == null ? "" : message;
        details = immutableCopy(details);
    }

    public static IaaResult calculable(
            MetricType metricType,
            ProjectType projectType,
            double value,
            int annotatorCount,
            int itemCount,
            int pairCount,
            Map<String, Object> details) {
        return new IaaResult(
                metricType,
                projectType,
                value,
                true,
                IaaResultStatus.CALCULABLE,
                "",
                annotatorCount,
                itemCount,
                pairCount,
                details);
    }

    public static IaaResult notCalculable(
            MetricType metricType,
            ProjectType projectType,
            IaaResultStatus status,
            String message,
            int annotatorCount,
            int itemCount,
            int pairCount,
            Map<String, Object> details) {
        if (status == IaaResultStatus.CALCULABLE) {
            throw new IllegalArgumentException("Non-calculable IAA results cannot use CALCULABLE status");
        }
        return new IaaResult(
                metricType,
                projectType,
                Double.NaN,
                false,
                status,
                message,
                annotatorCount,
                itemCount,
                pairCount,
                details);
    }

    private static IaaResultStatus inferStatus(boolean calculable, double value) {
        return calculable && Double.isFinite(value)
                ? IaaResultStatus.CALCULABLE
                : IaaResultStatus.UNDEFINED;
    }

    private static Map<String, Object> immutableCopy(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
