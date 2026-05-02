package es.udc.fic.corpuslab.modules.project.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import es.udc.fic.corpuslab.modules.auth.api.AuthApiService;
import es.udc.fic.corpuslab.modules.auth.api.dtos.UserInfo;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserTestBuilder;
import es.udc.fic.corpuslab.modules.notification.services.NotificationService;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectAnnotationWorkspaceDto;
import es.udc.fic.corpuslab.modules.project.dtos.SaveProjectAnnotationStepRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.SaveProjectAnnotationStepResponseDto;
import es.udc.fic.corpuslab.modules.project.entities.Annotation;
import es.udc.fic.corpuslab.modules.project.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.entities.Project;
import es.udc.fic.corpuslab.modules.project.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.enums.ProjectParticipantRole;
import es.udc.fic.corpuslab.modules.project.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.fixtures.ProjectParticipantTestBuilder;
import es.udc.fic.corpuslab.modules.project.fixtures.ProjectTestBuilder;
import es.udc.fic.corpuslab.modules.project.repositories.AnnotationRepository;
import es.udc.fic.corpuslab.modules.project.repositories.DatasetItemRepository;
import es.udc.fic.corpuslab.modules.project.repositories.ProjectParticipantRepository;
import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class ProjectAnnotationServiceImplTest {

    @Mock private ProjectParticipantRepository projectParticipantRepository;
    @Mock private DatasetItemRepository datasetItemRepository;
    @Mock private AnnotationRepository annotationRepository;
    @Mock private AuthApiService authApiService;
    @Mock private NotificationService notificationService;
    @Mock private EntityManager entityManager;

    private ProjectAnnotationService projectAnnotationService;

    @BeforeEach
    void setUp() {
        projectAnnotationService = new ProjectAnnotationServiceImpl(
                projectParticipantRepository, datasetItemRepository,
                annotationRepository, authApiService, notificationService, entityManager);
    }

    @Test
    void getAnnotationWorkspace_ShouldReturnWorkspace_WhenUserIsParticipant() {
        User user = UserTestBuilder.validUser().build();
        setId(user, 1L);

        when(authApiService.findUserByEmail(any()))
                .thenReturn(new UserInfo(1L, user.getEmail(), user.getFirstName(), user.getLastName()));

        Project project = ProjectTestBuilder.validProject().build();
        setProjectId(project, 100L);

        ProjectParticipant participant = ProjectParticipantTestBuilder.validParticipant()
                .withRole(ProjectParticipantRole.PARTICIPANT).withProject(project).withUser(user).build();

        when(projectParticipantRepository.findByProjectIdAndUserId(100L, 1L)).thenReturn(Optional.of(participant));
        when(projectParticipantRepository.findByProjectIdOrderByRoleAscUserLastNameAscUserFirstNameAsc(100L)).thenReturn(List.of(participant));

        DatasetItem item = new DatasetItem();
        item.setItemIndex(0);
        item.setContent(Map.of("text", "Hello World"));
        setId(item, 50L);

        when(datasetItemRepository.findByProjectIdOrderByItemIndexAsc(100L)).thenReturn(List.of(item));

        Annotation annotation = new Annotation();
        annotation.setDatasetItem(item);
        annotation.setUser(user);
        annotation.setStepIndex(0);
        annotation.setPayload(Map.of("label", "positive"));

        when(annotationRepository.findByDatasetItemProjectId(100L)).thenReturn(List.of(annotation));

        ProjectAnnotationWorkspaceDto workspace = projectAnnotationService.getAnnotationWorkspace(user.getEmail(), 100L, 0, 50);

        assertThat(workspace.totalSteps()).isEqualTo(1);
        assertThat(workspace.steps()).hasSize(1);
        assertThat(workspace.completionPercentage()).isEqualTo(100);
    }

    @Test
    void getParticipantAnnotationWorkspaceForCreator_ShouldThrowException_WhenRequesterIsNotCreator() {
        User requester = UserTestBuilder.validUser().build();
        setId(requester, 1L);

        when(authApiService.findUserByEmail(any()))
                .thenReturn(new UserInfo(1L, requester.getEmail(), requester.getFirstName(), requester.getLastName()));

        ProjectParticipant requesterParticipant = ProjectParticipantTestBuilder.validParticipant()
                .withRole(ProjectParticipantRole.PARTICIPANT).build();

        when(projectParticipantRepository.findByProjectIdAndUserId(100L, 1L)).thenReturn(Optional.of(requesterParticipant));

        assertThatThrownBy(() -> projectAnnotationService.getParticipantAnnotationWorkspaceForCreator(requester.getEmail(), 100L, 2L, 0, 50))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void saveAnnotationStep_ShouldSaveAnnotation_AndReturnCompletionProgress() {
        User user = UserTestBuilder.validUser().build();
        setId(user, 1L);

        when(authApiService.findUserByEmail(any()))
                .thenReturn(new UserInfo(1L, user.getEmail(), user.getFirstName(), user.getLastName()));
        when(entityManager.getReference(User.class, 1L)).thenReturn(user);

        Project project = ProjectTestBuilder.validProject().withProjectType(ProjectType.TEXT_CLASSIFICATION_SIMPLE).build();
        setProjectId(project, 100L);

        ProjectParticipant participant = ProjectParticipantTestBuilder.validParticipant()
                .withRole(ProjectParticipantRole.PARTICIPANT).withProject(project).withUser(user).build();

        when(projectParticipantRepository.findByProjectIdAndUserId(100L, 1L)).thenReturn(Optional.of(participant));
        when(projectParticipantRepository.findByProjectIdOrderByRoleAscUserLastNameAscUserFirstNameAsc(100L)).thenReturn(List.of(participant));

        DatasetItem item = new DatasetItem();
        item.setItemIndex(0);
        item.setContent(Map.of("text", "Hello World"));
        setId(item, 50L);
        Annotation expectedAnnotation = new Annotation();
        expectedAnnotation.setDatasetItem(item);
        expectedAnnotation.setUser(user);
        expectedAnnotation.setStepIndex(0);
        expectedAnnotation.setPayload(Map.of("label", "test"));
        
        when(datasetItemRepository.findByProjectIdOrderByItemIndexAsc(100L)).thenReturn(List.of(item));
        when(annotationRepository.findByDatasetItemProjectId(100L)).thenReturn(List.of(), List.of(expectedAnnotation));

        SaveProjectAnnotationStepRequestDto request = new SaveProjectAnnotationStepRequestDto(50L, 0, Map.of("label", "test"));
        
        SaveProjectAnnotationStepResponseDto response = projectAnnotationService.saveAnnotationStep(user.getEmail(), 100L, request);

        assertThat(response.participantCompletionPercentage()).isEqualTo(100);
        verify(annotationRepository).save(any());
    }

    private void setId(Object entity, Long id) {
        try {
            java.lang.reflect.Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {}
    }

    private void setProjectId(Project project, Long id) {
        try {
            java.lang.reflect.Field field = Project.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(project, id);
        } catch (Exception e) {}
    }
}
