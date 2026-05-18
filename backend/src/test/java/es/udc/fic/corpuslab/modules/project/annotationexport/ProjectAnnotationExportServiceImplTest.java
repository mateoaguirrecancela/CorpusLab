package es.udc.fic.corpuslab.modules.project.annotationexport;

import static org.assertj.core.api.Assertions.assertThat;
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
                .isEqualTo("text,meta,2_annotation,2_coment\nHello,1,POS,reviewed\n");
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
