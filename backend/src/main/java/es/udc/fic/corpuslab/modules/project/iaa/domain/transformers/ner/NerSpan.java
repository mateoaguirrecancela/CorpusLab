package es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.ner;

import java.util.Objects;

public record NerSpan(
        String label,
        String text,
        int startOffset,
        int endOffset) {

    public NerSpan {
        Objects.requireNonNull(label, "label is required");
        Objects.requireNonNull(text, "text is required");
        if (startOffset < 0 || endOffset <= startOffset) {
            throw new IllegalArgumentException("NER span offsets are invalid");
        }
    }

    public int charLength() {
        return endOffset - startOffset;
    }

    public boolean overlaps(NerSpan other) {
        return other != null
                && label.equals(other.label())
                && Math.max(startOffset, other.startOffset()) < Math.min(endOffset, other.endOffset());
    }
}
