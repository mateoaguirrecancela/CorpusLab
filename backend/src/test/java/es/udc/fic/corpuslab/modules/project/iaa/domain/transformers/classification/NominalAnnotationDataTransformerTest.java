package es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.classification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.AnnotationUnitKey;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaAnnotation;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaAnnotator;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaCalculationContext;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaDatasetItem;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaDatasetStep;
import es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.IaaPayloadUtils;

class NominalAnnotationDataTransformerTest {

    private final NominalAnnotationDataTransformer transformer = new NominalAnnotationDataTransformer();

    @Test
    void supportedProjectTypesShouldIncludeNerForSelectionOfFragments() {
        assertThat(transformer.supportedProjectTypes()).containsExactlyInAnyOrder(
                ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                ProjectType.TEXT_CLASSIFICATION_MULTILABEL,
                ProjectType.NER);
    }

    @Test
    void transformShouldCanonicalizeNerSpansIntoStrictMatchCategories() {
        IaaDatasetItem datasetItem = datasetItemWithId(42L);

        IaaCalculationContext context = new IaaCalculationContext(
                8L,
                ProjectType.NER,
                List.of(
                        annotation(8L, 42L, 0, Map.of(
                                IaaPayloadUtils.NER_ANNOTATION_KEY_ENTITIES, List.of(
                                        Map.of(
                                                IaaPayloadUtils.NER_ANNOTATION_KEY_LABEL, "ORG",
                                                IaaPayloadUtils.NER_ANNOTATION_KEY_START_OFFSET, 10,
                                                IaaPayloadUtils.NER_ANNOTATION_KEY_END_OFFSET, 16),
                                        Map.of(
                                                IaaPayloadUtils.NER_ANNOTATION_KEY_LABEL, "PER",
                                                IaaPayloadUtils.NER_ANNOTATION_KEY_START_OFFSET, 0,
                                                IaaPayloadUtils.NER_ANNOTATION_KEY_END_OFFSET, 5)))),
                        annotation(9L, 42L, 0, Map.of(
                                IaaPayloadUtils.NER_ANNOTATION_KEY_ENTITIES, List.of(
                                        Map.of(
                                                IaaPayloadUtils.NER_ANNOTATION_KEY_LABEL, "PER",
                                                IaaPayloadUtils.NER_ANNOTATION_KEY_START_OFFSET, 0,
                                                IaaPayloadUtils.NER_ANNOTATION_KEY_END_OFFSET, 5),
                                        Map.of(
                                                IaaPayloadUtils.NER_ANNOTATION_KEY_LABEL, "ORG",
                                                IaaPayloadUtils.NER_ANNOTATION_KEY_START_OFFSET, 4,
                                                IaaPayloadUtils.NER_ANNOTATION_KEY_END_OFFSET, 4))))),
                List.of(datasetItem),
                List.of(annotator(8L), annotator(9L)),
                Map.of());

        NominalAnnotationData data = transformer.transform(context);

        // Coincide con la canonicalización de XrrAnnotationDataTransformerTest para el
        // mismo payload: ambos transformadores comparten IaaPayloadUtils.canonicalizeNerEntities.
        assertThat(data.annotatorVectors().get(0).annotationsByUnit())
                .containsEntry(new AnnotationUnitKey(42L, 0), "0:5:PER||10:16:ORG");
        assertThat(data.annotatorVectors().get(1).annotationsByUnit())
                .containsEntry(new AnnotationUnitKey(42L, 0), "0:5:PER");
        assertThat(data.categories()).containsExactlyInAnyOrder("0:5:PER||10:16:ORG", "0:5:PER");
    }

    @Test
    void transformShouldNormalizeSingleLabelPayloadsAndKeepInactiveAnnotators() {
        IaaDatasetItem datasetItem = datasetItemWithId(10L);

        IaaCalculationContext context = new IaaCalculationContext(
                1L,
                ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                List.of(
                        annotation(1L, 10L, 0, Map.of("label", " Positive ")),
                        annotation(2L, 10L, 0, Map.of("isExplanationCorrect", true)),
                        annotation(1L, 10L, 1, Map.of("value", "Maybe")),
                        annotation(2L, 10L, -1, Map.of("label", "ignored"))),
                List.of(datasetItem),
                List.of(
                        annotator(1L),
                        annotator(2L),
                        annotator(3L)),
                Map.of());

        NominalAnnotationData data = transformer.transform(context);

        assertThat(data.annotatorVectors()).extracting(vector -> vector.annotatorId())
                .containsExactly(1L, 2L, 3L);
        assertThat(data.annotatorVectors().get(0).annotationsByUnit())
                .containsEntry(new AnnotationUnitKey(10L, 0), "Positive")
                .containsEntry(new AnnotationUnitKey(10L, 1), "Maybe");
        assertThat(data.annotatorVectors().get(1).annotationsByUnit())
                .containsEntry(new AnnotationUnitKey(10L, 0), "true");
        assertThat(data.annotatorVectors().get(2).annotationsByUnit()).isEmpty();
        assertThat(data.ratingsByUnit())
                .containsEntry(new AnnotationUnitKey(10L, 0), List.of("Positive", "true"))
                .containsEntry(new AnnotationUnitKey(10L, 1), List.of("Maybe"));
        assertThat(data.categories()).containsExactlyInAnyOrder("Positive", "true", "Maybe");
    }

