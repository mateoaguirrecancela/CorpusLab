package es.udc.fic.corpuslab.modules.project.annotationexport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import es.udc.fic.corpuslab.modules.auth.api.AuthApiService;
import es.udc.fic.corpuslab.modules.auth.api.dtos.UserInfo;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserTestBuilder;
import es.udc.fic.corpuslab.modules.project.shared.entities.Annotation;
import es.udc.fic.corpuslab.modules.project.shared.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.shared.entities.Project;
import es.udc.fic.corpuslab.modules.project.shared.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectParticipantRole;
import es.udc.fic.corpuslab.modules.project.fixtures.ProjectParticipantTestBuilder;
import es.udc.fic.corpuslab.modules.project.fixtures.ProjectTestBuilder;
import es.udc.fic.corpuslab.modules.project.shared.repositories.AnnotationRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.DatasetItemRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.ProjectParticipantRepository;
import es.udc.fic.corpuslab.modules.project.shared.utils.ProjectConstants;

@ExtendWith(MockitoExtension.class)
class ProjectAnnotationExportServiceImplTest {

    @Mock
    private ProjectParticipantRepository projectParticipantRepository;

    @Mock
    private DatasetItemRepository datasetItemRepository;

    @Mock
    private AnnotationRepository annotationRepository;

    @Mock
    private AuthApiService authApiService;

    @Test
    void getAnnotationResultsCsvFileNameShouldSlugProjectName() {
        Project project = projectWithId("NER Demo: Phase 1", 100L);
        User creator = userWithId(1L, "creator@example.com");
        ProjectParticipant creatorParticipant = participant(project, creator, ProjectParticipantRole.CREATOR);
        ProjectAnnotationExportServiceImpl exportService = service();

        when(authApiService.findUserByEmail("creator@example.com"))
                .thenReturn(new UserInfo(1L, "creator@example.com", "Creator", "User"));
        when(projectParticipantRepository.findByProjectIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(creatorParticipant));

        String fileName = exportService.getAnnotationResultsCsvFileName("creator@example.com", 100L);

        assertThat(fileName).isEqualTo("ner-demo-phase-1-annotations.csv");
    }

    @Test
    void writeAnnotationResultsCsvShouldExportCsvRowsAndNonEmptyAnnotatorColumns() throws Exception {
        Project project = projectWithId("Exportable", 100L);
        User creator = userWithId(1L, "creator@example.com");
        User annotator = userWithId(2L, "annotator@example.com");
        ProjectParticipant creatorParticipant = participant(project, creator, ProjectParticipantRole.CREATOR);
        ProjectParticipant annotatorParticipant = participant(project, annotator, ProjectParticipantRole.PARTICIPANT);
        DatasetItem datasetItem = csvDatasetItem(project, 50L);
        Annotation annotation = annotation(datasetItem, annotator, Map.of(
                ProjectConstants.ANNOTATION_KEY_LABEL, "POS",
                ProjectConstants.ANNOTATION_KEY_NOTES, "reviewed"));
        ProjectAnnotationExportServiceImpl exportService = service();

        when(authApiService.findUserByEmail("creator@example.com"))
                .thenReturn(new UserInfo(1L, "creator@example.com", "Creator", "User"));
        when(projectParticipantRepository.findByProjectIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(creatorParticipant));
        when(datasetItemRepository.findByProjectIdOrderByItemIndexAsc(100L)).thenReturn(List.of(datasetItem));
        when(annotationRepository.findByProjectIdWithDatasetItemAndUser(100L)).thenReturn(List.of(annotation));
        when(projectParticipantRepository.findByProjectIdWithUserAndProject(100L))
                .thenReturn(List.of(creatorParticipant, annotatorParticipant));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        exportService.writeAnnotationResultsCsv("creator@example.com", 100L, outputStream);

        assertThat(outputStream.toString(StandardCharsets.UTF_8))
                .isEqualTo("text,meta,2_annotation,2_comment\nHello,1,POS,reviewed\n");
    }

