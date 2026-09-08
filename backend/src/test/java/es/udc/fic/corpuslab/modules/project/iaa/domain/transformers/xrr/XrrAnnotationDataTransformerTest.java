package es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.xrr;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaAnnotation;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaAnnotator;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaCalculationContext;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaDatasetItem;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaDatasetStep;
import es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.IaaPayloadUtils;

class XrrAnnotationDataTransformerTest {

    private final XrrAnnotationDataTransformer transformer = new XrrAnnotationDataTransformer();

    @Test
    void transformShouldBuildAnnotatorGroupsAndSkipIncompleteUnits() {
        IaaCalculationContext context = new IaaCalculationContext(
                6L,
                ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                List.of(
                        annotation(1L, 40L, 0, Map.of("label", "A")),
                        annotation(2L, 40L, 0, Map.of("label", "B")),
                        annotation(3L, 40L, 0, Map.of("label", "A")),
                        annotation(1L, 40L, 1, Map.of("label", "A")),
                        annotation(3L, 40L, 1, Map.of("label", "C"))),
                List.of(),
                List.of(annotator(1L), annotator(2L), annotator(3L)),
                Map.of(
                        XrrAnnotationDataTransformer.METADATA_KEY_GROUP_X_ANNOTATOR_IDS, List.of(1L, 2L),
                        XrrAnnotationDataTransformer.METADATA_KEY_GROUP_Y_ANNOTATOR_IDS, List.of(3L)));

        XrrAnnotationData data = transformer.transform(context);

        assertThat(data.groupXSources()).containsExactly("annotator:1", "annotator:2");
        assertThat(data.groupYSources()).containsExactly("annotator:3");
        assertThat(data.groupXRows()).containsExactly(List.of("A", "B"));
        assertThat(data.groupYRows()).containsExactly(List.of("A"));
        assertThat(data.candidateUnitCount()).isEqualTo(2);
        assertThat(data.skippedUnitCount()).isEqualTo(1);
        assertThat(data.totalGroupXSourceCount()).isEqualTo(2);
        assertThat(data.totalGroupYSourceCount()).isEqualTo(1);
    }

    @Test
    void transformShouldUseColumnGroupsAndRemoveInactiveSources() {
        IaaDatasetItem datasetItem = csvDatasetItemWithRows(
                41L,
                0,
                List.of(
                        Map.of("gold", "POS", "empty", "", "pred", "POS"),
                        Map.of("gold", "NEG", "empty", "", "pred", "NEG")));

        IaaCalculationContext context = new IaaCalculationContext(
                7L,
                ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                List.of(),
                List.of(datasetItem),
                List.of(),
                Map.of(
                        XrrAnnotationDataTransformer.METADATA_KEY_GROUP_X_COLUMNS, List.of("gold", "empty"),
                        XrrAnnotationDataTransformer.METADATA_KEY_GROUP_Y_COLUMNS, List.of("pred")));

        XrrAnnotationData data = transformer.transform(context);

        assertThat(data.groupXSources()).containsExactly("column:gold");
        assertThat(data.groupYSources()).containsExactly("column:pred");
        assertThat(data.groupXRows()).containsExactly(List.of("POS"), List.of("NEG"));
        assertThat(data.groupYRows()).containsExactly(List.of("POS"), List.of("NEG"));
        assertThat(data.totalGroupXSourceCount()).isEqualTo(2);
        assertThat(data.totalGroupYSourceCount()).isEqualTo(1);
        assertThat(data.skippedUnitCount()).isZero();
    }

