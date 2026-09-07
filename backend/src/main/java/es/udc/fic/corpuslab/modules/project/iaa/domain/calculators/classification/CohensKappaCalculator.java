package es.udc.fic.corpuslab.modules.project.iaa.domain.calculators.classification;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;


import es.udc.fic.corpuslab.modules.project.shared.enums.MetricType;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.iaa.domain.calculators.pairwise.AbstractPairwiseCalculator;
import es.udc.fic.corpuslab.modules.project.iaa.domain.calculators.pairwise.PairwiseAnnotationPair;
import es.udc.fic.corpuslab.modules.project.iaa.domain.calculators.pairwise.PairwiseMetricResult;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.AnnotationUnitKey;
import es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.AnnotationDataTransformer;
import es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.classification.NominalAnnotationData;
import es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.classification.NominalAnnotationDataTransformer;

public class CohensKappaCalculator
        extends AbstractPairwiseCalculator<AnnotationUnitKey, String, NominalAnnotationData> {

    public CohensKappaCalculator(NominalAnnotationDataTransformer transformer) {
        this((AnnotationDataTransformer<NominalAnnotationData>) transformer);
    }

    CohensKappaCalculator(AnnotationDataTransformer<NominalAnnotationData> transformer) {
        super(
                MetricType.COHENS_KAPPA,
                Set.of(
                        ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                        ProjectType.TEXT_CLASSIFICATION_MULTILABEL,
                        ProjectType.NER),
                transformer);
    }

    @Override
    protected PairwiseMetricResult calculatePairMetric(PairwiseAnnotationPair<AnnotationUnitKey, String> pair) {
        int total = pair.sharedUnitCount();
        if (total <= 0) {
            return PairwiseMetricResult.notCalculable("No shared units for Cohen's kappa");
        }

        Map<String, Integer> leftCategoryCounts = new LinkedHashMap<>();
        Map<String, Integer> rightCategoryCounts = new LinkedHashMap<>();
        int agreements = 0;

        for (AnnotationUnitKey unitKey : pair.sharedUnitKeys()) {
            String left = pair.leftAnnotationFor(unitKey);
            String right = pair.rightAnnotationFor(unitKey);
            leftCategoryCounts.merge(left, 1, Integer::sum);
            rightCategoryCounts.merge(right, 1, Integer::sum);
            if (left.equals(right)) {
                agreements++;
            }
        }

        double observedAgreement = agreements / (double) total;
        double expectedAgreement = 0.0;
        for (String category : unionCategories(leftCategoryCounts, rightCategoryCounts)) {
            double leftProportion = leftCategoryCounts.getOrDefault(category, 0) / (double) total;
            double rightProportion = rightCategoryCounts.getOrDefault(category, 0) / (double) total;
            expectedAgreement += leftProportion * rightProportion;
        }

        double denominator = 1.0 - expectedAgreement;
        if (Math.abs(denominator) < 1.0e-12) {
            return PairwiseMetricResult.notCalculable(
                    "Cohen's kappa is undefined when expected agreement is 1",
                    Map.of(
                            "observedAgreement", observedAgreement,
                            "expectedAgreement", expectedAgreement,
                            "sharedUnits", total));
        }

        double kappa = (observedAgreement - expectedAgreement) / denominator;
        return PairwiseMetricResult.calculable(
                kappa,
                Map.of(
                        "observedAgreement", observedAgreement,
                        "expectedAgreement", expectedAgreement,
                        "sharedUnits", total));
    }

    private Set<String> unionCategories(
            Map<String, Integer> leftCategoryCounts,
            Map<String, Integer> rightCategoryCounts) {
        java.util.LinkedHashSet<String> categories = new java.util.LinkedHashSet<>();
        categories.addAll(leftCategoryCounts.keySet());
        categories.addAll(rightCategoryCounts.keySet());
        return categories;
    }
}
