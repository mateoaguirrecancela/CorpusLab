package es.udc.fic.corpuslab.modules.project.iaa.domain.registry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


import es.udc.fic.corpuslab.modules.project.shared.enums.MetricType;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.iaa.domain.exceptions.UnsupportedIaaMetricException;
import es.udc.fic.corpuslab.modules.project.iaa.domain.calculators.IaaMetricCalculator;

public class MetricsRegistry {

    private static final Map<ProjectType, Set<MetricType>> COMPATIBILITY_MATRIX = Map.of(
            ProjectType.TEXT_CLASSIFICATION_SIMPLE,
            Set.of(
                    MetricType.COHENS_KAPPA,
                    MetricType.KRIPPENDORFFS_ALPHA,
                    MetricType.FLEISS_KAPPA,
                    MetricType.XRR),
            ProjectType.TEXT_CLASSIFICATION_MULTILABEL,
            Set.of(
                    MetricType.COHENS_KAPPA,
                    MetricType.KRIPPENDORFFS_ALPHA,
                    MetricType.FLEISS_KAPPA,
                    MetricType.XRR),
            ProjectType.NER,
            Set.of(
                    MetricType.COHENS_KAPPA,
                    MetricType.KRIPPENDORFFS_ALPHA,
                    MetricType.FLEISS_KAPPA,
                    MetricType.XRR,
                    MetricType.SPAN_OVERLAP_F1),
            ProjectType.SEQ2SEQ,
            Set.of());

    private final Map<MetricStrategyKey, IaaMetricCalculator> calculatorsByKey;

    public MetricsRegistry(List<IaaMetricCalculator> calculators) {
        this.calculatorsByKey = registerCalculators(calculators == null ? List.of() : calculators);
    }

    public IaaMetricCalculator getCalculator(ProjectType projectType, MetricType metricType) {
        validateCompatible(projectType, metricType);

        IaaMetricCalculator calculator = calculatorsByKey.get(new MetricStrategyKey(projectType, metricType));
        if (calculator == null) {
            throw UnsupportedIaaMetricException.notRegistered(projectType, metricType);
        }
        return calculator;
    }

    public boolean isCompatible(ProjectType projectType, MetricType metricType) {
        Objects.requireNonNull(projectType, "projectType is required");
        Objects.requireNonNull(metricType, "metricType is required");
        return COMPATIBILITY_MATRIX.getOrDefault(projectType, Set.of()).contains(metricType);
    }

    public Set<MetricType> supportedMetrics(ProjectType projectType) {
        Objects.requireNonNull(projectType, "projectType is required");
        return COMPATIBILITY_MATRIX.getOrDefault(projectType, Set.of());
    }

    public Map<ProjectType, Set<MetricType>> compatibilityMatrix() {
        return COMPATIBILITY_MATRIX;
    }

    private void validateCompatible(ProjectType projectType, MetricType metricType) {
        if (!isCompatible(projectType, metricType)) {
            throw UnsupportedIaaMetricException.incompatible(projectType, metricType);
        }
    }

    private Map<MetricStrategyKey, IaaMetricCalculator> registerCalculators(List<IaaMetricCalculator> calculators) {
        Map<MetricStrategyKey, IaaMetricCalculator> registry = new LinkedHashMap<>();
        for (IaaMetricCalculator calculator : calculators) {
            Objects.requireNonNull(calculator, "calculator cannot be null");
            Objects.requireNonNull(calculator.metricType(), "calculator metricType is required");
            Objects.requireNonNull(calculator.supportedProjectTypes(), "calculator supportedProjectTypes is required");

            for (ProjectType projectType : calculator.supportedProjectTypes()) {
                if (!isCompatible(projectType, calculator.metricType())) {
                    throw new IllegalStateException(
                            "Calculator " + calculator.getClass().getName()
                                    + " declares unsupported pair "
                                    + projectType + " / " + calculator.metricType());
                }

                MetricStrategyKey key = new MetricStrategyKey(projectType, calculator.metricType());
                IaaMetricCalculator previous = registry.putIfAbsent(key, calculator);
                if (previous != null) {
                    throw new IllegalStateException(
                            "Duplicated IAA calculator for "
                                    + projectType + " / " + calculator.metricType());
                }
            }
        }
        return Map.copyOf(registry);
    }

    private record MetricStrategyKey(ProjectType projectType, MetricType metricType) {
    }
}
