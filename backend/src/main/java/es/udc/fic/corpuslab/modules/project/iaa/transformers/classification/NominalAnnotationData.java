package es.udc.fic.corpuslab.modules.project.iaa.transformers.classification;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import es.udc.fic.corpuslab.modules.project.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.iaa.calculators.pairwise.AnnotatorAnnotationVector;
import es.udc.fic.corpuslab.modules.project.iaa.calculators.pairwise.PairwiseAnnotationData;
import es.udc.fic.corpuslab.modules.project.iaa.model.AnnotationUnitKey;

public record NominalAnnotationData(
        ProjectType projectType,
        List<AnnotatorAnnotationVector<AnnotationUnitKey, String>> annotatorVectors,
        Map<AnnotationUnitKey, List<String>> ratingsByUnit,
        Set<String> categories) implements PairwiseAnnotationData<AnnotationUnitKey, String> {

    public NominalAnnotationData {
        annotatorVectors = annotatorVectors == null ? List.of() : List.copyOf(annotatorVectors);
        ratingsByUnit = immutableRatingsByUnit(ratingsByUnit);
        categories = categories == null ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(categories));
    }

    private static Map<AnnotationUnitKey, List<String>> immutableRatingsByUnit(
            Map<AnnotationUnitKey, List<String>> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }

        Map<AnnotationUnitKey, List<String>> copy = new LinkedHashMap<>();
        source.forEach((unitKey, ratings) -> {
            if (unitKey != null && ratings != null && !ratings.isEmpty()) {
                copy.put(unitKey, List.copyOf(ratings));
            }
        });
        return Collections.unmodifiableMap(copy);
    }
}
