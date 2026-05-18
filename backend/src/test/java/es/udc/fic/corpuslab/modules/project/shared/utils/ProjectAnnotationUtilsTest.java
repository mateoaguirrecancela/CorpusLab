package es.udc.fic.corpuslab.modules.project.shared.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserTestBuilder;
import es.udc.fic.corpuslab.modules.project.shared.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.shared.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.shared.exceptions.InvalidProjectDatasetException;
import es.udc.fic.corpuslab.modules.project.fixtures.ProjectParticipantTestBuilder;

class ProjectAnnotationUtilsTest {

    @Test
    void normalizeNerAnnotationPayloadShouldTrimAndDeduplicateEntities() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(ProjectConstants.NER_ANNOTATION_KEY_ENTITIES, List.of(
                Map.of(
                        ProjectConstants.NER_ANNOTATION_KEY_LABEL, " PERSON ",
                        ProjectConstants.NER_ANNOTATION_KEY_TEXT, "John",
                        ProjectConstants.NER_ANNOTATION_KEY_START_OFFSET, "6",
                        ProjectConstants.NER_ANNOTATION_KEY_END_OFFSET, 10),
                Map.of(
                        ProjectConstants.NER_ANNOTATION_KEY_LABEL, "person",
                        ProjectConstants.NER_ANNOTATION_KEY_TEXT, "John",
                        ProjectConstants.NER_ANNOTATION_KEY_START_OFFSET, 6,
                        ProjectConstants.NER_ANNOTATION_KEY_END_OFFSET, 10)));
        payload.put(ProjectConstants.ANNOTATION_KEY_NOTES, "  reviewed  ");

        Map<String, Object> normalized = ProjectAnnotationUtils.normalizeNerAnnotationPayload(payload);
        @SuppressWarnings("unchecked")
        Map<String, Object> normalizedEntity = (Map<String, Object>) ((List<?>) normalized
                .get(ProjectConstants.NER_ANNOTATION_KEY_ENTITIES)).getFirst();

        assertThat(normalized).containsEntry(ProjectConstants.ANNOTATION_KEY_NOTES, "reviewed");
        assertThat((List<?>) normalized.get(ProjectConstants.NER_ANNOTATION_KEY_ENTITIES)).hasSize(1);
        assertThat(normalizedEntity)
                .containsEntry(ProjectConstants.NER_ANNOTATION_KEY_LABEL, "PERSON")
                .containsEntry(ProjectConstants.NER_ANNOTATION_KEY_TEXT, "John")
                .containsEntry(ProjectConstants.NER_ANNOTATION_KEY_START_OFFSET, 6)
                .containsEntry(ProjectConstants.NER_ANNOTATION_KEY_END_OFFSET, 10);
    }

    @Test
    void normalizeNerAnnotationPayloadShouldRejectLengthMismatches() {
        Map<String, Object> payload = Map.of(
                ProjectConstants.NER_ANNOTATION_KEY_ENTITIES, List.of(Map.of(
                        ProjectConstants.NER_ANNOTATION_KEY_LABEL, "ORG",
                        ProjectConstants.NER_ANNOTATION_KEY_TEXT, "OpenAI",
                        ProjectConstants.NER_ANNOTATION_KEY_START_OFFSET, 0,
                        ProjectConstants.NER_ANNOTATION_KEY_END_OFFSET, 3)));

        assertThatThrownBy(() -> ProjectAnnotationUtils.normalizeNerAnnotationPayload(payload))
                .isInstanceOf(InvalidProjectDatasetException.class)
                .hasMessageContaining("selected text length");
    }

    @Test
    void buildProjectProgressSnapshotShouldCountCompletedStepsAndPercentages() {
        User firstUser = userWithId(1L, "first@example.com");
        User secondUser = userWithId(2L, "second@example.com");
        ProjectParticipant firstParticipant = ProjectParticipantTestBuilder.validParticipant().withUser(firstUser).build();
        ProjectParticipant secondParticipant = ProjectParticipantTestBuilder.validParticipant()
                .withUser(secondUser)
                .build();

        DatasetItem csvDatasetItem = datasetItemWithId(
                100L,
                0,
                "rows.csv",
                "text/csv",
                "text,label\nrow-1,A\nrow-2,B");
        DatasetItem textDatasetItem = datasetItemWithId(
                101L,
                1,
                "note.txt",
                "text/plain",
                "single step");

        Map<Integer, Object> userTwoTextAnnotations = new LinkedHashMap<>();
        userTwoTextAnnotations.put(0, null);

        Map<Long, Map<Long, Map<Integer, Object>>> annotationLookup = Map.of(
                100L, Map.of(
                        1L, Map.of(0, Map.of("label", "A"), 1, " "),
                        2L, Map.of(1, Map.of("label", "B"))),
                101L, Map.of(
                        1L, Map.of(0, List.of("done")),
                        2L, userTwoTextAnnotations));

        ProjectAnnotationUtils.ProjectProgressSnapshot snapshot = ProjectAnnotationUtils.buildProjectProgressSnapshot(
                List.of(firstParticipant, secondParticipant),
                List.of(csvDatasetItem, textDatasetItem),
                annotationLookup);

        assertThat(snapshot.totalSteps()).isEqualTo(3);
        assertThat(snapshot.completedStepsForUser(1L)).isEqualTo(2);
        assertThat(snapshot.completedStepsForUser(2L)).isEqualTo(1);
        assertThat(snapshot.completionPercentageForUser(1L)).isEqualTo(67);
        assertThat(snapshot.completionPercentageForUser(2L)).isEqualTo(33);
        assertThat(snapshot.projectCompletionPercentage()).isEqualTo(50);
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