    @Test
    void transformShouldNormalizeNerAnnotationsBeforeBuildingRows() {
        IaaCalculationContext context = new IaaCalculationContext(
                8L,
                ProjectType.NER,
                List.of(
                        annotation(8L, 42L, 0, Map.of(
                                IaaPayloadUtils.NER_ANNOTATION_KEY_ENTITIES, List.of(
                                        Map.of(
                                                IaaPayloadUtils.NER_ANNOTATION_KEY_LABEL, "ORG",
                                                IaaPayloadUtils.NER_ANNOTATION_KEY_TEXT, "OpenAI",
                                                IaaPayloadUtils.NER_ANNOTATION_KEY_START_OFFSET, 10,
                                                IaaPayloadUtils.NER_ANNOTATION_KEY_END_OFFSET, 16),
                                        Map.of(
                                                IaaPayloadUtils.NER_ANNOTATION_KEY_LABEL, "PER",
                                                IaaPayloadUtils.NER_ANNOTATION_KEY_TEXT, "Mateo",
                                                IaaPayloadUtils.NER_ANNOTATION_KEY_START_OFFSET, 0,
                                                IaaPayloadUtils.NER_ANNOTATION_KEY_END_OFFSET, 5)))),
                        annotation(9L, 42L, 0, Map.of(
                                IaaPayloadUtils.NER_ANNOTATION_KEY_ENTITIES, List.of(
                                        Map.of(
                                                IaaPayloadUtils.NER_ANNOTATION_KEY_LABEL, "PER",
                                                IaaPayloadUtils.NER_ANNOTATION_KEY_TEXT, "Mateo",
                                                IaaPayloadUtils.NER_ANNOTATION_KEY_START_OFFSET, 0,
                                                IaaPayloadUtils.NER_ANNOTATION_KEY_END_OFFSET, 5),
                                        Map.of(
                                                IaaPayloadUtils.NER_ANNOTATION_KEY_LABEL, "ORG",
                                                IaaPayloadUtils.NER_ANNOTATION_KEY_TEXT, "ignored",
                                                IaaPayloadUtils.NER_ANNOTATION_KEY_START_OFFSET, 4,
                                                IaaPayloadUtils.NER_ANNOTATION_KEY_END_OFFSET, 4))))),
                List.of(),
                List.of(annotator(8L), annotator(9L)),
                Map.of(
                        XrrAnnotationDataTransformer.METADATA_KEY_GROUP_X_ANNOTATOR_IDS, List.of(8L),
                        XrrAnnotationDataTransformer.METADATA_KEY_GROUP_Y_ANNOTATOR_IDS, List.of(9L)));

        XrrAnnotationData data = transformer.transform(context);

        assertThat(data.groupXRows()).containsExactly(List.of("0:5:PER||10:16:ORG"));
        assertThat(data.groupYRows()).containsExactly(List.of("0:5:PER"));
        assertThat(data.skippedUnitCount()).isZero();
    }

    @Test
    void supportedTypesShouldExposeSimpleMultilabelNerAndXrr() {
        assertThat(transformer.supportedProjectTypes()).containsExactlyInAnyOrder(
                ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                ProjectType.TEXT_CLASSIFICATION_MULTILABEL,
                ProjectType.NER);
        assertThat(transformer.supportedMetricTypes())
                .containsExactly(es.udc.fic.corpuslab.modules.project.shared.enums.MetricType.XRR);
    }

    @Test
    void transformShouldFallBackToContextAnnotatorsWhenNoMetadataConfigured() {
        IaaCalculationContext context = new IaaCalculationContext(
                10L,
                ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                List.of(
                        annotation(1L, 50L, 0, Map.of("label", "A")),
                        annotation(2L, 50L, 0, Map.of("label", "A"))),
                List.of(),
                List.of(annotator(1L), annotator(2L)),
                Map.of());

        XrrAnnotationData data = transformer.transform(context);

        assertThat(data.groupXSources()).containsExactly("annotator:1", "annotator:2");
        assertThat(data.groupYSources()).isEmpty();
    }

    @Test
    void transformShouldFallBackToAnnotationAnnotatorsWhenNoAnnotatorsProvided() {
        IaaCalculationContext context = new IaaCalculationContext(
                11L,
                ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                List.of(
                        annotation(1L, 51L, 0, Map.of("label", "A")),
                        annotation(2L, 51L, 0, Map.of("label", "A"))),
                List.of(),
                List.of(),
                Map.of());

        XrrAnnotationData data = transformer.transform(context);

        assertThat(data.groupXSources()).containsExactlyInAnyOrder("annotator:1", "annotator:2");
    }

