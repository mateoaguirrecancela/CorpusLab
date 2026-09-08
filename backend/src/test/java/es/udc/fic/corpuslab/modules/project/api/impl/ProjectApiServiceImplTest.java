package es.udc.fic.corpuslab.modules.project.api.impl;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import es.udc.fic.corpuslab.modules.notification.services.NotificationService;
import es.udc.fic.corpuslab.modules.project.shared.repositories.AnnotationRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.DatasetItemRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.ProjectParticipantRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.ProjectRepository;
import es.udc.fic.corpuslab.modules.project.participant.ProjectParticipantService;

@ExtendWith(MockitoExtension.class)
class ProjectApiServiceImplTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectParticipantRepository projectParticipantRepository;
    @Mock private DatasetItemRepository datasetItemRepository;
    @Mock private AnnotationRepository annotationRepository;
    @Mock private NotificationService notificationService;
    @Mock private ProjectParticipantService projectParticipantService;

    private ProjectApiServiceImpl projectApiService;

    @BeforeEach
    void setUp() {
        projectApiService = new ProjectApiServiceImpl(
                projectRepository,
                projectParticipantRepository,
                datasetItemRepository,
                annotationRepository,
                notificationService,
                projectParticipantService);
    }

    @Test
    void countProjectsByResearchGroupIdShouldDelegateToRepository() {
        when(projectRepository.countByResearchGroupIdAndArchivedFalse(7L)).thenReturn(4L);

        long result = projectApiService.countProjectsByResearchGroupId(7L);

        org.assertj.core.api.Assertions.assertThat(result).isEqualTo(4L);
        verify(projectRepository).countByResearchGroupIdAndArchivedFalse(7L);
    }

    @Test
    void deleteAllProjectsByResearchGroupIdShouldBulkDeleteProjectDataInOrder() {
        projectApiService.deleteAllProjectsByResearchGroupId(7L);

        InOrder inOrder = inOrder(
                projectRepository,
                projectParticipantRepository,
                datasetItemRepository,
                annotationRepository,
                notificationService);

        inOrder.verify(notificationService).deleteNotificationsByResearchGroupId(7L);
        inOrder.verify(annotationRepository).deleteByResearchGroupId(7L);
        inOrder.verify(datasetItemRepository).deleteByProjectResearchGroupId(7L);
        inOrder.verify(projectParticipantRepository).deleteByProjectResearchGroupId(7L);
        inOrder.verify(projectRepository).deleteByResearchGroupId(7L);
    }

    @Test
    void removeParticipantFromAllGroupProjectsShouldDelegateToParticipantService() {
        projectApiService.removeParticipantFromAllGroupProjects(7L, 3L);

        verify(projectParticipantService).removeParticipantFromAllGroupProjects(7L, 3L);
    }
}
