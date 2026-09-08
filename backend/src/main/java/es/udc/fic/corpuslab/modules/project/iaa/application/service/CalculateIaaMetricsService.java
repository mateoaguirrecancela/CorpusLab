package es.udc.fic.corpuslab.modules.project.iaa.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import es.udc.fic.corpuslab.modules.project.shared.enums.IaaResultStatus;
import es.udc.fic.corpuslab.modules.project.shared.enums.MetricType;
import es.udc.fic.corpuslab.modules.project.iaa.application.port.in.CalculateIaaMetricsCommand;
import es.udc.fic.corpuslab.modules.project.iaa.application.port.in.CalculateIaaMetricsResult;
import es.udc.fic.corpuslab.modules.project.iaa.application.port.in.CalculateIaaMetricsUseCase;
import es.udc.fic.corpuslab.modules.project.iaa.domain.calculators.IaaMetricCalculator;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaCalculationContext;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaResult;
import es.udc.fic.corpuslab.modules.project.iaa.domain.registry.MetricsRegistry;

public class CalculateIaaMetricsService implements CalculateIaaMetricsUseCase {

    private final MetricsRegistry metricsRegistry;

    public CalculateIaaMetricsService(MetricsRegistry metricsRegistry) {
        this.metricsRegistry = Objects.requireNonNull(metricsRegistry, "metricsRegistry is required");
    }

    @Override
    public CalculateIaaMetricsResult calculate(CalculateIaaMetricsCommand command) {
        IaaCalculationContext context = command.context();
        List<IaaResult> metrics = metricsRegistry.supportedMetrics(context.projectType())
                .stream()
                .sorted(Comparator.comparingInt(MetricType::ordinal))
                .map(metricType -> calculateMetric(metricType, context))
                .toList();

        return new CalculateIaaMetricsResult(metrics);
    }

    private IaaResult calculateMetric(MetricType metricType, IaaCalculationContext context) {
        try {
            IaaMetricCalculator calculator = metricsRegistry.getCalculator(context.projectType(), metricType);
            return calculator.calculate(context);
        } catch (RuntimeException ex) {
            return IaaResult.notCalculable(
                    metricType,
                    context.projectType(),
                    IaaResultStatus.UNDEFINED,
                    "Metric could not be calculated: " + ex.getMessage(),
                    context.annotators().size(),
                    context.datasetItems().size(),
                    0,
                    Map.of("errorType", ex.getClass().getSimpleName()));
        }
    }
}