    @Test
    void transformShouldNormalizeAndSortMultilabelPayloads() {
        IaaDatasetItem datasetItem = datasetItemWithId(21L);

        IaaCalculationContext context = new IaaCalculationContext(
                1L,
                ProjectType.TEXT_CLASSIFICATION_MULTILABEL,
                List.of(
                        annotation(1L, 21L, 0, Map.of("labels", List.of("beta", " alpha ", "beta"))),
                        annotation(1L, 21L, 1, Map.of("value", "science,  ai,science "))),
                List.of(datasetItem),
                List.of(annotator(1L)),
                Map.of());

        NominalAnnotationData data = transformer.transform(context);

        assertThat(data.annotatorVectors()).hasSize(1);
        assertThat(data.annotatorVectors().getFirst().annotationsByUnit())
                .containsEntry(new AnnotationUnitKey(21L, 0), "alpha||beta")
                .containsEntry(new AnnotationUnitKey(21L, 1), "ai||science");
        assertThat(data.categories()).containsExactlyInAnyOrder("alpha||beta", "ai||science");
    }

    @Test
    void transformShouldSkipAnnotatorsWithoutUserId() {
        IaaDatasetItem datasetItem = datasetItemWithId(30L);
        List<IaaAnnotator> annotators = List.of(annotator(1L), new IaaAnnotator(null));

        IaaCalculationContext context = new IaaCalculationContext(
                2L,
                ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                List.of(annotation(1L, 30L, 0, Map.of("label", "A"))),
                List.of(datasetItem),
                annotators,
                Map.of());

        NominalAnnotationData data = transformer.transform(context);

        assertThat(data.annotatorVectors()).hasSize(1);
        assertThat(data.annotatorVectors().getFirst().annotatorId()).isEqualTo(1L);
    }

    @Test
    void transformShouldSkipAnnotationsWithNullPayload() {
        IaaDatasetItem datasetItem = datasetItemWithId(31L);
        IaaAnnotation nullPayloadAnnotation = new IaaAnnotation(31L, 1L, 0, null);

        IaaCalculationContext context = new IaaCalculationContext(
                3L,
                ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                List.of(nullPayloadAnnotation),
                List.of(datasetItem),
                List.of(annotator(1L)),
                Map.of());

        NominalAnnotationData data = transformer.transform(context);

        assertThat(data.annotatorVectors().getFirst().annotationsByUnit()).isEmpty();
        assertThat(data.categories()).isEmpty();
    }

    @Test
    void transformShouldNormalizeNumericAndNonFiniteCategoryValues() {
        IaaDatasetItem datasetItem = datasetItemWithId(32L);

        IaaCalculationContext context = new IaaCalculationContext(
                4L,
                ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                List.of(
                        annotation(1L, 32L, 0, Map.of("label", 7)),
                        annotation(1L, 32L, 1, Map.of("label", Double.NaN))),
                List.of(datasetItem),
                List.of(annotator(1L)),
                Map.of());

        NominalAnnotationData data = transformer.transform(context);

        assertThat(data.annotatorVectors().getFirst().annotationsByUnit())
                .containsEntry(new AnnotationUnitKey(32L, 0), "7")
                .doesNotContainKey(new AnnotationUnitKey(32L, 1));
    }

    @Test
    void transformShouldUseSingularLabelKeyAndPlainScalarForMultilabel() {
        IaaDatasetItem datasetItem = datasetItemWithId(33L);

        IaaCalculationContext context = new IaaCalculationContext(
                5L,
                ProjectType.TEXT_CLASSIFICATION_MULTILABEL,
                List.of(
                        annotation(1L, 33L, 0, Map.of("label", "solo")),
                        new IaaAnnotation(33L, 1L, 1, "raw-scalar")),
                List.of(datasetItem),
                List.of(annotator(1L)),
                Map.of());

        NominalAnnotationData data = transformer.transform(context);

        assertThat(data.annotatorVectors().getFirst().annotationsByUnit())
                .containsEntry(new AnnotationUnitKey(33L, 0), "solo")
                .containsEntry(new AnnotationUnitKey(33L, 1), "raw-scalar");
    }

    private IaaAnnotation annotation(Long annotatorId, Long datasetItemId, int stepIndex, Map<String, Object> payload) {
        return new IaaAnnotation(datasetItemId, annotatorId, stepIndex, payload);
    }

    private IaaAnnotator annotator(Long userId) {
        return new IaaAnnotator(userId);
    }

    private IaaDatasetItem datasetItemWithId(Long id) {
        return new IaaDatasetItem(id, 0, List.of(new IaaDatasetStep(0, "", Map.of(), "")), false);
    }
}