    @Test
    void getAnnotationResultsCsvFileNameShouldDenyAccessWhenRequesterIsNotCreator() {
        Project project = projectWithId("Restricted", 100L);
        User participantUser = userWithId(2L, "participant@example.com");
        ProjectParticipant nonCreatorParticipant = participant(project, participantUser, ProjectParticipantRole.PARTICIPANT);
        ProjectAnnotationExportServiceImpl exportService = service();

        when(authApiService.findUserByEmail("participant@example.com"))
                .thenReturn(new UserInfo(2L, "participant@example.com", "Part", "User"));
        when(projectParticipantRepository.findByProjectIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(nonCreatorParticipant));

        assertThatThrownBy(() -> exportService.getAnnotationResultsCsvFileName("participant@example.com", 100L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getAnnotationResultsCsvFileNameShouldFallBackToProjectIdWhenNameIsBlank() {
        Project project = projectWithId("   ", 105L);
        User creator = userWithId(1L, "creator@example.com");
        ProjectParticipant creatorParticipant = participant(project, creator, ProjectParticipantRole.CREATOR);
        ProjectAnnotationExportServiceImpl exportService = service();

        when(authApiService.findUserByEmail("creator@example.com"))
                .thenReturn(new UserInfo(1L, "creator@example.com", "Creator", "User"));
        when(projectParticipantRepository.findByProjectIdAndUserId(105L, 1L))
                .thenReturn(Optional.of(creatorParticipant));

        String fileName = exportService.getAnnotationResultsCsvFileName("creator@example.com", 105L);

        assertThat(fileName).isEqualTo("project-105-annotations.csv");
    }

    @Test
    void getAnnotationResultsCsvFileNameShouldFallBackToProjectIdWhenNameHasNoAlphanumericCharacters() {
        // Unlike a blank name, this name is non-blank but slugifies to an empty string,
        // which is a distinct branch from the blank-name fallback above.
        Project project = projectWithId("###!!!", 106L);
        User creator = userWithId(1L, "creator@example.com");
        ProjectParticipant creatorParticipant = participant(project, creator, ProjectParticipantRole.CREATOR);
        ProjectAnnotationExportServiceImpl exportService = service();

        when(authApiService.findUserByEmail("creator@example.com"))
                .thenReturn(new UserInfo(1L, "creator@example.com", "Creator", "User"));
        when(projectParticipantRepository.findByProjectIdAndUserId(106L, 1L))
                .thenReturn(Optional.of(creatorParticipant));

        String fileName = exportService.getAnnotationResultsCsvFileName("creator@example.com", 106L);

        assertThat(fileName).isEqualTo("project-106-annotations.csv");
    }

    @Test
    void writeAnnotationResultsCsvShouldIncludeItemIndexAndSourceNameForNonCsvDatasets() throws Exception {
        Project project = projectWithId("Images", 100L);
        User creator = userWithId(1L, "creator@example.com");
        ProjectParticipant creatorParticipant = participant(project, creator, ProjectParticipantRole.CREATOR);
        DatasetItem datasetItem = imageDatasetItem(project, 60L, "photo.png");
        ProjectAnnotationExportServiceImpl exportService = service();

        when(authApiService.findUserByEmail("creator@example.com"))
                .thenReturn(new UserInfo(1L, "creator@example.com", "Creator", "User"));
        when(projectParticipantRepository.findByProjectIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(creatorParticipant));
        when(datasetItemRepository.findByProjectIdOrderByItemIndexAsc(100L)).thenReturn(List.of(datasetItem));
        when(annotationRepository.findByProjectIdWithDatasetItemAndUser(100L)).thenReturn(List.of());
        when(projectParticipantRepository.findByProjectIdWithUserAndProject(100L))
                .thenReturn(List.of(creatorParticipant));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        exportService.writeAnnotationResultsCsv("creator@example.com", 100L, outputStream);

        // No annotations exist, so the creator's own annotator column must be excluded from the export.
        assertThat(outputStream.toString(StandardCharsets.UTF_8))
                .isEqualTo("dataset_item_index,source_name\n0,photo.png\n");
    }

    @Test
    void writeAnnotationResultsCsvShouldExportOnlyHeadersWhenProjectHasNoDatasetItems() throws Exception {
        Project project = projectWithId("Empty", 100L);
        User creator = userWithId(1L, "creator@example.com");
        ProjectParticipant creatorParticipant = participant(project, creator, ProjectParticipantRole.CREATOR);
        ProjectAnnotationExportServiceImpl exportService = service();

        when(authApiService.findUserByEmail("creator@example.com"))
                .thenReturn(new UserInfo(1L, "creator@example.com", "Creator", "User"));
        when(projectParticipantRepository.findByProjectIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(creatorParticipant));
        when(datasetItemRepository.findByProjectIdOrderByItemIndexAsc(100L)).thenReturn(List.of());
        when(annotationRepository.findByProjectIdWithDatasetItemAndUser(100L)).thenReturn(List.of());
        when(projectParticipantRepository.findByProjectIdWithUserAndProject(100L)).thenReturn(List.of());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        exportService.writeAnnotationResultsCsv("creator@example.com", 100L, outputStream);

        // With no dataset items, isCsvDataset() defaults to false (anyMatch on an empty stream),
        // so the header row still includes the item-index/source-name columns even though no rows follow.
        assertThat(outputStream.toString(StandardCharsets.UTF_8))
                .isEqualTo("dataset_item_index,source_name\n");
    }

    @Test
    void writeAnnotationResultsCsvShouldQuoteValuesContainingCommasOrQuotes() throws Exception {
        Project project = projectWithId("Escaping", 100L);
        User creator = userWithId(1L, "creator@example.com");
        User annotator = userWithId(3L, "annotator@example.com");
        ProjectParticipant creatorParticipant = participant(project, creator, ProjectParticipantRole.CREATOR);
        ProjectParticipant annotatorParticipant = participant(project, annotator, ProjectParticipantRole.PARTICIPANT);
        DatasetItem datasetItem = csvDatasetItem(project, 51L);
        Annotation annotation = annotation(datasetItem, annotator, Map.of(
                ProjectConstants.ANNOTATION_KEY_TEXT, "Hello, \"world\""));
        ProjectAnnotationExportServiceImpl exportService = service();

        when(authApiService.findUserByEmail("creator@example.com"))
                .thenReturn(new UserInfo(1L, "creator@example.com", "Creator", "User"));
        when(projectParticipantRepository.findByProjectIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(creatorParticipant));
        when(datasetItemRepository.findByProjectIdOrderByItemIndexAsc(100L)).thenReturn(List.of(datasetItem));
        when(annotationRepository.findByProjectIdWithDatasetItemAndUser(100L)).thenReturn(List.of(annotation));
        when(projectParticipantRepository.findByProjectIdWithUserAndProject(100L))
                .thenReturn(List.of(creatorParticipant, annotatorParticipant));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        exportService.writeAnnotationResultsCsv("creator@example.com", 100L, outputStream);

        assertThat(outputStream.toString(StandardCharsets.UTF_8))
                .isEqualTo("text,meta,3_annotation\nHello,1,\"Hello, \"\"world\"\"\"\n");
    }

    @Test
    void writeAnnotationResultsCsvShouldJoinDistinctMultiLabelValuesWithPipe() throws Exception {
        Project project = projectWithId("MultiLabel", 100L);
        User creator = userWithId(1L, "creator@example.com");
        User annotator = userWithId(4L, "annotator@example.com");
        ProjectParticipant creatorParticipant = participant(project, creator, ProjectParticipantRole.CREATOR);
        ProjectParticipant annotatorParticipant = participant(project, annotator, ProjectParticipantRole.PARTICIPANT);
        DatasetItem datasetItem = csvDatasetItem(project, 52L);
        Annotation annotation = annotation(datasetItem, annotator, Map.of(
                ProjectConstants.ANNOTATION_KEY_LABELS, List.of("POS", "NEG", "POS")));
        ProjectAnnotationExportServiceImpl exportService = service();

        when(authApiService.findUserByEmail("creator@example.com"))
                .thenReturn(new UserInfo(1L, "creator@example.com", "Creator", "User"));
        when(projectParticipantRepository.findByProjectIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(creatorParticipant));
        when(datasetItemRepository.findByProjectIdOrderByItemIndexAsc(100L)).thenReturn(List.of(datasetItem));
        when(annotationRepository.findByProjectIdWithDatasetItemAndUser(100L)).thenReturn(List.of(annotation));
        when(projectParticipantRepository.findByProjectIdWithUserAndProject(100L))
                .thenReturn(List.of(creatorParticipant, annotatorParticipant));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        exportService.writeAnnotationResultsCsv("creator@example.com", 100L, outputStream);

        // Pipe is not a CSV special character, so the joined value is not quoted.
        assertThat(outputStream.toString(StandardCharsets.UTF_8))
                .isEqualTo("text,meta,4_annotation\nHello,1,POS|NEG\n");
    }

    @Test
    void writeAnnotationResultsCsvShouldFallBackToRawPayloadForUnrecognizedBinaryValue() throws Exception {
        Project project = projectWithId("BinaryFallback", 100L);
        User creator = userWithId(1L, "creator@example.com");
        User annotator = userWithId(5L, "annotator@example.com");
        ProjectParticipant creatorParticipant = participant(project, creator, ProjectParticipantRole.CREATOR);
        ProjectParticipant annotatorParticipant = participant(project, annotator, ProjectParticipantRole.PARTICIPANT);
        DatasetItem datasetItem = csvDatasetItem(project, 53L);
        // 2 is neither 0 nor 1, so it cannot be normalized as a boolean and the export
        // falls back to the raw payload map representation instead of losing the value.
        Annotation annotation = annotation(datasetItem, annotator, Map.of(
                ProjectConstants.ANNOTATION_KEY_BINARY_VALUE, 2));
        ProjectAnnotationExportServiceImpl exportService = service();

        when(authApiService.findUserByEmail("creator@example.com"))
                .thenReturn(new UserInfo(1L, "creator@example.com", "Creator", "User"));
        when(projectParticipantRepository.findByProjectIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(creatorParticipant));
        when(datasetItemRepository.findByProjectIdOrderByItemIndexAsc(100L)).thenReturn(List.of(datasetItem));
        when(annotationRepository.findByProjectIdWithDatasetItemAndUser(100L)).thenReturn(List.of(annotation));
        when(projectParticipantRepository.findByProjectIdWithUserAndProject(100L))
                .thenReturn(List.of(creatorParticipant, annotatorParticipant));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        exportService.writeAnnotationResultsCsv("creator@example.com", 100L, outputStream);

        assertThat(outputStream.toString(StandardCharsets.UTF_8))
                .isEqualTo("text,meta,5_annotation\nHello,1,{isExplanationCorrect=2}\n");
    }

    @Test
    void writeAnnotationResultsCsvShouldNormalizeRecognizedBinaryValue() throws Exception {
        Project project = projectWithId("BinaryRecognized", 100L);
        User creator = userWithId(1L, "creator@example.com");
        User annotator = userWithId(8L, "annotator@example.com");
        ProjectParticipant creatorParticipant = participant(project, creator, ProjectParticipantRole.CREATOR);
        ProjectParticipant annotatorParticipant = participant(project, annotator, ProjectParticipantRole.PARTICIPANT);
        DatasetItem datasetItem = csvDatasetItem(project, 56L);
        // 1 is a recognized boolean-like number, unlike the unrecognized "2" used above.
        Annotation annotation = annotation(datasetItem, annotator, Map.of(
                ProjectConstants.ANNOTATION_KEY_BINARY_VALUE, 1));
        ProjectAnnotationExportServiceImpl exportService = service();

        when(authApiService.findUserByEmail("creator@example.com"))
                .thenReturn(new UserInfo(1L, "creator@example.com", "Creator", "User"));
        when(projectParticipantRepository.findByProjectIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(creatorParticipant));
        when(datasetItemRepository.findByProjectIdOrderByItemIndexAsc(100L)).thenReturn(List.of(datasetItem));
        when(annotationRepository.findByProjectIdWithDatasetItemAndUser(100L)).thenReturn(List.of(annotation));
        when(projectParticipantRepository.findByProjectIdWithUserAndProject(100L))
                .thenReturn(List.of(creatorParticipant, annotatorParticipant));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        exportService.writeAnnotationResultsCsv("creator@example.com", 100L, outputStream);

        assertThat(outputStream.toString(StandardCharsets.UTF_8))
                .isEqualTo("text,meta,8_annotation\nHello,1,true\n");
    }

    @Test
    void writeAnnotationResultsCsvShouldJoinNerEntitiesWithLabelAndText() throws Exception {
        Project project = projectWithId("NerExport", 100L);
        User creator = userWithId(1L, "creator@example.com");
        User annotator = userWithId(6L, "annotator@example.com");
        ProjectParticipant creatorParticipant = participant(project, creator, ProjectParticipantRole.CREATOR);
        ProjectParticipant annotatorParticipant = participant(project, annotator, ProjectParticipantRole.PARTICIPANT);
        DatasetItem datasetItem = csvDatasetItem(project, 54L);
        Annotation annotation = annotation(datasetItem, annotator, Map.of(
                ProjectConstants.NER_ANNOTATION_KEY_ENTITIES, List.of(
                        Map.of(
                                ProjectConstants.NER_ANNOTATION_KEY_LABEL, "PER",
                                ProjectConstants.NER_ANNOTATION_KEY_TEXT, "Alice"),
                        Map.of(
                                ProjectConstants.NER_ANNOTATION_KEY_LABEL, "LOC",
                                ProjectConstants.NER_ANNOTATION_KEY_TEXT, "Madrid"))));
        ProjectAnnotationExportServiceImpl exportService = service();

        when(authApiService.findUserByEmail("creator@example.com"))
                .thenReturn(new UserInfo(1L, "creator@example.com", "Creator", "User"));
        when(projectParticipantRepository.findByProjectIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(creatorParticipant));
        when(datasetItemRepository.findByProjectIdOrderByItemIndexAsc(100L)).thenReturn(List.of(datasetItem));
        when(annotationRepository.findByProjectIdWithDatasetItemAndUser(100L)).thenReturn(List.of(annotation));
        when(projectParticipantRepository.findByProjectIdWithUserAndProject(100L))
                .thenReturn(List.of(creatorParticipant, annotatorParticipant));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        exportService.writeAnnotationResultsCsv("creator@example.com", 100L, outputStream);

        assertThat(outputStream.toString(StandardCharsets.UTF_8))
                .isEqualTo("text,meta,6_annotation\nHello,1,PER:Alice|LOC:Madrid\n");
    }

    private ProjectAnnotationExportServiceImpl service() {
        return new ProjectAnnotationExportServiceImpl(
                projectParticipantRepository,
                datasetItemRepository,
                annotationRepository,
                authApiService);
    }

    private Project projectWithId(String name, Long id) {
        Project project = ProjectTestBuilder.validProject().withName(name).build();
        setField(project, "id", id);
        return project;
    }

    private User userWithId(Long id, String email) {
        User user = UserTestBuilder.validUser().withEmail(email).build();
        setField(user, "id", id);
        return user;
    }

    private ProjectParticipant participant(Project project, User user, ProjectParticipantRole role) {
        return ProjectParticipantTestBuilder.validParticipant()
                .withProject(project)
                .withUser(user)
                .withRole(role)
                .build();
    }

    private DatasetItem csvDatasetItem(Project project, Long id) {
        String csv = "text,meta\nHello,1";
        DatasetItem datasetItem = new DatasetItem();
        setField(datasetItem, "id", id);
        datasetItem.setProject(project);
        datasetItem.setItemIndex(0);
        datasetItem.setContent(Map.of(
                ProjectConstants.CONTENT_KEY_FILE_NAME, "rows.csv",
                ProjectConstants.CONTENT_KEY_MIME_TYPE, "text/csv",
                ProjectConstants.CONTENT_KEY_BASE64, Base64.getEncoder()
                        .encodeToString(csv.getBytes(StandardCharsets.UTF_8))));
        return datasetItem;
    }

    private DatasetItem imageDatasetItem(Project project, Long id, String fileName) {
        DatasetItem datasetItem = new DatasetItem();
        setField(datasetItem, "id", id);
        datasetItem.setProject(project);
        datasetItem.setItemIndex(0);
        datasetItem.setContent(Map.of(
                ProjectConstants.CONTENT_KEY_FILE_NAME, fileName,
                ProjectConstants.CONTENT_KEY_MIME_TYPE, "image/png",
                ProjectConstants.CONTENT_KEY_BASE64, Base64.getEncoder()
                        .encodeToString("fake-image-bytes".getBytes(StandardCharsets.UTF_8))));
        return datasetItem;
    }

    private Annotation annotation(DatasetItem datasetItem, User user, Map<String, Object> payload) {
        Annotation annotation = new Annotation();
        annotation.setDatasetItem(datasetItem);
        annotation.setUser(user);
        annotation.setStepIndex(0);
        annotation.setPayload(payload);
        return annotation;
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
