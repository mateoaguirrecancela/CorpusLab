package es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.ner;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import es.udc.fic.corpuslab.common.utils.StringUtils;
import es.udc.fic.corpuslab.modules.project.shared.enums.MetricType;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.iaa.domain.calculators.ner.SpanOverlapUnit;
import es.udc.fic.corpuslab.modules.project.iaa.domain.calculators.pairwise.AnnotatorAnnotationVector;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.AnnotationUnitKey;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaAnnotation;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaAnnotator;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaCalculationContext;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaDatasetItem;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaDatasetStep;
import es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.AnnotationDataTransformer;
import es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.IaaPayloadUtils;

public class NerSpanAnnotationDataTransformer implements AnnotationDataTransformer<NerSpanAnnotationData> {

    @Override
    public Set<ProjectType> supportedProjectTypes() {
        return Set.of(ProjectType.NER);
    }

    @Override
    public Set<MetricType> supportedMetricTypes() {
        return Set.of(MetricType.SPAN_OVERLAP_F1);
    }

    @Override
    public NerSpanAnnotationData transform(IaaCalculationContext context) {
        SpanOverlapUnit overlapUnit = SpanOverlapUnit.fromMetadata(
                context.metadata().get(SpanOverlapUnit.METADATA_KEY));
        Map<AnnotationUnitKey, String> sourceTextByUnit = buildSourceTextByUnit(context);
        Map<Long, Map<AnnotationUnitKey, NerSpanAnnotationUnit>> valuesByAnnotator = initializeAnnotatorMap(context);

        for (IaaAnnotation annotation : context.annotations()) {
            if (!isUsableAnnotation(annotation)) {
                continue;
            }

            List<NerSpan> spans = extractSpans(annotation.payload());
            if (spans.isEmpty()) {
                continue;
            }

            AnnotationUnitKey unitKey = new AnnotationUnitKey(
                    annotation.datasetItemId(),
                    annotation.stepIndex());
            String sourceText = sourceTextByUnit.getOrDefault(unitKey, "");
            Long annotatorId = annotation.annotatorId();

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
            IaaCalculationContext context) {
        Map<Long, Map<AnnotationUnitKey, NerSpanAnnotationUnit>> valuesByAnnotator = new LinkedHashMap<>();
        for (IaaAnnotator annotator : context.annotators()) {
            if (annotator == null || annotator.userId() == null) {
                continue;
            }
            valuesByAnnotator.putIfAbsent(annotator.userId(), new LinkedHashMap<>());
        }
        return valuesByAnnotator;
    }

    private Map<AnnotationUnitKey, String> buildSourceTextByUnit(IaaCalculationContext context) {
        Map<AnnotationUnitKey, String> sourceTextByUnit = new LinkedHashMap<>();
        for (IaaDatasetItem datasetItem : context.datasetItems()) {
            if (datasetItem == null || datasetItem.id() == null) {
                continue;
            }

            for (IaaDatasetStep step : datasetItem.steps()) {
                AnnotationUnitKey unitKey = new AnnotationUnitKey(datasetItem.id(), step.stepIndex());
                sourceTextByUnit.put(unitKey, step.sourceText());
            }
        }

        return sourceTextByUnit;
    }

    private boolean isUsableAnnotation(IaaAnnotation annotation) {
        return annotation != null
                && annotation.datasetItemId() != null
                && annotation.annotatorId() != null
                && annotation.stepIndex() != null
                && annotation.stepIndex() >= 0;
    }

    private List<NerSpan> extractSpans(Object payload) {
        if (!(payload instanceof Map<?, ?> rawPayload)) {
            return List.of();
        }

        Map<String, Object> payloadMap = IaaPayloadUtils.toMutableStringObjectMap(rawPayload);
        Object rawEntities = payloadMap.get(IaaPayloadUtils.NER_ANNOTATION_KEY_ENTITIES);
        if (!(rawEntities instanceof List<?> rawEntitiesList)) {
            return List.of();
        }

        List<NerSpan> spans = new ArrayList<>();
        for (Object rawEntity : rawEntitiesList) {
            if (!(rawEntity instanceof Map<?, ?> rawEntityMap)) {
                continue;
            }

            Map<String, Object> entity = IaaPayloadUtils.toMutableStringObjectMap(rawEntityMap);
            String label = StringUtils.trimToNull(
                    IaaPayloadUtils.valueAsString(entity.get(IaaPayloadUtils.NER_ANNOTATION_KEY_LABEL)));
            String text = IaaPayloadUtils.valueAsString(entity.get(IaaPayloadUtils.NER_ANNOTATION_KEY_TEXT));
            Integer startOffset = IaaPayloadUtils.parseOffsetValue(
                    entity.get(IaaPayloadUtils.NER_ANNOTATION_KEY_START_OFFSET));
            Integer endOffset = IaaPayloadUtils.parseOffsetValue(
                    entity.get(IaaPayloadUtils.NER_ANNOTATION_KEY_END_OFFSET));

            if (label == null || text.isBlank() || startOffset == null || endOffset == null || endOffset <= startOffset) {
                continue;
            }

            spans.add(new NerSpan(label, text, startOffset, endOffset));
        }

        return spans;
    }
}
