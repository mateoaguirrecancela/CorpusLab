package es.udc.fic.corpuslab.modules.project.iaa.domain.calculators.ner;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import es.udc.fic.corpuslab.modules.project.shared.enums.MetricType;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.iaa.domain.calculators.pairwise.AnnotatorAnnotationVector;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaCalculationContext;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaResult;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.AnnotationUnitKey;
import es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.AnnotationDataTransformer;
import es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.ner.NerSpan;
import es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.ner.NerSpanAnnotationData;
import es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.ner.NerSpanAnnotationUnit;

class SpanOverlapF1CalculatorTest {

    private static final AnnotationUnitKey UNIT = new AnnotationUnitKey(1L, 0);
    private static final IaaCalculationContext NER_CONTEXT = new IaaCalculationContext(
            1L,
            ProjectType.NER,
            List.of(),
            List.of(),
            List.of(),
            Map.of());

    @Test
    void calculateShouldUseCharacterOverlapByDefault() {
        NerSpanAnnotationData data = data(
                unit(SpanOverlapUnit.CHARACTER, "abcdefgh", new NerSpan("PER", "abcd", 0, 4)),
                unit(SpanOverlapUnit.CHARACTER, "abcdefgh", new NerSpan("PER", "cdef", 2, 6)));

        IaaResult result = new SpanOverlapF1Calculator(new StubTransformer(data)).calculate(NER_CONTEXT);

        assertThat(result.calculable()).isTrue();
        assertThat(result.value()).isCloseTo(0.5, org.assertj.core.data.Offset.offset(1.0e-12));
    }

    @Test
    void calculateShouldUseWordOverlapWhenConfiguredInData() {
        String sourceText = "John Smith works";
        NerSpanAnnotationData data = data(
                unit(SpanOverlapUnit.WORD, sourceText, new NerSpan("PER", "John Smith", 0, 10)),
                unit(SpanOverlapUnit.WORD, sourceText, new NerSpan("PER", "Smith", 5, 10)));

        IaaResult result = new SpanOverlapF1Calculator(new StubTransformer(data)).calculate(NER_CONTEXT);

        assertThat(result.calculable()).isTrue();
        assertThat(result.value()).isCloseTo(2.0 / 3.0, org.assertj.core.data.Offset.offset(1.0e-12));
    }

    @Test
    void calculateShouldSkipUnitsMissingOnOneSide() {
        AnnotationUnitKey secondUnit = new AnnotationUnitKey(1L, 1);
        NerSpanAnnotationUnit leftUnit = unit(SpanOverlapUnit.CHARACTER, "abcd", new NerSpan("PER", "abcd", 0, 4));
        NerSpanAnnotationUnit rightUnit = unit(SpanOverlapUnit.CHARACTER, "abcd", new NerSpan("PER", "abcd", 0, 4));
        NerSpanAnnotationData data = new NerSpanAnnotationData(
                ProjectType.NER,
                List.of(
                        new AnnotatorAnnotationVector<>(1L, Map.of(UNIT, leftUnit, secondUnit, leftUnit)),
                        new AnnotatorAnnotationVector<>(2L, Map.of(UNIT, rightUnit))));

        IaaResult result = new SpanOverlapF1Calculator(new StubTransformer(data)).calculate(NER_CONTEXT);

        assertThat(result.calculable()).isTrue();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pairs = (List<Map<String, Object>>) result.details().get("pairs");
        assertThat(pairs).hasSize(1);
        assertThat(pairs.getFirst()).containsEntry("sharedUnitCount", 1);
    }

    @Test
    void calculateShouldBeNotCalculableWhenOneSideHasNoSpans() {
        NerSpanAnnotationData data = data(
                unit(SpanOverlapUnit.CHARACTER, "abcd"),
                unit(SpanOverlapUnit.CHARACTER, "abcd", new NerSpan("PER", "abcd", 0, 4)));

        IaaResult result = new SpanOverlapF1Calculator(new StubTransformer(data)).calculate(NER_CONTEXT);

        assertThat(result.calculable()).isFalse();
    }

