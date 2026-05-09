package es.udc.fic.corpuslab.modules.project.iaa.transformers.classification;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import es.udc.fic.corpuslab.common.utils.StringUtils;
import es.udc.fic.corpuslab.modules.project.entities.Annotation;
import es.udc.fic.corpuslab.modules.project.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.enums.MetricType;
import es.udc.fic.corpuslab.modules.project.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.iaa.calculators.pairwise.AnnotatorAnnotationVector;
import es.udc.fic.corpuslab.modules.project.iaa.context.AnnotationCalculationContext;
import es.udc.fic.corpuslab.modules.project.iaa.model.AnnotationUnitKey;
import es.udc.fic.corpuslab.modules.project.iaa.transformers.AnnotationDataTransformer;
import es.udc.fic.corpuslab.modules.project.utils.ProjectAnnotationUtils;
import es.udc.fic.corpuslab.modules.project.utils.ProjectConstants;

@Component
public class NominalAnnotationDataTransformer implements AnnotationDataTransformer<NominalAnnotationData> {

    private static final String MULTILABEL_CATEGORY_SEPARATOR = "||";

    @Override
    public Set<ProjectType> supportedProjectTypes() {
        return Set.of(ProjectType.TEXT_CLASSIFICATION_SIMPLE, ProjectType.TEXT_CLASSIFICATION_MULTILABEL);
    }

    @Override
    public Set<MetricType> supportedMetricTypes() {
        return Set.of(MetricType.COHENS_KAPPA, MetricType.KRIPPENDORFFS_ALPHA, MetricType.FLEISS_KAPPA);
    }

    @Override
    public NominalAnnotationData transform(AnnotationCalculationContext context) {
        Map<Long, Map<AnnotationUnitKey, String>> valuesByAnnotator = initializeAnnotatorMap(context);
        Map<AnnotationUnitKey, List<String>> ratingsByUnit = new LinkedHashMap<>();
        Set<String> categories = new LinkedHashSet<>();

        for (Annotation annotation : context.annotations()) {
            if (!isUsableAnnotation(annotation)) {
                continue;
            }

            Long annotatorId = annotation.getUser().getId();
            AnnotationUnitKey unitKey = new AnnotationUnitKey(
                    annotation.getDatasetItem().getId(),
                    annotation.getStepIndex());
            String category = extractCategory(context.projectType(), annotation.getPayload());
            if (category == null) {
                continue;
            }

            valuesByAnnotator
                    .computeIfAbsent(annotatorId, ignored -> new LinkedHashMap<>())
                    .put(unitKey, category);
            ratingsByUnit
                    .computeIfAbsent(unitKey, ignored -> new ArrayList<>())
                    .add(category);
            categories.add(category);
        }

        List<AnnotatorAnnotationVector<AnnotationUnitKey, String>> annotatorVectors = valuesByAnnotator.entrySet()
                .stream()
                .map(entry -> new AnnotatorAnnotationVector<>(entry.getKey(), entry.getValue()))
                .toList();

        return new NominalAnnotationData(context.projectType(), annotatorVectors, ratingsByUnit, categories);
    }

    private Map<Long, Map<AnnotationUnitKey, String>> initializeAnnotatorMap(AnnotationCalculationContext context) {
        Map<Long, Map<AnnotationUnitKey, String>> valuesByAnnotator = new LinkedHashMap<>();
        for (ProjectParticipant annotator : context.annotators()) {
            if (annotator == null || annotator.getUser() == null || annotator.getUser().getId() == null) {
                continue;
            }
            valuesByAnnotator.putIfAbsent(annotator.getUser().getId(), new LinkedHashMap<>());
        }
        return valuesByAnnotator;
    }

    private boolean isUsableAnnotation(Annotation annotation) {
        return annotation != null
                && annotation.getDatasetItem() != null
                && annotation.getDatasetItem().getId() != null
                && annotation.getUser() != null
                && annotation.getUser().getId() != null
                && annotation.getStepIndex() != null
                && annotation.getStepIndex() >= 0;
    }

    private String extractCategory(ProjectType projectType, Object payload) {
        if (payload == null) {
            return null;
        }

        if (projectType == ProjectType.TEXT_CLASSIFICATION_MULTILABEL) {
            return extractMultiLabelCategory(payload);
        }

        return extractSingleLabelCategory(payload);
    }

    private String extractSingleLabelCategory(Object payload) {
        if (payload instanceof Map<?, ?> rawMap) {
            Map<String, Object> payloadMap = ProjectAnnotationUtils.toMutableStringObjectMap(rawMap);
            String label = normalizeCategoryValue(payloadMap.get(ProjectConstants.ANNOTATION_KEY_LABEL));
            if (label != null) {
                return label;
            }

            String binaryValue = normalizeCategoryValue(payloadMap.get(ProjectConstants.ANNOTATION_KEY_BINARY_VALUE));
            if (binaryValue != null) {
                return binaryValue;
            }

            return normalizeCategoryValue(payloadMap.get(ProjectConstants.ANNOTATION_WRAPPED_VALUE_KEY));
        }

        return normalizeCategoryValue(payload);
    }

    private String extractMultiLabelCategory(Object payload) {
        Object rawLabels = payload;
        if (payload instanceof Map<?, ?> rawMap) {
            Map<String, Object> payloadMap = ProjectAnnotationUtils.toMutableStringObjectMap(rawMap);
            rawLabels = payloadMap.get(ProjectConstants.ANNOTATION_KEY_LABELS);
            if (rawLabels == null) {
                rawLabels = payloadMap.get(ProjectConstants.ANNOTATION_KEY_LABEL);
            }
            if (rawLabels == null) {
                rawLabels = payloadMap.get(ProjectConstants.ANNOTATION_WRAPPED_VALUE_KEY);
            }
        }

        List<String> labels = normalizeLabels(rawLabels);
        if (labels.isEmpty()) {
            return null;
        }
        return String.join(MULTILABEL_CATEGORY_SEPARATOR, labels);
    }

    private List<String> normalizeLabels(Object rawLabels) {
        List<String> labels = new ArrayList<>();
        if (rawLabels instanceof List<?> listValue) {
            for (Object entry : listValue) {
                String label = normalizeCategoryValue(entry);
                if (label != null) {
                    labels.add(label);
                }
            }
        } else if (rawLabels instanceof String stringValue && stringValue.contains(",")) {
            for (String entry : stringValue.split(",")) {
                String label = normalizeCategoryValue(entry);
                if (label != null) {
                    labels.add(label);
                }
            }
        } else {
            String label = normalizeCategoryValue(rawLabels);
            if (label != null) {
                labels.add(label);
            }
        }

        return labels.stream()
                .distinct()
                .sorted(Comparator.comparing((String label) -> label.toLowerCase())
                        .thenComparing(Comparator.naturalOrder()))
                .toList();
    }

    private String normalizeCategoryValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Boolean booleanValue) {
            return String.valueOf(booleanValue);
        }

        if (value instanceof Number numberValue) {
            double rawValue = numberValue.doubleValue();
            if (!Double.isFinite(rawValue)) {
                return null;
            }
            return String.valueOf(value);
        }

        return StringUtils.trimToNull(String.valueOf(value));
    }
}
