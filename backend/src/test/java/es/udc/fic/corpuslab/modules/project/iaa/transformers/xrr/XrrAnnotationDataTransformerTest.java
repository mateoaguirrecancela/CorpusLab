package es.udc.fic.corpuslab.modules.project.iaa.transformers.xrr;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
import es.udc.fic.corpuslab.modules.project.utils.ProjectConstants;

class XrrAnnotationDataTransformerTest {

    private final XrrAnnotationDataTransformer transformer = new XrrAnnotationDataTransformer();

    @Test
    void transformShouldBuildAnnotatorGroupsAndSkipIncompleteUnits() {
        User userOne = userWithId(1L, "one@example.com");
        User userTwo = userWithId(2L, "two@example.com");
        User userThree = userWithId(3L, "three@example.com");
        DatasetItem datasetItem = datasetItemWithId(40L, 0, "item.txt", "text/plain", "irrelevant");

        AnnotationCalculationContext context = new AnnotationCalculationContext(
                6L,
                ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                List.of(
                        annotation(userOne, datasetItem, 0, Map.of("label", "A")),
                        annotation(userTwo, datasetItem, 0, Map.of("label", "B")),
                        annotation(userThree, datasetItem, 0, Map.of("label", "A")),
                        annotation(userOne, datasetItem, 1, Map.of("label", "A")),
                        annotation(userThree, datasetItem, 1, Map.of("label", "C"))),
                List.of(),
                List.of(participant(userOne), participant(userTwo), participant(userThree)),
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
        DatasetItem datasetItem = datasetItemWithId(
                41L,
                0,
                "rows.csv",
                "text/csv",
                "gold,empty,pred\nPOS,,POS\nNEG,,NEG");

        AnnotationCalculationContext context = new AnnotationCalculationContext(
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
        User leftAnnotator = userWithId(8L, "left@example.com");
        User rightAnnotator = userWithId(9L, "right@example.com");
        DatasetItem datasetItem = datasetItemWithId(42L, 0, "note.txt", "text/plain", "entity text");

        AnnotationCalculationContext context = new AnnotationCalculationContext(
                8L,
                ProjectType.NER,
                List.of(
                        annotation(leftAnnotator, datasetItem, 0, Map.of(
                                ProjectConstants.NER_ANNOTATION_KEY_ENTITIES, List.of(
                                        Map.of(
                                                ProjectConstants.NER_ANNOTATION_KEY_LABEL, "ORG",
                                                ProjectConstants.NER_ANNOTATION_KEY_TEXT, "OpenAI",
                                                ProjectConstants.NER_ANNOTATION_KEY_START_OFFSET, 10,
                                                ProjectConstants.NER_ANNOTATION_KEY_END_OFFSET, 16),
                                        Map.of(
                                                ProjectConstants.NER_ANNOTATION_KEY_LABEL, "PER",
                                                ProjectConstants.NER_ANNOTATION_KEY_TEXT, "Mateo",
                                                ProjectConstants.NER_ANNOTATION_KEY_START_OFFSET, 0,
                                                ProjectConstants.NER_ANNOTATION_KEY_END_OFFSET, 5)))),
                        annotation(rightAnnotator, datasetItem, 0, Map.of(
                                ProjectConstants.NER_ANNOTATION_KEY_ENTITIES, List.of(
                                        Map.of(
                                                ProjectConstants.NER_ANNOTATION_KEY_LABEL, "PER",
                                                ProjectConstants.NER_ANNOTATION_KEY_TEXT, "Mateo",
                                                ProjectConstants.NER_ANNOTATION_KEY_START_OFFSET, 0,
                                                ProjectConstants.NER_ANNOTATION_KEY_END_OFFSET, 5),
                                        Map.of(
                                                ProjectConstants.NER_ANNOTATION_KEY_LABEL, "ORG",
                                                ProjectConstants.NER_ANNOTATION_KEY_TEXT, "ignored",
                                                ProjectConstants.NER_ANNOTATION_KEY_START_OFFSET, 4,
                                                ProjectConstants.NER_ANNOTATION_KEY_END_OFFSET, 4))))),
                List.of(datasetItem),
                List.of(participant(leftAnnotator), participant(rightAnnotator)),
                Map.of(
                        XrrAnnotationDataTransformer.METADATA_KEY_GROUP_X_ANNOTATOR_IDS, List.of(8L),
                        XrrAnnotationDataTransformer.METADATA_KEY_GROUP_Y_ANNOTATOR_IDS, List.of(9L)));

        XrrAnnotationData data = transformer.transform(context);

        assertThat(data.groupXRows()).containsExactly(List.of("0:5:PER||10:16:ORG"));
        assertThat(data.groupYRows()).containsExactly(List.of("0:5:PER"));
        assertThat(data.skippedUnitCount()).isZero();
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

    private DatasetItem datasetItemWithId(Long id, int itemIndex, String fileName, String mimeType, String content) {
        DatasetItem datasetItem = new DatasetItem();
        setField(datasetItem, "id", id);
        datasetItem.setItemIndex(itemIndex);
        datasetItem.setContent(Map.of(
                ProjectConstants.CONTENT_KEY_FILE_NAME, fileName,
                ProjectConstants.CONTENT_KEY_MIME_TYPE, mimeType,
                ProjectConstants.CONTENT_KEY_BASE64, Base64.getEncoder()
                        .encodeToString(content.getBytes(StandardCharsets.UTF_8))));
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
