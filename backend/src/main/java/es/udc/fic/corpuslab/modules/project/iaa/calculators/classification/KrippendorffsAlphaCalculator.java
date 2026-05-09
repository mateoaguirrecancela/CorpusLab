package es.udc.fic.corpuslab.modules.project.iaa.calculators.classification;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import es.udc.fic.corpuslab.modules.project.enums.IaaResultStatus;
import es.udc.fic.corpuslab.modules.project.enums.MetricType;
import es.udc.fic.corpuslab.modules.project.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.exceptions.UnsupportedMetricException;
import es.udc.fic.corpuslab.modules.project.iaa.calculators.IaaMetricCalculator;
import es.udc.fic.corpuslab.modules.project.iaa.context.AnnotationCalculationContext;
import es.udc.fic.corpuslab.modules.project.iaa.dtos.IaaResult;
import es.udc.fic.corpuslab.modules.project.iaa.transformers.AnnotationDataTransformer;
import es.udc.fic.corpuslab.modules.project.iaa.transformers.classification.NominalAnnotationData;
import es.udc.fic.corpuslab.modules.project.iaa.transformers.classification.NominalAnnotationDataTransformer;

@Component
public class KrippendorffsAlphaCalculator implements IaaMetricCalculator {

    private static final Set<ProjectType> SUPPORTED_PROJECT_TYPES = Set.of(
            ProjectType.TEXT_CLASSIFICATION_SIMPLE,
            ProjectType.TEXT_CLASSIFICATION_MULTILABEL);

    private final AnnotationDataTransformer<NominalAnnotationData> transformer;

    @Autowired
    public KrippendorffsAlphaCalculator(NominalAnnotationDataTransformer transformer) {
        this((AnnotationDataTransformer<NominalAnnotationData>) transformer);
    }

    KrippendorffsAlphaCalculator(AnnotationDataTransformer<NominalAnnotationData> transformer) {
        this.transformer = Objects.requireNonNull(transformer, "transformer is required");
    }

    @Override
    public MetricType metricType() {
        return MetricType.KRIPPENDORFFS_ALPHA;
    }

    @Override
    public Set<ProjectType> supportedProjectTypes() {
        return SUPPORTED_PROJECT_TYPES;
    }

    @Override
    public IaaResult calculate(AnnotationCalculationContext context) {
        Objects.requireNonNull(context, "context is required");
        if (!supports(context.projectType())) {
            throw UnsupportedMetricException.incompatible(context.projectType(), metricType());
        }

        NominalAnnotationData data = transformer.transform(context);
        int annotatorCount = data.annotatorCount();
        int unitCount = data.unitCount();

        if (annotatorCount < 2) {
            return notCalculable(
                    data.projectType(),
                    IaaResultStatus.INSUFFICIENT_ANNOTATORS,
                    "At least two annotators are required for Krippendorff's alpha",
                    annotatorCount,
                    unitCount,
                    Map.of());
        }

        double observedDisagreementNumerator = 0.0;
        double totalPairableValues = 0.0;
        int pairableUnitCount = 0;
        int skippedUnitCount = 0;
        Map<String, Double> categoryMarginals = new LinkedHashMap<>();

        for (var ratings : data.ratingsByUnit().values()) {
            int unitRatingCount = ratings.size();
            if (unitRatingCount < 2) {
                skippedUnitCount++;
                continue;
            }

            Map<String, Integer> counts = categoryCounts(ratings);
            double sameOrderedPairs = counts.values().stream()
                    .mapToDouble(count -> count * (count - 1.0))
                    .sum();
            double orderedPairs = unitRatingCount * (unitRatingCount - 1.0);
            observedDisagreementNumerator += (orderedPairs - sameOrderedPairs) / (unitRatingCount - 1.0);
            totalPairableValues += unitRatingCount;
            pairableUnitCount++;

            counts.forEach((category, count) -> categoryMarginals.merge(category, (double) count, Double::sum));
        }

        if (pairableUnitCount == 0 || totalPairableValues <= 1.0) {
            return notCalculable(
                    data.projectType(),
                    IaaResultStatus.NO_SHARED_ITEMS,
                    "No unit has at least two ratings for Krippendorff's alpha",
                    annotatorCount,
                    unitCount,
                    Map.of("skippedUnits", skippedUnitCount));
        }

        double observedDisagreement = observedDisagreementNumerator / totalPairableValues;
        double squaredMarginalSum = categoryMarginals.values().stream()
                .mapToDouble(value -> value * value)
                .sum();
        double expectedDisagreement = ((totalPairableValues * totalPairableValues) - squaredMarginalSum)
                / (totalPairableValues * (totalPairableValues - 1.0));

        if (Math.abs(expectedDisagreement) < 1.0e-12) {
            return notCalculable(
                    data.projectType(),
                    IaaResultStatus.UNDEFINED,
                    "Krippendorff's alpha is undefined when expected disagreement is 0",
                    annotatorCount,
                    unitCount,
                    details(pairableUnitCount, skippedUnitCount, observedDisagreement, expectedDisagreement));
        }

        double alpha = 1.0 - (observedDisagreement / expectedDisagreement);
        return IaaResult.calculable(
                metricType(),
                data.projectType(),
                alpha,
                annotatorCount,
                unitCount,
                pairableUnitCount,
                details(pairableUnitCount, skippedUnitCount, observedDisagreement, expectedDisagreement));
    }

    private Map<String, Integer> categoryCounts(java.util.List<String> ratings) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        ratings.forEach(rating -> counts.merge(rating, 1, Integer::sum));
        return counts;
    }

    private Map<String, Object> details(
            int pairableUnitCount,
            int skippedUnitCount,
            double observedDisagreement,
            double expectedDisagreement) {
        return Map.of(
                "pairableUnits", pairableUnitCount,
                "skippedUnits", skippedUnitCount,
                "observedDisagreement", observedDisagreement,
                "expectedDisagreement", expectedDisagreement);
    }

    private IaaResult notCalculable(
            ProjectType projectType,
            IaaResultStatus status,
            String message,
            int annotatorCount,
            int unitCount,
            Map<String, Object> details) {
        return IaaResult.notCalculable(
                metricType(),
                projectType,
                status,
                message,
                annotatorCount,
                unitCount,
                0,
                details);
    }
}
