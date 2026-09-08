package es.udc.fic.corpuslab.modules.project.iaa.domain.calculators.classification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import es.udc.fic.corpuslab.modules.project.shared.enums.MetricType;
import es.udc.fic.corpuslab.modules.project.shared.enums.IaaResultStatus;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.iaa.domain.calculators.pairwise.AnnotatorAnnotationVector;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaCalculationContext;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaResult;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.AnnotationUnitKey;
import es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.AnnotationDataTransformer;
import es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.classification.NominalAnnotationData;
import es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.classification.NominalAnnotationDataTransformer;

class ClassificationCalculatorTest {

    private static final IaaCalculationContext SIMPLE_CONTEXT = new IaaCalculationContext(
            1L,
            ProjectType.TEXT_CLASSIFICATION_SIMPLE,
            List.of(),
            List.of(),
            List.of(),
            Map.of());

    private static final IaaCalculationContext NER_CONTEXT = new IaaCalculationContext(
            1L,
            ProjectType.NER,
            List.of(),
            List.of(),
            List.of(),
            Map.of());

    @Test
    void allClassificationCalculatorsShouldSupportSelectionOfFragments() {
        NominalAnnotationDataTransformer transformer = new NominalAnnotationDataTransformer();

        assertThat(new CohensKappaCalculator(transformer).supportedProjectTypes()).contains(ProjectType.NER);
        assertThat(new FleissKappaCalculator(transformer).supportedProjectTypes()).contains(ProjectType.NER);
        assertThat(new KrippendorffsAlphaCalculator(transformer).supportedProjectTypes()).contains(ProjectType.NER);
    }

    @Test
    void cohensKappaShouldCalculateKappaOverStrictMatchSpanCategories() {
        // Valor de referencia calculado a mano: dos anotadores sobre tres unidades,
        // cada una con un único span canonicalizado a "inicio:fin:etiqueta" (selección
        // de fragmentos). Po=2/3, Pe=(2/3*1/3)+(1/3*2/3)=4/9, kappa=(2/3-4/9)/(5/9)=0,4.
        NominalAnnotationData data = data(
                vector(1L, Map.of(unit(1), "0:5:PER", unit(2), "0:5:ORG", unit(3), "0:5:PER")),
                vector(2L, Map.of(unit(1), "0:5:PER", unit(2), "0:5:ORG", unit(3), "0:5:ORG")));

        IaaResult result = new CohensKappaCalculator(new StubTransformer(data)).calculate(NER_CONTEXT);

        assertThat(result.calculable()).isTrue();
        assertThat(result.value()).isCloseTo(0.4, org.assertj.core.data.Offset.offset(1.0e-12));
    }

    @Test
    void cohensKappaShouldCalculatePairwiseNominalKappa() {
        NominalAnnotationData data = data(
                vector(1L, Map.of(unit(1), "A", unit(2), "A", unit(3), "B")),
                vector(2L, Map.of(unit(1), "A", unit(2), "B", unit(3), "B")));

        IaaResult result = new CohensKappaCalculator(new StubTransformer(data)).calculate(SIMPLE_CONTEXT);

        assertThat(result.calculable()).isTrue();
        assertThat(result.value()).isCloseTo(0.4, org.assertj.core.data.Offset.offset(1.0e-12));
        assertThat(result.pairCount()).isEqualTo(1);
    }

