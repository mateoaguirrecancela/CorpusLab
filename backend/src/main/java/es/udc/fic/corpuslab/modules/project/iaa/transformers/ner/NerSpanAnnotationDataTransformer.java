package es.udc.fic.corpuslab.modules.project.iaa.transformers.ner;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import es.udc.fic.corpuslab.common.utils.StringUtils;
import es.udc.fic.corpuslab.modules.project.entities.Annotation;
import es.udc.fic.corpuslab.modules.project.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.enums.MetricType;
import es.udc.fic.corpuslab.modules.project.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.iaa.calculators.ner.SpanOverlapUnit;
import es.udc.fic.corpuslab.modules.project.iaa.calculators.pairwise.AnnotatorAnnotationVector;
import es.udc.fic.corpuslab.modules.project.iaa.context.AnnotationCalculationContext;
import es.udc.fic.corpuslab.modules.project.iaa.model.AnnotationUnitKey;
import es.udc.fic.corpuslab.modules.project.iaa.transformers.AnnotationDataTransformer;
import es.udc.fic.corpuslab.modules.project.utils.ProjectAnnotationUtils;
import es.udc.fic.corpuslab.modules.project.utils.ProjectConstants;
import es.udc.fic.corpuslab.modules.project.utils.ProjectDatasetUtils;

@Component
public class NerSpanAnnotationDataTransformer implements AnnotationDataTransformer<NerSpanAnnotationData> {

    private static final String METADATA_KEY_ANNOTATION_TARGET_COLUMN = "annotationTargetColumn";

    @Override
    public Set<ProjectType> supportedProjectTypes() {
        return Set.of(ProjectType.NER);
    }

    @Override
    public Set<MetricType> supportedMetricTypes() {
        return Set.of(MetricType.SPAN_OVERLAP_F1);
    }

    @Override
    public NerSpanAnnotationData transform(AnnotationCalculationContext context) {
        SpanOverlapUnit overlapUnit = SpanOverlapUnit.fromMetadata(
                context.metadata().get(SpanOverlapUnit.METADATA_KEY));
        Map<AnnotationUnitKey, String> sourceTextByUnit = buildSourceTextByUnit(context);
        Map<Long, Map<AnnotationUnitKey, NerSpanAnnotationUnit>> valuesByAnnotator = initializeAnnotatorMap(context);

        for (Annotation annotation : context.annotations()) {
            if (!isUsableAnnotation(annotation)) {
                continue;
            }

            List<NerSpan> spans = extractSpans(annotation.getPayload());
            if (spans.isEmpty()) {
                continue;
            }

            AnnotationUnitKey unitKey = new AnnotationUnitKey(
                    annotation.getDatasetItem().getId(),
                    annotation.getStepIndex());
            String sourceText = sourceTextByUnit.getOrDefault(unitKey, "");
            Long annotatorId = annotation.getUser().getId();

            valuesByAnnotator
                    .computeIfAbsent(annotatorId, ignored -> new LinkedHashMap<>())
                    .put(unitKey, new NerSpanAnnotationUnit(spans, sourceText, overlapUnit));
        }

        List<AnnotatorAnnotationVector<AnnotationUnitKey, NerSpanAnnotationUnit>> annotatorVectors =
                valuesByAnnotator.entrySet()
                        .stream()
                        .map(entry -> new AnnotatorAnnotationVector<>(entry.getKey(), entry.getValue()))
                        .toList();

        return new NerSpanAnnotationData(context.projectType(), annotatorVectors);
    }

    private Map<Long, Map<AnnotationUnitKey, NerSpanAnnotationUnit>> initializeAnnotatorMap(
            AnnotationCalculationContext context) {
        Map<Long, Map<AnnotationUnitKey, NerSpanAnnotationUnit>> valuesByAnnotator = new LinkedHashMap<>();
        for (ProjectParticipant annotator : context.annotators()) {
            if (annotator == null || annotator.getUser() == null || annotator.getUser().getId() == null) {
                continue;
            }
            valuesByAnnotator.putIfAbsent(annotator.getUser().getId(), new LinkedHashMap<>());
        }
        return valuesByAnnotator;
    }

