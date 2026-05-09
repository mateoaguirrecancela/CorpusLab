package es.udc.fic.corpuslab.modules.project.iaa.calculators.xrr;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import es.udc.fic.corpuslab.modules.project.enums.IaaResultStatus;
import es.udc.fic.corpuslab.modules.project.enums.MetricType;
import es.udc.fic.corpuslab.modules.project.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.iaa.context.AnnotationCalculationContext;
import es.udc.fic.corpuslab.modules.project.iaa.dtos.IaaResult;
import es.udc.fic.corpuslab.modules.project.iaa.transformers.AnnotationDataTransformer;
import es.udc.fic.corpuslab.modules.project.iaa.transformers.xrr.XrrAnnotationData;

class XrrCalculatorTest {

    private static final AnnotationCalculationContext SIMPLE_CONTEXT = new AnnotationCalculationContext(
            1L,
            ProjectType.TEXT_CLASSIFICATION_SIMPLE,
            List.of(),
            List.of(),
            List.of(),
            Map.of());

    @Test
    void calculateShouldReturnNormalizedCrossKappa() {
        XrrAnnotationData data = new XrrAnnotationData(
                ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                List.of(
                        List.of("0", "0", "0"),
                        List.of("0", "1", "1"),
                        List.of("1", "1", "1")),
                List.of(
                        List.of("0", "0", "0"),
                        List.of("0", "1", "1"),
                        List.of("1", "1", "1")),
                List.of("human1", "human2", "human3"),
                List.of("llm1", "llm2", "llm3"),
                3,
                0);

        IaaResult result = new XrrCalculator(new StubTransformer(data)).calculate(SIMPLE_CONTEXT);

        assertThat(result.calculable()).isTrue();
        assertThat(result.value()).isCloseTo(7.0 / 6.0, org.assertj.core.data.Offset.offset(1.0e-12));
        assertThat(result.details())
                .containsEntry("crossKappa", 0.7)
                .containsEntry("groupXIrr", 0.6)
                .containsEntry("groupYIrr", 0.6);
    }

    @Test
    void calculateShouldReturnNaNWhenIrrDenominatorIsNotPositive() {
        XrrAnnotationData data = new XrrAnnotationData(
                ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                List.of(
                        List.of("0", "1"),
                        List.of("0", "1")),
                List.of(
                        List.of("0", "1"),
                        List.of("0", "1")),
                List.of("human1", "human2"),
                List.of("llm1", "llm2"),
                2,
                0);

        IaaResult result = new XrrCalculator(new StubTransformer(data)).calculate(SIMPLE_CONTEXT);

        assertThat(result.calculable()).isFalse();
        assertThat(result.value()).isNaN();
        assertThat(result.status()).isEqualTo(IaaResultStatus.UNDEFINED);
        assertThat(result.details()).containsEntry("groupXIrr", 0.0);
    }

    @Test
    void calculateShouldReturnInsufficientItemsWhenNoCompleteRowsExist() {
        XrrAnnotationData data = new XrrAnnotationData(
                ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                List.of(),
                List.of(),
                List.of("human1"),
                List.of("llm1"),
                4,
                4);

        IaaResult result = new XrrCalculator(new StubTransformer(data)).calculate(SIMPLE_CONTEXT);

        assertThat(result.calculable()).isFalse();
        assertThat(result.value()).isNaN();
        assertThat(result.status()).isEqualTo(IaaResultStatus.INSUFFICIENT_ITEMS);
        assertThat(result.details()).containsEntry("missingCompleteUnits", 4);
    }

    @Test
    void calculateShouldMarkResultAsProvisionalWhenInactiveSourcesWereIgnored() {
        XrrAnnotationData data = new XrrAnnotationData(
                ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                List.of(
                        List.of("A"),
                        List.of("B")),
                List.of(
                        List.of("A"),
                        List.of("B")),
                List.of("human1"),
                List.of("llm1"),
                2,
                0,
                2,
                2);

        IaaResult result = new XrrCalculator(new StubTransformer(data)).calculate(SIMPLE_CONTEXT);

        assertThat(result.calculable()).isTrue();
        assertThat(result.annotatorCount()).isEqualTo(2);
        assertThat(result.details())
                .containsEntry("activeAnnotators", 2)
                .containsEntry("inactiveAnnotators", 2)
                .containsEntry("provisional", true);
    }

    @Test
    void calculateShouldReportMissingRaterGroups() {
        XrrAnnotationData data = new XrrAnnotationData(
                ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                List.of(),
                List.of(),
                List.of("human1"),
                List.of(),
                4,
                0);

        IaaResult result = new XrrCalculator(new StubTransformer(data)).calculate(SIMPLE_CONTEXT);

        assertThat(result.calculable()).isFalse();
        assertThat(result.status()).isEqualTo(IaaResultStatus.INSUFFICIENT_ANNOTATORS);
        assertThat(result.details()).containsEntry("missingRaterGroups", 1);
    }

    private record StubTransformer(XrrAnnotationData data)
            implements AnnotationDataTransformer<XrrAnnotationData> {

        @Override
        public Set<ProjectType> supportedProjectTypes() {
            return Set.of(ProjectType.TEXT_CLASSIFICATION_SIMPLE);
        }

        @Override
        public Set<MetricType> supportedMetricTypes() {
            return Set.of(MetricType.XRR);
        }

        @Override
        public XrrAnnotationData transform(AnnotationCalculationContext context) {
            return data;
        }
    }
}
