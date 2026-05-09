package es.udc.fic.corpuslab.modules.project.iaa.transformers.ner;

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
import es.udc.fic.corpuslab.modules.project.iaa.calculators.ner.SpanOverlapUnit;
import es.udc.fic.corpuslab.modules.project.iaa.context.AnnotationCalculationContext;
import es.udc.fic.corpuslab.modules.project.iaa.model.AnnotationUnitKey;
import es.udc.fic.corpuslab.modules.project.utils.ProjectConstants;

class NerSpanAnnotationDataTransformerTest {

    private final NerSpanAnnotationDataTransformer transformer = new NerSpanAnnotationDataTransformer();

    @Test
    void transformShouldUseConfiguredTargetColumnAndIgnoreInvalidEntities() {
        User firstUser = userWithId(1L, "first@example.com");
        User secondUser = userWithId(2L, "second@example.com");
        DatasetItem datasetItem = datasetItemWithId(
                30L,
                0,
                "rows.csv",
                "text/csv",
                "Text,Type\nHello John,PERSON");

        AnnotationCalculationContext context = new AnnotationCalculationContext(
                4L,
                ProjectType.NER,
                List.of(annotation(
                        firstUser,
                        datasetItem,
                        0,
                        Map.of(ProjectConstants.NER_ANNOTATION_KEY_ENTITIES, List.of(
                                Map.of(
                                        ProjectConstants.NER_ANNOTATION_KEY_LABEL, "PERSON",
                                        ProjectConstants.NER_ANNOTATION_KEY_TEXT, "John",
                                        ProjectConstants.NER_ANNOTATION_KEY_START_OFFSET, "6",
                                        ProjectConstants.NER_ANNOTATION_KEY_END_OFFSET, 10),
                                Map.of(
                                        ProjectConstants.NER_ANNOTATION_KEY_LABEL, "ORG",
                                        ProjectConstants.NER_ANNOTATION_KEY_TEXT, "Bad",
                                        ProjectConstants.NER_ANNOTATION_KEY_START_OFFSET, 4,
                                        ProjectConstants.NER_ANNOTATION_KEY_END_OFFSET, 4))))),
                List.of(datasetItem),
                List.of(participant(firstUser), participant(secondUser)),
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
    void transformShouldFallBackToDecodedTextDatasetContent() {
        User user = userWithId(5L, "annotator@example.com");
        DatasetItem datasetItem = datasetItemWithId(
                31L,
                1,
                "document.txt",
                "text/plain",
                "Hello Jane");

        AnnotationCalculationContext context = new AnnotationCalculationContext(
                5L,
                ProjectType.NER,
                List.of(annotation(
                        user,
                        datasetItem,
                        0,
                        Map.of(ProjectConstants.NER_ANNOTATION_KEY_ENTITIES, List.of(Map.of(
                                ProjectConstants.NER_ANNOTATION_KEY_LABEL, "PERSON",
                                ProjectConstants.NER_ANNOTATION_KEY_TEXT, "Jane",
                                ProjectConstants.NER_ANNOTATION_KEY_START_OFFSET, 6,
                                ProjectConstants.NER_ANNOTATION_KEY_END_OFFSET, 10))))),
                List.of(datasetItem),
                List.of(participant(user)),
                Map.of());

        NerSpanAnnotationData data = transformer.transform(context);

        NerSpanAnnotationUnit annotationUnit = data.annotatorVectors().getFirst().annotationsByUnit()
                .get(new AnnotationUnitKey(31L, 0));
        assertThat(annotationUnit.sourceText()).isEqualTo("Hello Jane");
        assertThat(annotationUnit.overlapUnit()).isEqualTo(SpanOverlapUnit.CHARACTER);
        assertThat(annotationUnit.spans()).containsExactly(new NerSpan("PERSON", "Jane", 6, 10));
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
