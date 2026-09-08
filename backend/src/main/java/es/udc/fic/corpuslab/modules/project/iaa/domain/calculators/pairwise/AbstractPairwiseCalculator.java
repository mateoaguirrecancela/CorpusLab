package es.udc.fic.corpuslab.modules.project.iaa.domain.calculators.pairwise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import es.udc.fic.corpuslab.modules.project.shared.enums.IaaResultStatus;
import es.udc.fic.corpuslab.modules.project.shared.enums.MetricType;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.iaa.domain.exceptions.UnsupportedIaaMetricException;
import es.udc.fic.corpuslab.modules.project.iaa.domain.calculators.IaaMetricCalculator;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaCalculationContext;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaResult;
import es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.AnnotationDataTransformer;

public abstract class AbstractPairwiseCalculator<K, V, T extends PairwiseAnnotationData<K, V>>
        implements IaaMetricCalculator {

    private final MetricType metricType;
    private final Set<ProjectType> supportedProjectTypes;
    private final AnnotationDataTransformer<T> transformer;

    protected AbstractPairwiseCalculator(
            MetricType metricType,
            Set<ProjectType> supportedProjectTypes,
            AnnotationDataTransformer<T> transformer) {
        this.metricType = Objects.requireNonNull(metricType, "metricType is required");
        this.supportedProjectTypes = immutableNonEmptyCopy(supportedProjectTypes);
        this.transformer = Objects.requireNonNull(transformer, "transformer is required");
    }

    @Override
    public MetricType metricType() {
        return metricType;
    }

    @Override
    public Set<ProjectType> supportedProjectTypes() {
        return supportedProjectTypes;
    }

    @Override
    public IaaResult calculate(IaaCalculationContext context) {
        Objects.requireNonNull(context, "context is required");
        if (!supports(context.projectType())) {
            throw UnsupportedIaaMetricException.incompatible(context.projectType(), metricType);
        }

        T data = transformer.transform(context);
        if (data == null) {
            return notCalculable(
                    context.projectType(),
                    IaaResultStatus.UNDEFINED,
                    "Transformer returned no pairwise data",
                    0,
                    0,
                    0,
                    Map.of());
        }

        List<AnnotatorAnnotationVector<K, V>> annotators = normalizedAnnotators(data);
        int annotatorCount = annotators.size();
        int unitCount = countDistinctUnits(annotators);

        if (annotatorCount < 2) {
            return notCalculable(
                    data.projectType(),
                    IaaResultStatus.INSUFFICIENT_ANNOTATORS,
                    "At least two annotators are required for pairwise IAA",
                    annotatorCount,
                    unitCount,
                    0,
                    Map.of("missingAnnotators", 2 - annotatorCount));
        }

        if (unitCount == 0) {
            return notCalculable(
                    data.projectType(),
                    IaaResultStatus.NO_ANNOTATIONS,
                    "No annotations are available for pairwise IAA",
                    annotatorCount,
                    unitCount,
                    0,
                    Map.of());
        }

        List<PairwiseAnnotationPair<K, V>> allPairs = generateUniquePairs(annotators);
        List<Double> calculablePairValues = new ArrayList<>();
        List<Map<String, Object>> pairDetails = new ArrayList<>();
        int overlappingPairCount = 0;
        int nonOverlappingPairCount = 0;
        int nonCalculablePairCount = 0;

        for (PairwiseAnnotationPair<K, V> pair : allPairs) {
            if (!pair.hasSharedUnits()) {
                nonOverlappingPairCount++;
                pairDetails.add(pairDetails(pair, null, false, "No shared annotation units"));
                continue;
            }

            overlappingPairCount++;
            PairwiseMetricResult pairResult = calculatePairMetric(pair);
            if (pairResult != null && pairResult.calculable()) {
                calculablePairValues.add(pairResult.value());
                pairDetails.add(pairDetails(pair, pairResult, true, ""));
            } else {
                nonCalculablePairCount++;
                String message = pairResult == null ? "Pairwise metric returned no result" : pairResult.message();
                pairDetails.add(pairDetails(pair, pairResult, false, message));
            }
        }

        Map<String, Object> details = baseDetails(
                allPairs.size(),
                overlappingPairCount,
                nonOverlappingPairCount,
                nonCalculablePairCount,
                calculablePairValues,
                pairDetails);

        if (overlappingPairCount == 0) {
            return notCalculable(
                    data.projectType(),
                    IaaResultStatus.NO_SHARED_ITEMS,
                    "No annotator pair shares annotated units",
                    annotatorCount,
                    unitCount,
                    0,
                    details);
        }

        if (calculablePairValues.isEmpty()) {
            return notCalculable(
                    data.projectType(),
                    IaaResultStatus.NO_VALID_PAIRS,
                    "No annotator pair produced a calculable metric value",
                    annotatorCount,
                    unitCount,
                    0,
                    details);
        }

        double mean = calculablePairValues.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(Double.NaN);

        return IaaResult.calculable(
                metricType,
                data.projectType(),
                mean,
                annotatorCount,
                unitCount,
                calculablePairValues.size(),
                details);
    }

    protected abstract PairwiseMetricResult calculatePairMetric(PairwiseAnnotationPair<K, V> pair);

    protected List<PairwiseAnnotationPair<K, V>> generateUniquePairs(List<AnnotatorAnnotationVector<K, V>> annotators) {
        List<PairwiseAnnotationPair<K, V>> pairs = new ArrayList<>();
        for (int leftIndex = 0; leftIndex < annotators.size(); leftIndex++) {
            for (int rightIndex = leftIndex + 1; rightIndex < annotators.size(); rightIndex++) {
                AnnotatorAnnotationVector<K, V> left = annotators.get(leftIndex);
                AnnotatorAnnotationVector<K, V> right = annotators.get(rightIndex);
                pairs.add(new PairwiseAnnotationPair<>(left, right, sharedUnitKeys(left, right)));
            }
        }
        return List.copyOf(pairs);
    }

    protected Map<String, Object> baseDetails(
            int totalPairCount,
            int overlappingPairCount,
            int nonOverlappingPairCount,
            int nonCalculablePairCount,
            List<Double> calculablePairValues,
            List<Map<String, Object>> pairDetails) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("totalPairs", totalPairCount);
        details.put("overlappingPairs", overlappingPairCount);
        details.put("nonOverlappingPairs", nonOverlappingPairCount);
        details.put("nonCalculablePairs", nonCalculablePairCount);
        details.put("calculablePairs", calculablePairValues.size());
        details.put("pairScores", List.copyOf(calculablePairValues));
        details.put("pairs", List.copyOf(pairDetails));
        return Collections.unmodifiableMap(details);
    }

    private IaaResult notCalculable(
            ProjectType projectType,
            IaaResultStatus status,
            String message,
            int annotatorCount,
            int unitCount,
            int pairCount,
            Map<String, Object> details) {
        return IaaResult.notCalculable(
                metricType,
                projectType,
                status,
                message,
                annotatorCount,
                unitCount,
                pairCount,
                details);
    }

    private List<AnnotatorAnnotationVector<K, V>> normalizedAnnotators(T data) {
        if (data.annotatorVectors() == null || data.annotatorVectors().isEmpty()) {
            return List.of();
        }

        List<AnnotatorAnnotationVector<K, V>> annotators = new ArrayList<>();
        for (AnnotatorAnnotationVector<K, V> annotatorVector : data.annotatorVectors()) {
            if (annotatorVector != null) {
                annotators.add(annotatorVector);
            }
        }
        return List.copyOf(annotators);
    }

    private int countDistinctUnits(List<AnnotatorAnnotationVector<K, V>> annotators) {
        Set<K> unitKeys = new LinkedHashSet<>();
        for (AnnotatorAnnotationVector<K, V> annotator : annotators) {
            unitKeys.addAll(annotator.unitKeys());
        }
        return unitKeys.size();
    }

    private Set<K> sharedUnitKeys(
            AnnotatorAnnotationVector<K, V> left,
            AnnotatorAnnotationVector<K, V> right) {
        Set<K> sharedUnitKeys = new LinkedHashSet<>();
        for (K unitKey : left.unitKeys()) {
            if (right.hasAnnotationFor(unitKey)) {
                sharedUnitKeys.add(unitKey);
            }
        }
        return sharedUnitKeys;
    }

    private Map<String, Object> pairDetails(
            PairwiseAnnotationPair<K, V> pair,
            PairwiseMetricResult pairResult,
            boolean calculable,
            String nonCalculableMessage) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("leftAnnotatorId", pair.leftAnnotatorId());
        details.put("rightAnnotatorId", pair.rightAnnotatorId());
        details.put("sharedUnitCount", pair.sharedUnitCount());
        details.put("calculable", calculable);
        if (calculable && pairResult != null) {
            details.put("value", pairResult.value());
        }
        if (!calculable) {
            details.put("message", nonCalculableMessage == null ? "" : nonCalculableMessage);
        }
        if (pairResult != null && !pairResult.details().isEmpty()) {
            details.put("details", pairResult.details());
        }
        return Collections.unmodifiableMap(details);
    }

    private Set<ProjectType> immutableNonEmptyCopy(Set<ProjectType> source) {
        Objects.requireNonNull(source, "supportedProjectTypes is required");
        if (source.isEmpty()) {
            throw new IllegalArgumentException("At least one supported project type is required");
        }
        return Set.copyOf(source);
    }
}