    private Map<AnnotationUnitKey, String> buildSourceTextByUnit(AnnotationCalculationContext context) {
        Map<AnnotationUnitKey, String> sourceTextByUnit = new LinkedHashMap<>();
        String annotationTargetColumn = StringUtils.trimToNull(
                String.valueOf(context.metadata().getOrDefault(METADATA_KEY_ANNOTATION_TARGET_COLUMN, "")));

        for (DatasetItem datasetItem : context.datasetItems()) {
            if (datasetItem == null || datasetItem.getId() == null) {
                continue;
            }

            ProjectDatasetUtils.DatasetStepDefinition definition = ProjectDatasetUtils.resolveStepDefinition(datasetItem);
            int totalSteps = definition.totalSteps();
            for (int stepIndex = 0; stepIndex < totalSteps; stepIndex++) {
                AnnotationUnitKey unitKey = new AnnotationUnitKey(datasetItem.getId(), stepIndex);
                sourceTextByUnit.put(unitKey, resolveSourceText(datasetItem, definition, stepIndex, annotationTargetColumn));
            }
        }

        return sourceTextByUnit;
    }

    private String resolveSourceText(
            DatasetItem datasetItem,
            ProjectDatasetUtils.DatasetStepDefinition definition,
            int stepIndex,
            String annotationTargetColumn) {
        Map<String, String> rowValues = definition.rowValuesForStep(stepIndex);
        if (rowValues != null && annotationTargetColumn != null) {
            for (Map.Entry<String, String> entry : rowValues.entrySet()) {
                if (entry.getKey() != null
                        && entry.getKey().trim().equalsIgnoreCase(annotationTargetColumn)) {
                    return entry.getValue() == null ? "" : entry.getValue();
                }
            }
        }

        String decodedText = decodeTextDatasetItem(datasetItem);
        if (decodedText != null) {
            return decodedText;
        }

        return definition.previewForStep(stepIndex);
    }

    private String decodeTextDatasetItem(DatasetItem datasetItem) {
        if (datasetItem.getContent() == null) {
            return null;
        }

        String mimeType = ProjectDatasetUtils.valueAsString(
                datasetItem.getContent().get(ProjectConstants.CONTENT_KEY_MIME_TYPE)).toLowerCase();
        String fileName = ProjectDatasetUtils.valueAsString(
                datasetItem.getContent().get(ProjectConstants.CONTENT_KEY_FILE_NAME)).toLowerCase();
        if (!mimeType.startsWith("text/")
                && !mimeType.contains("json")
                && !fileName.endsWith(".txt")
                && !fileName.endsWith(".json")) {
            return null;
        }

        String base64 = ProjectDatasetUtils.valueAsString(
                datasetItem.getContent().get(ProjectConstants.CONTENT_KEY_BASE64));
        if (base64.isBlank()) {
            return null;
        }

        try {
            return new String(ProjectDatasetUtils.decodeStoredBase64(base64), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return null;
        }
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

    private List<NerSpan> extractSpans(Object payload) {
        if (!(payload instanceof Map<?, ?> rawPayload)) {
            return List.of();
        }

        Map<String, Object> payloadMap = ProjectAnnotationUtils.toMutableStringObjectMap(rawPayload);
        Object rawEntities = payloadMap.get(ProjectConstants.NER_ANNOTATION_KEY_ENTITIES);
        if (!(rawEntities instanceof List<?> rawEntitiesList)) {
            return List.of();
        }

        List<NerSpan> spans = new ArrayList<>();
        for (Object rawEntity : rawEntitiesList) {
            if (!(rawEntity instanceof Map<?, ?> rawEntityMap)) {
                continue;
            }

            Map<String, Object> entity = ProjectAnnotationUtils.toMutableStringObjectMap(rawEntityMap);
            String label = StringUtils.trimToNull(
                    ProjectDatasetUtils.valueAsString(entity.get(ProjectConstants.NER_ANNOTATION_KEY_LABEL)));
            String text = ProjectDatasetUtils.valueAsString(entity.get(ProjectConstants.NER_ANNOTATION_KEY_TEXT));
            Integer startOffset = ProjectAnnotationUtils.parseOffsetValue(
                    entity.get(ProjectConstants.NER_ANNOTATION_KEY_START_OFFSET));
            Integer endOffset = ProjectAnnotationUtils.parseOffsetValue(
                    entity.get(ProjectConstants.NER_ANNOTATION_KEY_END_OFFSET));

            if (label == null || text.isBlank() || startOffset == null || endOffset == null || endOffset <= startOffset) {
                continue;
            }

            spans.add(new NerSpan(label, text, startOffset, endOffset));
        }

        return spans;
    }
}
