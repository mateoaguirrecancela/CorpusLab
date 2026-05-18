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
