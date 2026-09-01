package es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.ner;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.iaa.domain.calculators.ner.SpanOverlapUnit;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.AnnotationUnitKey;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaAnnotation;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaAnnotator;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaCalculationContext;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaDatasetItem;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaDatasetStep;
import es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.IaaPayloadUtils;

class NerSpanAnnotationDataTransformerTest {

    private final NerSpanAnnotationDataTransformer transformer = new NerSpanAnnotationDataTransformer();

    @Test
    void transformShouldUseConfiguredTargetColumnAndIgnoreInvalidEntities() {
        IaaDatasetItem datasetItem = datasetItemWithId(
                30L,
                0,
                "Hello John");

        IaaCalculationContext context = new IaaCalculationContext(
                4L,
                ProjectType.NER,
                List.of(annotation(
                        1L,
                        30L,
                        0,
                        Map.of(IaaPayloadUtils.NER_ANNOTATION_KEY_ENTITIES, List.of(
                                Map.of(
                                        IaaPayloadUtils.NER_ANNOTATION_KEY_LABEL, "PERSON",
                                        IaaPayloadUtils.NER_ANNOTATION_KEY_TEXT, "John",
                                        IaaPayloadUtils.NER_ANNOTATION_KEY_START_OFFSET, "6",
                                        IaaPayloadUtils.NER_ANNOTATION_KEY_END_OFFSET, 10),
                                Map.of(
                                        IaaPayloadUtils.NER_ANNOTATION_KEY_LABEL, "ORG",
                                        IaaPayloadUtils.NER_ANNOTATION_KEY_TEXT, "Bad",
                                        IaaPayloadUtils.NER_ANNOTATION_KEY_START_OFFSET, 4,
                                        IaaPayloadUtils.NER_ANNOTATION_KEY_END_OFFSET, 4))))),
                List.of(datasetItem),
                List.of(annotator(1L), annotator(2L)),
                Map.of(
                        "annotationTargetColumn", " text ",
                        SpanOverlapUnit.METADATA_KEY, "words"));

        NerSpanAnnotationData data = transformer.transform(context);

        assertThat(data.annotatorVectors()).extracting(vector -> vector.annotatorId())
                .containsExactly(1L, 2L);
        NerSpanAnnotationUnit annotationUnit = data.annotatorVectors().getFirst().annotationsByUnit()
                .get(new AnnotationUnitKey(30L, 0));
        assertThat(annotationUnit.sourceText()).isEqualTo("Hello John");
        assertThat(annotationUnit.overlapUnit()).isEqualTo(SpanOverlapUnit.WORD);
        assertThat(annotationUnit.spans())
                .containsExactly(new NerSpan("PERSON", "John", 6, 10));
        assertThat(data.annotatorVectors().get(1).annotationsByUnit()).isEmpty();
    }

    @Test
    void transformShouldUseAdapterProvidedSourceText() {
        IaaDatasetItem datasetItem = datasetItemWithId(
                31L,
                1,
                "Hello Jane");

        IaaCalculationContext context = new IaaCalculationContext(
                5L,
                ProjectType.NER,
                List.of(annotation(
                        5L,
                        31L,
                        0,
                        Map.of(IaaPayloadUtils.NER_ANNOTATION_KEY_ENTITIES, List.of(Map.of(
                                IaaPayloadUtils.NER_ANNOTATION_KEY_LABEL, "PERSON",
                                IaaPayloadUtils.NER_ANNOTATION_KEY_TEXT, "Jane",
                                IaaPayloadUtils.NER_ANNOTATION_KEY_START_OFFSET, 6,
                                IaaPayloadUtils.NER_ANNOTATION_KEY_END_OFFSET, 10))))),
                List.of(datasetItem),
                List.of(annotator(5L)),
                Map.of());

        NerSpanAnnotationData data = transformer.transform(context);

        NerSpanAnnotationUnit annotationUnit = data.annotatorVectors().getFirst().annotationsByUnit()
                .get(new AnnotationUnitKey(31L, 0));
        assertThat(annotationUnit.sourceText()).isEqualTo("Hello Jane");
        assertThat(annotationUnit.overlapUnit()).isEqualTo(SpanOverlapUnit.CHARACTER);
        assertThat(annotationUnit.spans()).containsExactly(new NerSpan("PERSON", "Jane", 6, 10));
    }

    @Test
    void transformShouldSkipAnnotatorsWithoutUserIdAndDatasetItemsWithoutId() {
        IaaDatasetItem itemWithNullId = new IaaDatasetItem(null, 0, List.of(), false);
        IaaDatasetItem validItem = datasetItemWithId(40L, 0, "Hello Amy");
        List<IaaAnnotator> annotators = List.of(annotator(1L), new IaaAnnotator(null));

        IaaCalculationContext context = new IaaCalculationContext(
                6L,
                ProjectType.NER,
                List.of(annotation(
                        1L,
                        40L,
                        0,
                        Map.of(IaaPayloadUtils.NER_ANNOTATION_KEY_ENTITIES, List.of(Map.of(
                                IaaPayloadUtils.NER_ANNOTATION_KEY_LABEL, "PERSON",
                                IaaPayloadUtils.NER_ANNOTATION_KEY_TEXT, "Amy",
                                IaaPayloadUtils.NER_ANNOTATION_KEY_START_OFFSET, 6,
                                IaaPayloadUtils.NER_ANNOTATION_KEY_END_OFFSET, 9))))),
                List.of(itemWithNullId, validItem),
                annotators,
                Map.of());

        NerSpanAnnotationData data = transformer.transform(context);

        assertThat(data.annotatorVectors()).hasSize(1);
        assertThat(data.annotatorVectors().getFirst().annotationsByUnit()).hasSize(1);
    }

    @Test
    void transformShouldSkipAnnotationsWithoutUsableSpans() {
        IaaDatasetItem datasetItem = datasetItemWithId(41L, 0, "no entities here");

        IaaCalculationContext context = new IaaCalculationContext(
                7L,
                ProjectType.NER,
                List.of(
                        annotation(1L, 41L, 0, Map.of("unrelated", "value")),
                        new IaaAnnotation(41L, 1L, 1, "not-a-map"),
                        annotation(1L, 41L, 2, Map.of(
                                IaaPayloadUtils.NER_ANNOTATION_KEY_ENTITIES, "not-a-list")),
                        annotation(1L, 41L, 3, Map.of(
                                IaaPayloadUtils.NER_ANNOTATION_KEY_ENTITIES, List.of("not-a-map-entity")))),
                List.of(datasetItem),
                List.of(annotator(1L)),
                Map.of());

        NerSpanAnnotationData data = transformer.transform(context);

        assertThat(data.annotatorVectors().getFirst().annotationsByUnit()).isEmpty();
    }

    private IaaAnnotation annotation(Long annotatorId, Long datasetItemId, int stepIndex, Map<String, Object> payload) {
        return new IaaAnnotation(datasetItemId, annotatorId, stepIndex, payload);
    }

    private IaaAnnotator annotator(Long userId) {
        return new IaaAnnotator(userId);
    }

    private IaaDatasetItem datasetItemWithId(Long id, int itemIndex, String sourceText) {
        return new IaaDatasetItem(
                id,
                itemIndex,
                List.of(new IaaDatasetStep(0, sourceText, Map.of(), sourceText)),
                false);
    }
}
