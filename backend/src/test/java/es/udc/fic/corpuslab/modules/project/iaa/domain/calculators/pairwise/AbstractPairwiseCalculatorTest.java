package es.udc.fic.corpuslab.modules.project.iaa.domain.calculators.pairwise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import es.udc.fic.corpuslab.modules.project.shared.enums.IaaResultStatus;
import es.udc.fic.corpuslab.modules.project.shared.enums.MetricType;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.iaa.domain.exceptions.UnsupportedIaaMetricException;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaCalculationContext;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaResult;
import es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.AnnotationDataTransformer;

class AbstractPairwiseCalculatorTest {

    private static final IaaCalculationContext SIMPLE_CONTEXT = new IaaCalculationContext(
            1L,
            ProjectType.TEXT_CLASSIFICATION_SIMPLE,
            List.of(),
            List.of(),
            List.of(),
            Map.of());

    @Test
    void calculateShouldReturnArithmeticMeanAcrossAllUniqueAnnotatorPairs() {
        TestPairwiseData data = data(
                vector(1L, Map.of("a", 1, "b", 1)),
                vector(2L, Map.of("a", 1, "b", 2, "c", 3)),
                vector(3L, Map.of("a", 2, "d", 4)));

        IaaResult result = new SharedUnitCountCalculator(data).calculate(SIMPLE_CONTEXT);

        assertThat(result.calculable()).isTrue();
        assertThat(result.value()).isEqualTo(4.0 / 3.0);
        assertThat(result.pairCount()).isEqualTo(3);
        assertThat(result.itemCount()).isEqualTo(4);
        assertThat(result.details())
                .containsEntry("totalPairs", 3)
                .containsEntry("overlappingPairs", 3)
                .containsEntry("nonOverlappingPairs", 0)
                .containsEntry("calculablePairs", 3);
    }

    @Test
    void calculateShouldIgnorePairsWithoutSharedUnits() {
        TestPairwiseData data = data(
                vector(1L, Map.of("a", 1)),
                vector(2L, Map.of("a", 2)),
                vector(3L, Map.of("b", 3)));

        IaaResult result = new SharedUnitCountCalculator(data).calculate(SIMPLE_CONTEXT);

        assertThat(result.calculable()).isTrue();
        assertThat(result.value()).isEqualTo(1.0);
        assertThat(result.pairCount()).isEqualTo(1);
        assertThat(result.details())
                .containsEntry("totalPairs", 3)
                .containsEntry("overlappingPairs", 1)
                .containsEntry("nonOverlappingPairs", 2)
                .containsEntry("calculablePairs", 1);
    }

    @Test
    void calculateShouldReturnNoSharedItemsWhenNoPairOverlaps() {
        TestPairwiseData data = data(
                vector(1L, Map.of("a", 1)),
                vector(2L, Map.of("b", 2)));

        IaaResult result = new SharedUnitCountCalculator(data).calculate(SIMPLE_CONTEXT);

        assertThat(result.calculable()).isFalse();
        assertThat(result.value()).isNaN();
        assertThat(result.status()).isEqualTo(IaaResultStatus.NO_SHARED_ITEMS);
        assertThat(result.pairCount()).isZero();
        assertThat(result.details()).containsEntry("totalPairs", 1);
    }

    @Test
    void calculateShouldReturnNoValidPairsWhenOverlappingPairsAreNotMathematicallyCalculable() {
        TestPairwiseData data = data(
                vector(1L, Map.of("a", 1)),
                vector(2L, Map.of("a", 2)));

        IaaResult result = new NonCalculableCalculator(data).calculate(SIMPLE_CONTEXT);

        assertThat(result.calculable()).isFalse();
        assertThat(result.value()).isNaN();
        assertThat(result.status()).isEqualTo(IaaResultStatus.NO_VALID_PAIRS);
        assertThat(result.details())
                .containsEntry("overlappingPairs", 1)
                .containsEntry("nonCalculablePairs", 1);
    }

    @Test
    void calculateShouldReturnInsufficientAnnotatorsWhenLessThanTwoAnnotatorsExist() {
        TestPairwiseData data = data(vector(1L, Map.of("a", 1)));

        IaaResult result = new SharedUnitCountCalculator(data).calculate(SIMPLE_CONTEXT);

        assertThat(result.calculable()).isFalse();
        assertThat(result.value()).isNaN();
        assertThat(result.status()).isEqualTo(IaaResultStatus.INSUFFICIENT_ANNOTATORS);
    }

    @Test
    void calculateShouldThrowWhenProjectTypeIsUnsupportedByCalculator() {
        IaaCalculationContext nerContext = new IaaCalculationContext(
                1L,
                ProjectType.NER,
                List.of(),
                List.of(),
                List.of(),
                Map.of());

        assertThatThrownBy(() -> new SharedUnitCountCalculator(data()).calculate(nerContext))
                .isInstanceOf(UnsupportedIaaMetricException.class)
                .hasMessageContaining("not compatible");
    }

    @SafeVarargs
    private static TestPairwiseData data(AnnotatorAnnotationVector<String, Integer>... annotators) {
        return new TestPairwiseData(ProjectType.TEXT_CLASSIFICATION_SIMPLE, List.of(annotators));
    }

    private static AnnotatorAnnotationVector<String, Integer> vector(Long annotatorId, Map<String, Integer> units) {
        return new AnnotatorAnnotationVector<>(annotatorId, units);
    }

    private record TestPairwiseData(
            ProjectType projectType,
            List<AnnotatorAnnotationVector<String, Integer>> annotatorVectors)
            implements PairwiseAnnotationData<String, Integer> {
    }

    private static class TestTransformer implements AnnotationDataTransformer<TestPairwiseData> {

        private final TestPairwiseData data;

        private TestTransformer(TestPairwiseData data) {
            this.data = data;
        }

        @Override
        public Set<ProjectType> supportedProjectTypes() {
            return Set.of(ProjectType.TEXT_CLASSIFICATION_SIMPLE);
        }

        @Override
        public Set<MetricType> supportedMetricTypes() {
            return Set.of(MetricType.COHENS_KAPPA);
        }

        @Override
        public TestPairwiseData transform(IaaCalculationContext context) {
            return data;
        }
    }

    private static class SharedUnitCountCalculator
            extends AbstractPairwiseCalculator<String, Integer, TestPairwiseData> {

        private SharedUnitCountCalculator(TestPairwiseData data) {
            super(
                    MetricType.COHENS_KAPPA,
                    Set.of(ProjectType.TEXT_CLASSIFICATION_SIMPLE),
                    new TestTransformer(data));
        }

        @Override
        protected PairwiseMetricResult calculatePairMetric(PairwiseAnnotationPair<String, Integer> pair) {
            return PairwiseMetricResult.calculable(pair.sharedUnitCount());
        }
    }

    private static class NonCalculableCalculator
            extends AbstractPairwiseCalculator<String, Integer, TestPairwiseData> {

        private NonCalculableCalculator(TestPairwiseData data) {
            super(
                    MetricType.COHENS_KAPPA,
                    Set.of(ProjectType.TEXT_CLASSIFICATION_SIMPLE),
                    new TestTransformer(data));
        }

        @Override
        protected PairwiseMetricResult calculatePairMetric(PairwiseAnnotationPair<String, Integer> pair) {
            return PairwiseMetricResult.notCalculable("Denominator is zero");
        }
    }
}
