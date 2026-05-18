package es.udc.fic.corpuslab.modules.project.iaa.domain.calculators.pairwise;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.TransformedAnnotationData;

public interface PairwiseAnnotationData<K, V> extends TransformedAnnotationData {

    List<AnnotatorAnnotationVector<K, V>> annotatorVectors();

    @Override
    default int annotatorCount() {
        return annotatorVectors() == null ? 0 : annotatorVectors().size();
    }

    @Override
    default int unitCount() {
        if (annotatorVectors() == null || annotatorVectors().isEmpty()) {
            return 0;
        }

        Set<K> unitKeys = new LinkedHashSet<>();
        for (AnnotatorAnnotationVector<K, V> annotatorVector : annotatorVectors()) {
            if (annotatorVector != null) {
                unitKeys.addAll(annotatorVector.unitKeys());
            }
        }
        return unitKeys.size();
    }
}
