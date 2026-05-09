package es.udc.fic.corpuslab.modules.project.iaa.calculators.ner;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import es.udc.fic.corpuslab.modules.project.enums.MetricType;
import es.udc.fic.corpuslab.modules.project.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.iaa.calculators.pairwise.AbstractPairwiseCalculator;
import es.udc.fic.corpuslab.modules.project.iaa.calculators.pairwise.PairwiseAnnotationPair;
import es.udc.fic.corpuslab.modules.project.iaa.calculators.pairwise.PairwiseMetricResult;
import es.udc.fic.corpuslab.modules.project.iaa.model.AnnotationUnitKey;
import es.udc.fic.corpuslab.modules.project.iaa.transformers.AnnotationDataTransformer;
import es.udc.fic.corpuslab.modules.project.iaa.transformers.ner.NerSpan;
import es.udc.fic.corpuslab.modules.project.iaa.transformers.ner.NerSpanAnnotationData;
import es.udc.fic.corpuslab.modules.project.iaa.transformers.ner.NerSpanAnnotationDataTransformer;
import es.udc.fic.corpuslab.modules.project.iaa.transformers.ner.NerSpanAnnotationUnit;

@Component
public class SpanOverlapF1Calculator
        extends AbstractPairwiseCalculator<AnnotationUnitKey, NerSpanAnnotationUnit, NerSpanAnnotationData> {

    @Autowired
    public SpanOverlapF1Calculator(NerSpanAnnotationDataTransformer transformer) {
        this((AnnotationDataTransformer<NerSpanAnnotationData>) transformer);
    }

    SpanOverlapF1Calculator(AnnotationDataTransformer<NerSpanAnnotationData> transformer) {
        super(MetricType.SPAN_OVERLAP_F1, Set.of(ProjectType.NER), transformer);
    }

    @Override
    protected PairwiseMetricResult calculatePairMetric(
            PairwiseAnnotationPair<AnnotationUnitKey, NerSpanAnnotationUnit> pair) {
        double overlapUnits = 0.0;
        double leftUnits = 0.0;
        double rightUnits = 0.0;
        int sharedUnits = 0;

        for (AnnotationUnitKey unitKey : pair.sharedUnitKeys()) {
            NerSpanAnnotationUnit left = pair.leftAnnotationFor(unitKey);
            NerSpanAnnotationUnit right = pair.rightAnnotationFor(unitKey);
            if (left == null || right == null) {
                continue;
            }

            sharedUnits++;
            SpanOverlapUnit overlapUnit = left.overlapUnit();
            String sourceText = left.sourceText().isBlank() ? right.sourceText() : left.sourceText();
            leftUnits += totalSpanUnits(left.spans(), sourceText, overlapUnit);
            rightUnits += totalSpanUnits(right.spans(), sourceText, overlapUnit);
            overlapUnits += matchedOverlapUnits(left.spans(), right.spans(), sourceText, overlapUnit);
        }

        if (leftUnits <= 0.0 || rightUnits <= 0.0) {
            return PairwiseMetricResult.notCalculable(
                    "Span overlap F1 requires non-empty spans for both annotators",
                    Map.of("sharedUnits", sharedUnits));
        }

        double precision = overlapUnits / leftUnits;
        double recall = overlapUnits / rightUnits;
        double denominator = precision + recall;
        double f1 = denominator <= 0.0 ? 0.0 : (2.0 * precision * recall) / denominator;

        return PairwiseMetricResult.calculable(
                f1,
                Map.of(
                        "precision", precision,
                        "recall", recall,
                        "overlapUnits", overlapUnits,
                        "leftUnits", leftUnits,
                        "rightUnits", rightUnits,
                        "sharedUnits", sharedUnits));
    }

    private double totalSpanUnits(List<NerSpan> spans, String sourceText, SpanOverlapUnit overlapUnit) {
        return spans.stream()
                .mapToDouble(span -> spanUnits(span, sourceText, overlapUnit))
                .sum();
    }

    private double matchedOverlapUnits(
            List<NerSpan> leftSpans,
            List<NerSpan> rightSpans,
            String sourceText,
            SpanOverlapUnit overlapUnit) {
        List<SpanMatchCandidate> candidates = new ArrayList<>();
        for (int leftIndex = 0; leftIndex < leftSpans.size(); leftIndex++) {
            for (int rightIndex = 0; rightIndex < rightSpans.size(); rightIndex++) {
                NerSpan left = leftSpans.get(leftIndex);
                NerSpan right = rightSpans.get(rightIndex);
                if (!left.overlaps(right)) {
                    continue;
                }

                double overlap = overlapUnits(left, right, sourceText, overlapUnit);
                if (overlap > 0.0) {
                    candidates.add(new SpanMatchCandidate(leftIndex, rightIndex, overlap));
                }
            }
        }

        candidates.sort(Comparator.comparingDouble(SpanMatchCandidate::overlapUnits).reversed());
        Set<Integer> matchedLeftIndexes = new java.util.LinkedHashSet<>();
        Set<Integer> matchedRightIndexes = new java.util.LinkedHashSet<>();
        double totalOverlap = 0.0;

        for (SpanMatchCandidate candidate : candidates) {
            if (matchedLeftIndexes.contains(candidate.leftIndex())
                    || matchedRightIndexes.contains(candidate.rightIndex())) {
                continue;
            }

            matchedLeftIndexes.add(candidate.leftIndex());
            matchedRightIndexes.add(candidate.rightIndex());
            totalOverlap += candidate.overlapUnits();
        }

        return totalOverlap;
    }

    private double spanUnits(NerSpan span, String sourceText, SpanOverlapUnit overlapUnit) {
        if (overlapUnit == SpanOverlapUnit.CHARACTER) {
            return span.charLength();
        }

        String text = safeSubstring(sourceText, span.startOffset(), span.endOffset());
        if (text == null || text.isBlank()) {
            text = span.text();
        }
        return countWords(text);
    }

    private double overlapUnits(
            NerSpan left,
            NerSpan right,
            String sourceText,
            SpanOverlapUnit overlapUnit) {
        int start = Math.max(left.startOffset(), right.startOffset());
        int end = Math.min(left.endOffset(), right.endOffset());
        if (start >= end) {
            return 0.0;
        }

        if (overlapUnit == SpanOverlapUnit.CHARACTER) {
            return end - start;
        }

        String overlapText = safeSubstring(sourceText, start, end);
        if (overlapText == null || overlapText.isBlank()) {
            overlapText = safeSubstring(
                    left.text(),
                    start - left.startOffset(),
                    end - left.startOffset());
        }
        if (overlapText == null || overlapText.isBlank()) {
            overlapText = safeSubstring(
                    right.text(),
                    start - right.startOffset(),
                    end - right.startOffset());
        }
        return overlapText == null || overlapText.isBlank() ? 0.0 : countWords(overlapText);
    }

    private String safeSubstring(String sourceText, int start, int end) {
        if (sourceText == null || start < 0 || end > sourceText.length() || start >= end) {
            return null;
        }
        return sourceText.substring(start, end);
    }

    private int countWords(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }

        BreakIterator iterator = BreakIterator.getWordInstance(Locale.ROOT);
        iterator.setText(value);
        int count = 0;
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            String token = value.substring(start, end);
            if (token.codePoints().anyMatch(Character::isLetterOrDigit)) {
                count++;
            }
        }
        return count;
    }

    private record SpanMatchCandidate(
            int leftIndex,
            int rightIndex,
            double overlapUnits) {
    }
}
