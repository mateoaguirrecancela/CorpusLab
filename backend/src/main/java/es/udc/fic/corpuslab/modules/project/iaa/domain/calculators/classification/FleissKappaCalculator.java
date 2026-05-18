package es.udc.fic.corpuslab.modules.project.iaa.domain.calculators.classification;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


import es.udc.fic.corpuslab.modules.project.shared.enums.IaaResultStatus;
import es.udc.fic.corpuslab.modules.project.shared.enums.MetricType;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.iaa.domain.exceptions.UnsupportedIaaMetricException;
import es.udc.fic.corpuslab.modules.project.iaa.domain.calculators.IaaMetricCalculator;
import es.udc.fic.corpuslab.modules.project.iaa.domain.calculators.pairwise.AnnotatorAnnotationVector;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaCalculationContext;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaResult;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.AnnotationUnitKey;
import es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.AnnotationDataTransformer;
import es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.classification.NominalAnnotationData;
import es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.classification.NominalAnnotationDataTransformer;

public class FleissKappaCalculator implements IaaMetricCalculator {

    private static final Set<ProjectType> SUPPORTED_PROJECT_TYPES = Set.of(
            ProjectType.TEXT_CLASSIFICATION_SIMPLE,
            ProjectType.TEXT_CLASSIFICATION_MULTILABEL);

    private final AnnotationDataTransformer<NominalAnnotationData> transformer;

    public FleissKappaCalculator(NominalAnnotationDataTransformer transformer) {
        this((AnnotationDataTransformer<NominalAnnotationData>) transformer);
    }

    FleissKappaCalculator(AnnotationDataTransformer<NominalAnnotationData> transformer) {
        this.transformer = Objects.requireNonNull(transformer, "transformer is required");
    }

    @Override
    public MetricType metricType() {
        return MetricType.FLEISS_KAPPA;
    }

    @Override
    public Set<ProjectType> supportedProjectTypes() {
        return SUPPORTED_PROJECT_TYPES;
    }

    @Override
    public IaaResult calculate(IaaCalculationContext context) {
        Objects.requireNonNull(context, "context is required");
        if (!supports(context.projectType())) {
            throw UnsupportedIaaMetricException.incompatible(context.projectType(), metricType());
        }

        NominalAnnotationData data = transformer.transform(context);
        int totalAnnotatorCount = data.annotatorCount();
        List<AnnotatorAnnotationVector<AnnotationUnitKey, String>> activeAnnotators = activeAnnotators(data);
        int annotatorCount = activeAnnotators.size();
        int unitCount = data.unitCount();

        if (annotatorCount < 2) {
            return notCalculable(
                    data.projectType(),
                    IaaResultStatus.INSUFFICIENT_ANNOTATORS,
                    "At least two annotators are required for Fleiss' kappa",
                    annotatorCount,
                    unitCount,
                    Map.of(
                            "activeAnnotators", annotatorCount,
                            "inactiveAnnotators", totalAnnotatorCount - annotatorCount,
                            "missingAnnotators", 2 - annotatorCount));
        }

        double agreementSum = 0.0;
        int completeUnitCount = 0;
        int incompleteUnitCount = 0;
        int missingRatingCount = 0;
        Map<String, Integer> categoryTotals = new LinkedHashMap<>();

        for (var ratings : data.ratingsByUnit().values()) {
            if (ratings.size() != annotatorCount) {
                incompleteUnitCount++;
                missingRatingCount += Math.max(0, annotatorCount - ratings.size());
                continue;
            }

            Map<String, Integer> counts = categoryCounts(ratings);
            double unitAgreementNumerator = counts.values().stream()
                    .mapToDouble(count -> count * (count - 1.0))
                    .sum();
            agreementSum += unitAgreementNumerator / (annotatorCount * (annotatorCount - 1.0));
            completeUnitCount++;
            counts.forEach((category, count) -> categoryTotals.merge(category, count, Integer::sum));
        }

        if (completeUnitCount == 0) {
            return notCalculable(
                    data.projectType(),
                    IaaResultStatus.INSUFFICIENT_ITEMS,
                    "No item has ratings from every annotator, which Fleiss' kappa requires",
                    annotatorCount,
                    unitCount,
                    Map.of(
                            "completeUnits", completeUnitCount,
                            "incompleteUnits", incompleteUnitCount,
                            "missingRatings", missingRatingCount,
                            "activeAnnotators", annotatorCount,
                            "inactiveAnnotators", totalAnnotatorCount - annotatorCount));
        }

        double meanObservedAgreement = agreementSum / completeUnitCount;
        double totalRatings = completeUnitCount * (double) annotatorCount;
        double expectedAgreement = categoryTotals.values().stream()
                .mapToDouble(count -> {
                    double proportion = count / totalRatings;
                    return proportion * proportion;
                })
                .sum();

        double denominator = 1.0 - expectedAgreement;
        Map<String, Object> details = Map.of(
                "completeUnits", completeUnitCount,
                "incompleteUnits", incompleteUnitCount,
                "missingRatings", missingRatingCount,
                "activeAnnotators", annotatorCount,
                "inactiveAnnotators", totalAnnotatorCount - annotatorCount,
                "provisional", totalAnnotatorCount != annotatorCount || incompleteUnitCount > 0,
                "meanObservedAgreement", meanObservedAgreement,
                "expectedAgreement", expectedAgreement);

        if (Math.abs(denominator) < 1.0e-12) {
            return notCalculable(
                    data.projectType(),
                    IaaResultStatus.UNDEFINED,
                    "Fleiss' kappa is undefined when expected agreement is 1",
                    annotatorCount,
                    unitCount,
                    details);
        }

        double kappa = (meanObservedAgreement - expectedAgreement) / denominator;
        return IaaResult.calculable(
                metricType(),
                data.projectType(),
                kappa,
                annotatorCount,
                unitCount,
                completeUnitCount,
                details);
    }

    private List<AnnotatorAnnotationVector<AnnotationUnitKey, String>> activeAnnotators(NominalAnnotationData data) {
        return data.annotatorVectors()
                .stream()
                .filter(annotator -> annotator != null && !annotator.annotationsByUnit().isEmpty())
                .toList();
    }

    private Map<String, Integer> categoryCounts(java.util.List<String> ratings) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        ratings.forEach(rating -> counts.merge(rating, 1, Integer::sum));
        return counts;
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