    @Test
    void calculateShouldReturnZeroWhenSpansDoNotOverlap() {
        NerSpanAnnotationData data = data(
                unit(SpanOverlapUnit.CHARACTER, "abcdefgh", new NerSpan("PER", "ab", 0, 2)),
                unit(SpanOverlapUnit.CHARACTER, "abcdefgh", new NerSpan("PER", "gh", 6, 8)));

        IaaResult result = new SpanOverlapF1Calculator(new StubTransformer(data)).calculate(NER_CONTEXT);

        assertThat(result.calculable()).isTrue();
        assertThat(result.value()).isEqualTo(0.0);
    }

    @Test
    void calculateShouldGreedilyMatchMultipleOverlappingSpans() {
        NerSpanAnnotationUnit left = unit(
                SpanOverlapUnit.CHARACTER,
                "abcdefgh",
                new NerSpan("PER", "abcd", 0, 4),
                new NerSpan("PER", "efgh", 4, 8));
        NerSpanAnnotationUnit right = unit(
                SpanOverlapUnit.CHARACTER,
                "abcdefgh",
                new NerSpan("PER", "abcdef", 0, 6));

        NerSpanAnnotationData data = data(left, right);

        IaaResult result = new SpanOverlapF1Calculator(new StubTransformer(data)).calculate(NER_CONTEXT);

        assertThat(result.calculable()).isTrue();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pairs = (List<Map<String, Object>>) result.details().get("pairs");
        @SuppressWarnings("unchecked")
        Map<String, Object> pairMetricDetails = (Map<String, Object>) pairs.getFirst().get("details");
        assertThat((Double) pairMetricDetails.get("overlapUnits")).isEqualTo(4.0);
    }

    @Test
    void calculateShouldCountWordsAndIgnorePunctuationOnlyTokens() {
        String sourceText = "John, Smith!";
        NerSpanAnnotationUnit left = unit(SpanOverlapUnit.WORD, sourceText, new NerSpan("PER", "John, Smith!", 0, 12));
        NerSpanAnnotationUnit right = unit(SpanOverlapUnit.WORD, sourceText, new NerSpan("PER", "John, Smith!", 0, 12));

        IaaResult result = new SpanOverlapF1Calculator(new StubTransformer(data(left, right))).calculate(NER_CONTEXT);

        assertThat(result.calculable()).isTrue();
        assertThat(result.value()).isEqualTo(1.0);
    }

    @Test
    void calculateShouldFallBackToSpanTextWhenOffsetsExceedSourceTextLength() {
        NerSpan outOfBoundsSpan = new NerSpan("PER", "ghost", 0, 5);
        NerSpanAnnotationUnit left = unit(SpanOverlapUnit.WORD, "", outOfBoundsSpan);
        NerSpanAnnotationUnit right = unit(SpanOverlapUnit.WORD, "", outOfBoundsSpan);

        IaaResult result = new SpanOverlapF1Calculator(new StubTransformer(data(left, right))).calculate(NER_CONTEXT);

        assertThat(result.calculable()).isTrue();
        assertThat(result.value()).isEqualTo(1.0);
    }

    private static NerSpanAnnotationData data(NerSpanAnnotationUnit left, NerSpanAnnotationUnit right) {
        return new NerSpanAnnotationData(
                ProjectType.NER,
                List.of(
                        new AnnotatorAnnotationVector<>(1L, Map.of(UNIT, left)),
                        new AnnotatorAnnotationVector<>(2L, Map.of(UNIT, right))));
    }

    private static NerSpanAnnotationUnit unit(SpanOverlapUnit overlapUnit, String sourceText, NerSpan... spans) {
        return new NerSpanAnnotationUnit(List.of(spans), sourceText, overlapUnit);
    }

    private record StubTransformer(NerSpanAnnotationData data)
            implements AnnotationDataTransformer<NerSpanAnnotationData> {

        @Override
        public Set<ProjectType> supportedProjectTypes() {
            return Set.of(ProjectType.NER);
        }

        @Override
        public Set<MetricType> supportedMetricTypes() {
            return Set.of(MetricType.SPAN_OVERLAP_F1);
        }

        @Override
        public NerSpanAnnotationData transform(IaaCalculationContext context) {
            return data;
        }
    }
}
