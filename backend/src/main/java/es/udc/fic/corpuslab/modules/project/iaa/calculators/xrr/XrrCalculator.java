package es.udc.fic.corpuslab.modules.project.iaa.calculators.xrr;

import java.util.LinkedHashMap;
import java.util.List;
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
import es.udc.fic.corpuslab.modules.project.iaa.transformers.xrr.XrrAnnotationData;
import es.udc.fic.corpuslab.modules.project.iaa.transformers.xrr.XrrAnnotationDataTransformer;

@Component
public class XrrCalculator implements IaaMetricCalculator {

    private static final double EPSILON = 1.0e-12;
    private static final Set<ProjectType> SUPPORTED_PROJECT_TYPES = Set.of(
            ProjectType.TEXT_CLASSIFICATION_SIMPLE,
            ProjectType.TEXT_CLASSIFICATION_MULTILABEL,
            ProjectType.NER);

    private final AnnotationDataTransformer<XrrAnnotationData> transformer;

    @Autowired
    public XrrCalculator(XrrAnnotationDataTransformer transformer) {
        this((AnnotationDataTransformer<XrrAnnotationData>) transformer);
    }

    XrrCalculator(AnnotationDataTransformer<XrrAnnotationData> transformer) {
        this.transformer = Objects.requireNonNull(transformer, "transformer is required");
    }

    @Override
    public MetricType metricType() {
        return MetricType.XRR;
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

        XrrAnnotationData data = transformer.transform(context);
        if (data.groupXRaterCount() == 0 || data.groupYRaterCount() == 0) {
            return notCalculable(
                    data,
                    IaaResultStatus.INSUFFICIENT_ANNOTATORS,
                    "XRR requires at least one rater/source in each group",
                    baseDetails(data));
        }

        if (data.unitCount() == 0) {
            return notCalculable(
                    data,
                    IaaResultStatus.INSUFFICIENT_ITEMS,
                    "XRR requires at least one unit with complete ratings in both groups",
                    baseDetails(data));
        }

        double groupXIrr = calculateGroupIrr(data.groupXRows());
        double groupYIrr = calculateGroupIrr(data.groupYRows());
        double crossKappa = calculateCrossKappa(data.groupXRows(), data.groupYRows());

        Map<String, Object> details = baseDetails(data);
        details.put("crossKappa", crossKappa);
        details.put("groupXIrr", groupXIrr);
        details.put("groupYIrr", groupYIrr);

        if (!Double.isFinite(groupXIrr) || !Double.isFinite(groupYIrr)
                || groupXIrr <= 0.0 || groupYIrr <= 0.0) {
            return notCalculable(
                    data,
                    IaaResultStatus.UNDEFINED,
                    "XRR cannot be normalized when one or both group IRR values are not positive",
                    details);
        }

        double denominator = Math.sqrt(groupXIrr) * Math.sqrt(groupYIrr);
        details.put("normalizationDenominator", denominator);
        if (denominator <= 0.0 || !Double.isFinite(denominator) || !Double.isFinite(crossKappa)) {
            return notCalculable(
                    data,
                    IaaResultStatus.UNDEFINED,
                    "XRR normalization denominator is not mathematically usable",
                    details);
        }

        double normalizedXrr = crossKappa / denominator;
        if (!Double.isFinite(normalizedXrr)) {
            return notCalculable(
                    data,
                    IaaResultStatus.UNDEFINED,
                    "XRR produced a non-finite value",
                    details);
        }

        return IaaResult.calculable(
                metricType(),
                data.projectType(),
                normalizedXrr,
                data.annotatorCount(),
                data.unitCount(),
                data.unitCount(),
                details);
    }

    private double calculateGroupIrr(List<List<String>> matrix) {
        int raterCount = columnCount(matrix);
        if (raterCount < 2) {
            return 1.0;
        }

        double kappaSum = 0.0;
        int pairCount = 0;
        for (int leftIndex = 0; leftIndex < raterCount; leftIndex++) {
            for (int rightIndex = leftIndex + 1; rightIndex < raterCount; rightIndex++) {
                double kappa = calculateCohenKappa(matrix, leftIndex, rightIndex);
                if (!Double.isFinite(kappa)) {
                    return Double.NaN;
                }
                kappaSum += kappa;
                pairCount++;
            }
        }

        return pairCount == 0 ? Double.NaN : kappaSum / pairCount;
    }

