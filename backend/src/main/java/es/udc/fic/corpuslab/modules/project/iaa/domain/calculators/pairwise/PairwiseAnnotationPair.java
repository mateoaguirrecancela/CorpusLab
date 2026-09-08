package es.udc.fic.corpuslab.modules.project.iaa.domain.calculators.pairwise;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record PairwiseAnnotationPair<K, V>(
        AnnotatorAnnotationVector<K, V> left,
        AnnotatorAnnotationVector<K, V> right,
        Set<K> sharedUnitKeys) {

    public PairwiseAnnotationPair {
        Objects.requireNonNull(left, "left annotator is required");
        Objects.requireNonNull(right, "right annotator is required");
        sharedUnitKeys = immutableCopy(sharedUnitKeys);
    }

    public Long leftAnnotatorId() {
        return left.annotatorId();
    }

    public Long rightAnnotatorId() {
        return right.annotatorId();
    }

    public int sharedUnitCount() {
        return sharedUnitKeys.size();
    }

    public boolean hasSharedUnits() {
        return !sharedUnitKeys.isEmpty();
    }

    public V leftAnnotationFor(K unitKey) {
        return left.annotationFor(unitKey);
    }

    public V rightAnnotationFor(K unitKey) {
        return right.annotationFor(unitKey);
    }

    private static <K> Set<K> immutableCopy(Set<K> source) {
        if (source == null || source.isEmpty()) {
            return Set.of();
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(source));
    }
}
