package es.udc.fic.corpuslab.modules.project.iaa.transformers.classification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserTestBuilder;
import es.udc.fic.corpuslab.modules.project.entities.Annotation;
import es.udc.fic.corpuslab.modules.project.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.fixtures.ProjectParticipantTestBuilder;
import es.udc.fic.corpuslab.modules.project.iaa.context.AnnotationCalculationContext;
import es.udc.fic.corpuslab.modules.project.iaa.model.AnnotationUnitKey;

class NominalAnnotationDataTransformerTest {

    private final NominalAnnotationDataTransformer transformer = new NominalAnnotationDataTransformer();

    @Test
    void transformShouldNormalizeSingleLabelPayloadsAndKeepInactiveAnnotators() {
        User firstUser = userWithId(1L, "first@example.com");
        User secondUser = userWithId(2L, "second@example.com");
        User thirdUser = userWithId(3L, "third@example.com");
        DatasetItem datasetItem = datasetItemWithId(10L);

        AnnotationCalculationContext context = new AnnotationCalculationContext(
                1L,
                ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                List.of(
                        annotation(firstUser, datasetItem, 0, Map.of("label", " Positive ")),
                        annotation(secondUser, datasetItem, 0, Map.of("isExplanationCorrect", true)),
                        annotation(firstUser, datasetItem, 1, Map.of("value", "Maybe")),
                        annotation(secondUser, datasetItem, -1, Map.of("label", "ignored"))),
                List.of(datasetItem),
                List.of(
                        participant(firstUser),
                        participant(secondUser),
                        participant(thirdUser)),
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
        User user = userWithId(1L, "annotator@example.com");
        DatasetItem datasetItem = datasetItemWithId(21L);

        AnnotationCalculationContext context = new AnnotationCalculationContext(
                1L,
                ProjectType.TEXT_CLASSIFICATION_MULTILABEL,
                List.of(
                        annotation(user, datasetItem, 0, Map.of("labels", List.of("beta", " alpha ", "beta"))),
                        annotation(user, datasetItem, 1, Map.of("value", "science,  ai,science "))),
                List.of(datasetItem),
                List.of(participant(user)),
                Map.of());

        NominalAnnotationData data = transformer.transform(context);

        assertThat(data.annotatorVectors()).hasSize(1);
        assertThat(data.annotatorVectors().getFirst().annotationsByUnit())
                .containsEntry(new AnnotationUnitKey(21L, 0), "alpha||beta")
                .containsEntry(new AnnotationUnitKey(21L, 1), "ai||science");
        assertThat(data.categories()).containsExactlyInAnyOrder("alpha||beta", "ai||science");
    }

    private Annotation annotation(User user, DatasetItem datasetItem, int stepIndex, Map<String, Object> payload) {
        Annotation annotation = new Annotation();
        annotation.setUser(user);
        annotation.setDatasetItem(datasetItem);
        annotation.setStepIndex(stepIndex);
        annotation.setPayload(payload);
        return annotation;
    }

    private ProjectParticipant participant(User user) {
        return ProjectParticipantTestBuilder.validParticipant().withUser(user).build();
    }

    private User userWithId(Long id, String email) {
        User user = UserTestBuilder.validUser().withEmail(email).build();
        setField(user, "id", id);
        return user;
    }

    private DatasetItem datasetItemWithId(Long id) {
        DatasetItem datasetItem = new DatasetItem();
        setField(datasetItem, "id", id);
        datasetItem.setItemIndex(0);
        datasetItem.setContent(Map.of());
        return datasetItem;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