    @Test
    void krippendorffsAlphaShouldBeOneForPerfectAgreementWithCategoryVariation() {
        NominalAnnotationData data = data(
                vector(1L, Map.of(unit(1), "A", unit(2), "B")),
                vector(2L, Map.of(unit(1), "A", unit(2), "B")),
                vector(3L, Map.of(unit(1), "A", unit(2), "B")));

        IaaResult result = new KrippendorffsAlphaCalculator(new StubTransformer(data)).calculate(SIMPLE_CONTEXT);

        assertThat(result.calculable()).isTrue();
        assertThat(result.value()).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1.0e-12));
        assertThat(result.pairCount()).isEqualTo(2);
    }

    @Test
    void fleissKappaShouldCalculateNominalMultiRaterKappa() {
        NominalAnnotationData data = data(
                vector(1L, Map.of(unit(1), "A", unit(2), "A", unit(3), "B")),
                vector(2L, Map.of(unit(1), "A", unit(2), "B", unit(3), "B")),
                vector(3L, Map.of(unit(1), "A", unit(2), "B", unit(3), "B")));

        IaaResult result = new FleissKappaCalculator(new StubTransformer(data)).calculate(SIMPLE_CONTEXT);

        assertThat(result.calculable()).isTrue();
        assertThat(result.value()).isCloseTo(0.55, org.assertj.core.data.Offset.offset(1.0e-12));
        assertThat(result.pairCount()).isEqualTo(3);
    }

    @Test
    void fleissKappaShouldIgnoreAnnotatorsWithoutAnnotations() {
        NominalAnnotationData data = data(
                vector(1L, Map.of(unit(1), "A", unit(2), "A")),
                vector(2L, Map.of(unit(1), "A", unit(2), "B")),
                vector(3L, Map.of()));

        IaaResult result = new FleissKappaCalculator(new StubTransformer(data)).calculate(SIMPLE_CONTEXT);

        assertThat(result.calculable()).isTrue();
        assertThat(result.annotatorCount()).isEqualTo(2);
        assertThat(result.details())
                .containsEntry("inactiveAnnotators", 1)
                .containsEntry("provisional", true);
    }

    @Test
    void fleissKappaShouldReportMissingRatingsWhenActiveAnnotatorsHaveNoCompleteItems() {
        NominalAnnotationData data = data(
                vector(1L, Map.of(unit(1), "A")),
                vector(2L, Map.of(unit(2), "B")));

        IaaResult result = new FleissKappaCalculator(new StubTransformer(data)).calculate(SIMPLE_CONTEXT);

        assertThat(result.calculable()).isFalse();
        assertThat(result.status()).isEqualTo(IaaResultStatus.INSUFFICIENT_ITEMS);
        assertThat(result.details()).containsEntry("missingRatings", 2);
    }

    @SafeVarargs
    private static NominalAnnotationData data(AnnotatorAnnotationVector<AnnotationUnitKey, String>... vectors) {
        Map<AnnotationUnitKey, List<String>> ratingsByUnit = new LinkedHashMap<>();
        Set<String> categories = new LinkedHashSet<>();
        for (AnnotatorAnnotationVector<AnnotationUnitKey, String> vector : vectors) {
            vector.annotationsByUnit().forEach((unitKey, rating) -> {
                ratingsByUnit.computeIfAbsent(unitKey, ignored -> new ArrayList<>()).add(rating);
                categories.add(rating);
            });
        }

        return new NominalAnnotationData(
                ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                List.of(vectors),
                ratingsByUnit,
                categories);
    }

    private static AnnotatorAnnotationVector<AnnotationUnitKey, String> vector(
            Long annotatorId,
            Map<AnnotationUnitKey, String> ratings) {
        return new AnnotatorAnnotationVector<>(annotatorId, ratings);
    }

    private static AnnotationUnitKey unit(int stepIndex) {
        return new AnnotationUnitKey(1L, stepIndex);
    }

    private record StubTransformer(NominalAnnotationData data)
            implements AnnotationDataTransformer<NominalAnnotationData> {

        @Override
        public Set<ProjectType> supportedProjectTypes() {
            return Set.of(ProjectType.TEXT_CLASSIFICATION_SIMPLE);
        }

        @Override
        public Set<MetricType> supportedMetricTypes() {
            return Set.of(MetricType.COHENS_KAPPA, MetricType.KRIPPENDORFFS_ALPHA, MetricType.FLEISS_KAPPA);
        }

        @Override
        public NominalAnnotationData transform(IaaCalculationContext context) {
            return data;
        }
    }
}
