package es.udc.fic.corpuslab.modules.project.iaa.factory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import es.udc.fic.corpuslab.modules.project.enums.IaaResultStatus;
import es.udc.fic.corpuslab.modules.project.enums.MetricType;
import es.udc.fic.corpuslab.modules.project.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.exceptions.UnsupportedMetricException;
import es.udc.fic.corpuslab.modules.project.iaa.calculators.IaaMetricCalculator;
import es.udc.fic.corpuslab.modules.project.iaa.context.AnnotationCalculationContext;
import es.udc.fic.corpuslab.modules.project.iaa.dtos.IaaResult;

class MetricsFactoryTest {

    @Test
    void supportedMetricsShouldReturnEmptySetForSeq2Seq() {
        MetricsFactory factory = new MetricsFactory(List.of());

        assertThat(factory.supportedMetrics(ProjectType.SEQ2SEQ)).isEmpty();
    }

    @Test
    void getCalculatorShouldFailFastWhenMetricIsIncompatibleWithProjectType() {
        MetricsFactory factory = new MetricsFactory(List.of());

        assertThatThrownBy(() -> factory.getCalculator(ProjectType.SEQ2SEQ, MetricType.COHENS_KAPPA))
                .isInstanceOf(UnsupportedMetricException.class)
                .hasMessageContaining("not compatible");
    }

    @Test
    void getCalculatorShouldReturnRegisteredStrategyForCompatiblePair() {
        StubCalculator calculator = new StubCalculator(
                MetricType.COHENS_KAPPA,
                Set.of(ProjectType.TEXT_CLASSIFICATION_SIMPLE));
        MetricsFactory factory = new MetricsFactory(List.of(calculator));

        IaaMetricCalculator resolved = factory.getCalculator(
                ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                MetricType.COHENS_KAPPA);

        assertThat(resolved).isSameAs(calculator);
    }

    @Test
    void constructorShouldRejectStrategyRegisteredForUnsupportedPair() {
        StubCalculator calculator = new StubCalculator(
                MetricType.SPAN_OVERLAP_F1,
                Set.of(ProjectType.TEXT_CLASSIFICATION_SIMPLE));

        assertThatThrownBy(() -> new MetricsFactory(List.of(calculator)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("declares unsupported pair");
    }

    private record StubCalculator(
            MetricType metricType,
            Set<ProjectType> supportedProjectTypes) implements IaaMetricCalculator {

        @Override
        public IaaResult calculate(AnnotationCalculationContext context) {
            return IaaResult.notCalculable(
                    metricType,
                    context.projectType(),
                    IaaResultStatus.UNDEFINED,
                    "Stub calculator",
                    0,
                    0,
                    0,
                    Map.of());
        }
    }
}
