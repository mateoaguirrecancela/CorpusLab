package es.udc.fic.corpuslab.modules.project.api.impl;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import es.udc.fic.corpuslab.modules.notification.services.NotificationService;
import es.udc.fic.corpuslab.modules.project.entities.Project;
import es.udc.fic.corpuslab.modules.project.fixtures.ProjectTestBuilder;
import es.udc.fic.corpuslab.modules.project.repositories.AnnotationRepository;
import es.udc.fic.corpuslab.modules.project.repositories.DatasetItemRepository;
import es.udc.fic.corpuslab.modules.project.repositories.ProjectParticipantRepository;
import es.udc.fic.corpuslab.modules.project.repositories.ProjectRepository;

@ExtendWith(MockitoExtension.class)
class ProjectApiServiceImplTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectParticipantRepository projectParticipantRepository;
    @Mock private DatasetItemRepository datasetItemRepository;
    @Mock private AnnotationRepository annotationRepository;
    @Mock private NotificationService notificationService;

    private ProjectApiServiceImpl projectApiService;

    @BeforeEach
    void setUp() {
        projectApiService = new ProjectApiServiceImpl(
                projectRepository,
                projectParticipantRepository,
                datasetItemRepository,
                annotationRepository,
                notificationService);
    }

    @Test
    void countProjectsByResearchGroupIdShouldDelegateToRepository() {
        when(projectRepository.countByResearchGroupIdAndArchivedFalse(7L)).thenReturn(4L);

        long result = projectApiService.countProjectsByResearchGroupId(7L);

        org.assertj.core.api.Assertions.assertThat(result).isEqualTo(4L);
        verify(projectRepository).countByResearchGroupIdAndArchivedFalse(7L);
    }

    @Test
    void deleteAllProjectsByResearchGroupIdShouldDeleteProjectDataInOrderForEachProject() {
        Project firstProject = projectWithId(11L);
        Project secondProject = projectWithId(12L);
        when(projectRepository.findByResearchGroupId(7L)).thenReturn(List.of(firstProject, secondProject));

        projectApiService.deleteAllProjectsByResearchGroupId(7L);

        InOrder inOrder = inOrder(
                projectRepository,
                projectParticipantRepository,
                datasetItemRepository,
                annotationRepository,
                notificationService);

        inOrder.verify(projectRepository).findByResearchGroupId(7L);

        verifyDeletionOrder(inOrder, firstProject);
        verifyDeletionOrder(inOrder, secondProject);
    }

    private void verifyDeletionOrder(InOrder inOrder, Project project) {
        Long projectId = project.getId();
        inOrder.verify(notificationService).deleteNotificationsByProjectId(projectId);
        inOrder.verify(annotationRepository).deleteByDatasetItemProjectId(projectId);
        inOrder.verify(datasetItemRepository).deleteByProjectId(projectId);
        inOrder.verify(projectParticipantRepository).deleteByProjectId(projectId);
        inOrder.verify(projectRepository).delete(project);
    }

    private Project projectWithId(Long id) {
        Project project = ProjectTestBuilder.validProject().build();
        setField(project, "id", id);
        return project;
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
