package es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.ner;

import java.util.List;

import es.udc.fic.corpuslab.modules.project.iaa.domain.calculators.ner.SpanOverlapUnit;

public record NerSpanAnnotationUnit(
        List<NerSpan> spans,
        String sourceText,
        SpanOverlapUnit overlapUnit) {

    public NerSpanAnnotationUnit {
        spans = spans == null ? List.of() : List.copyOf(spans);
        sourceText = sourceText == null ? "" : sourceText;
        overlapUnit = overlapUnit == null ? SpanOverlapUnit.CHARACTER : overlapUnit;
    }
}