    @Test
    void transformShouldNormalizeMultilabelAnnotationsFromDifferentPayloadShapes() {
        IaaCalculationContext context = new IaaCalculationContext(
                12L,
                ProjectType.TEXT_CLASSIFICATION_MULTILABEL,
                List.of(
                        annotation(20L, 60L, 0, Map.of("labels", List.of("b", "a", "b"))),
                        new IaaAnnotation(60L, 21L, 0, "a,b")),
                List.of(),
                List.of(),
                Map.of(
                        XrrAnnotationDataTransformer.METADATA_KEY_GROUP_X_ANNOTATOR_IDS, List.of(20L),
                        XrrAnnotationDataTransformer.METADATA_KEY_GROUP_Y_ANNOTATOR_IDS, List.of(21L)));

        XrrAnnotationData data = transformer.transform(context);

        assertThat(data.groupXRows()).containsExactly(List.of("a||b"));
        assertThat(data.groupYRows()).containsExactly(List.of("a||b"));
    }

    @Test
    void transformShouldSkipAnnotationsWithNullNormalizedValue() {
        IaaCalculationContext context = new IaaCalculationContext(
                13L,
                ProjectType.TEXT_CLASSIFICATION_MULTILABEL,
                List.of(annotation(30L, 61L, 0, Map.of("unrelatedKey", "value"))),
                List.of(),
                List.of(),
                Map.of(XrrAnnotationDataTransformer.METADATA_KEY_GROUP_X_ANNOTATOR_IDS, List.of(30L)));

        XrrAnnotationData data = transformer.transform(context);

        assertThat(data.groupXRows()).isEmpty();
    }

    @Test
    void transformShouldSkipDatasetItemsWithNullId() {
        IaaDatasetItem itemWithNullId = new IaaDatasetItem(null, 0, List.of(), true);
        IaaDatasetItem validItem = csvDatasetItemWithRows(
                62L, 0, List.of(Map.of("gold", "A", "pred", "A")));

        IaaCalculationContext context = new IaaCalculationContext(
                14L,
                ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                List.of(),
                List.of(itemWithNullId, validItem),
                List.of(),
                Map.of(
                        XrrAnnotationDataTransformer.METADATA_KEY_GROUP_X_COLUMNS, List.of("gold"),
                        XrrAnnotationDataTransformer.METADATA_KEY_GROUP_Y_COLUMNS, List.of("pred")));

        XrrAnnotationData data = transformer.transform(context);

        assertThat(data.groupXRows()).containsExactly(List.of("A"));
    }

    @Test
    void transformShouldReturnNullForUnmatchedColumn() {
        IaaDatasetItem datasetItem = csvDatasetItemWithRows(
                63L, 0, List.of(Map.of("gold", "A")));

        IaaCalculationContext context = new IaaCalculationContext(
                15L,
                ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                List.of(),
                List.of(datasetItem),
                List.of(),
                Map.of(
                        XrrAnnotationDataTransformer.METADATA_KEY_GROUP_X_COLUMNS, List.of("gold"),
                        XrrAnnotationDataTransformer.METADATA_KEY_GROUP_Y_COLUMNS, List.of("missing-column")));

        XrrAnnotationData data = transformer.transform(context);

        assertThat(data.groupYSources()).isEmpty();
        assertThat(data.skippedUnitCount()).isEqualTo(1);
    }

    private IaaAnnotation annotation(Long annotatorId, Long datasetItemId, int stepIndex, Map<String, Object> payload) {
        return new IaaAnnotation(datasetItemId, annotatorId, stepIndex, payload);
    }

    private IaaAnnotator annotator(Long userId) {
        return new IaaAnnotator(userId);
    }

    private IaaDatasetItem csvDatasetItemWithRows(Long id, int itemIndex, List<Map<String, String>> rows) {
        List<IaaDatasetStep> steps = java.util.stream.IntStream.range(0, rows.size())
                .mapToObj(stepIndex -> new IaaDatasetStep(stepIndex, "", rows.get(stepIndex), ""))
                .toList();
        return new IaaDatasetItem(id, itemIndex, steps, true);
    }
}