    private double calculateCrossKappa(List<List<String>> groupX, List<List<String>> groupY) {
        int itemCount = groupX.size();
        int groupXRaterCount = columnCount(groupX);
        int groupYRaterCount = columnCount(groupY);
        if (itemCount == 0 || itemCount != groupY.size() || groupXRaterCount == 0 || groupYRaterCount == 0) {
            return Double.NaN;
        }

        double observedDisagreements = 0.0;
        for (int itemIndex = 0; itemIndex < itemCount; itemIndex++) {
            List<String> xRow = groupX.get(itemIndex);
            List<String> yRow = groupY.get(itemIndex);
            for (String xValue : xRow) {
                for (String yValue : yRow) {
                    if (!xValue.equals(yValue)) {
                        observedDisagreements++;
                    }
                }
            }
        }
        double observedDisagreement = observedDisagreements
                / (itemCount * (double) groupXRaterCount * groupYRaterCount);

        List<String> allX = flatten(groupX);
        List<String> allY = flatten(groupY);
        double expectedDisagreements = 0.0;
        for (String xValue : allX) {
            for (String yValue : allY) {
                if (!xValue.equals(yValue)) {
                    expectedDisagreements++;
                }
            }
        }
        double expectedDisagreement = expectedDisagreements
                / (itemCount * (double) itemCount * groupXRaterCount * groupYRaterCount);

        if (Math.abs(expectedDisagreement) < EPSILON) {
            return Math.abs(observedDisagreement) < EPSILON ? 1.0 : 0.0;
        }

        return 1.0 - (observedDisagreement / expectedDisagreement);
    }

    private double calculateCohenKappa(List<List<String>> matrix, int leftIndex, int rightIndex) {
        int itemCount = matrix.size();
        if (itemCount == 0) {
            return Double.NaN;
        }

        Map<String, Integer> leftCounts = new LinkedHashMap<>();
        Map<String, Integer> rightCounts = new LinkedHashMap<>();
        int agreements = 0;

        for (List<String> row : matrix) {
            String left = row.get(leftIndex);
            String right = row.get(rightIndex);
            leftCounts.merge(left, 1, Integer::sum);
            rightCounts.merge(right, 1, Integer::sum);
            if (left.equals(right)) {
                agreements++;
            }
        }

        double observedAgreement = agreements / (double) itemCount;
        double expectedAgreement = 0.0;
        Set<String> categories = new java.util.LinkedHashSet<>();
        categories.addAll(leftCounts.keySet());
        categories.addAll(rightCounts.keySet());
        for (String category : categories) {
            double leftProportion = leftCounts.getOrDefault(category, 0) / (double) itemCount;
            double rightProportion = rightCounts.getOrDefault(category, 0) / (double) itemCount;
            expectedAgreement += leftProportion * rightProportion;
        }

        double denominator = 1.0 - expectedAgreement;
        if (Math.abs(denominator) < EPSILON) {
            return Math.abs(observedAgreement - 1.0) < EPSILON ? 1.0 : 0.0;
        }

        return (observedAgreement - expectedAgreement) / denominator;
    }

    private int columnCount(List<List<String>> matrix) {
        return matrix == null || matrix.isEmpty() ? 0 : matrix.get(0).size();
    }

    private List<String> flatten(List<List<String>> matrix) {
        return matrix.stream()
                .flatMap(List::stream)
                .toList();
    }

    private Map<String, Object> baseDetails(XrrAnnotationData data) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("groupXSources", data.groupXSources());
        details.put("groupYSources", data.groupYSources());
        details.put("groupXRaterCount", data.groupXRaterCount());
        details.put("groupYRaterCount", data.groupYRaterCount());
        details.put("groupXTotalRaterCount", data.totalGroupXSourceCount());
        details.put("groupYTotalRaterCount", data.totalGroupYSourceCount());
        details.put("activeAnnotators", data.annotatorCount());
        details.put("inactiveAnnotators", data.totalAnnotatorCount() - data.annotatorCount());
        details.put("provisional",
                data.totalAnnotatorCount() != data.annotatorCount() || data.skippedUnitCount() > 0);
        details.put("missingRaterGroups", missingRaterGroups(data));
        details.put("usableUnits", data.unitCount());
        details.put("candidateUnits", data.candidateUnitCount());
        details.put("skippedUnits", data.skippedUnitCount());
        details.put("missingCompleteUnits", data.skippedUnitCount());
        return details;
    }

    private int missingRaterGroups(XrrAnnotationData data) {
        int missingGroups = 0;
        if (data.groupXRaterCount() == 0) {
            missingGroups++;
        }
        if (data.groupYRaterCount() == 0) {
            missingGroups++;
        }
        return missingGroups;
    }

    private IaaResult notCalculable(
            XrrAnnotationData data,
            IaaResultStatus status,
            String message,
            Map<String, Object> details) {
        return IaaResult.notCalculable(
                metricType(),
                data.projectType(),
                status,
                message,
                data.annotatorCount(),
                data.unitCount(),
                data.unitCount(),
                details);
    }
}
