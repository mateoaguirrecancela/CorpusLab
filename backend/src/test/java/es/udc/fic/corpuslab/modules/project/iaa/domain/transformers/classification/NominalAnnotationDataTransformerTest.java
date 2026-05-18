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

class NominalAnnotationDataTransformerTest {

    private final NominalAnnotationDataTransformer transformer = new NominalAnnotationDataTransformer();

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
