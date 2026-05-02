package es.udc.fic.corpuslab.modules.project.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
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
import es.udc.fic.corpuslab.modules.project.dtos.CreateProjectRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectAssignedSummaryDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSummaryDto;
import es.udc.fic.corpuslab.modules.project.dtos.UpdateProjectRequestDto;
import es.udc.fic.corpuslab.modules.project.entities.Project;
import es.udc.fic.corpuslab.modules.project.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.enums.ProjectParticipantRole;
import es.udc.fic.corpuslab.modules.project.fixtures.ProjectParticipantTestBuilder;
import es.udc.fic.corpuslab.modules.project.fixtures.ProjectTestBuilder;
import es.udc.fic.corpuslab.modules.project.repositories.AnnotationRepository;
import es.udc.fic.corpuslab.modules.project.repositories.DatasetItemRepository;
import es.udc.fic.corpuslab.modules.project.repositories.ProjectParticipantRepository;
import es.udc.fic.corpuslab.modules.project.repositories.ProjectRepository;
import es.udc.fic.corpuslab.modules.researchgroup.api.ResearchGroupApiService;
import es.udc.fic.corpuslab.modules.researchgroup.api.dtos.ResearchGroupInfo;
import es.udc.fic.corpuslab.modules.researchgroup.api.dtos.ResearchGroupMemberInfo;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroup;
import es.udc.fic.corpuslab.modules.researchgroup.fixtures.ResearchGroupTestBuilder;
import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock private AuthApiService authApiService;
    @Mock private ResearchGroupApiService researchGroupApiService;
    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectParticipantRepository projectParticipantRepository;
    @Mock private DatasetItemRepository datasetItemRepository;
    @Mock private AnnotationRepository annotationRepository;
    @Mock private NotificationService notificationService;
    @Mock private ProjectParticipantService projectParticipantService;
    @Mock private EntityManager entityManager;

    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectServiceImpl(
                projectRepository, projectParticipantRepository,
                datasetItemRepository, annotationRepository,
                authApiService, researchGroupApiService,
                notificationService, projectParticipantService, entityManager);
    }

    @Test
    void createProject_ShouldCreateProjectAndAssignCreator_WhenUserIsOwner() {
        when(authApiService.findUserByEmail("owner@example.com"))
                .thenReturn(new UserInfo(1L, "owner@example.com", "Owner", "User"));
        when(researchGroupApiService.findGroupById(10L))
                .thenReturn(Optional.of(new ResearchGroupInfo(10L, "Test Group", "Description")));
        when(researchGroupApiService.findActiveMember(10L, 1L))
                .thenReturn(Optional.of(new ResearchGroupMemberInfo(1L, "OWNER")));

        ResearchGroup groupRef = ResearchGroupTestBuilder.validGroup().build();
        setGroupId(groupRef, 10L);
        when(entityManager.getReference(ResearchGroup.class, 10L)).thenReturn(groupRef);

        User userRef = UserTestBuilder.validUser().build();
        setId(userRef, 1L);
        when(entityManager.getReference(User.class, 1L)).thenReturn(userRef);

        when(projectRepository.save(any())).thenAnswer(invocation -> {
            Project p = invocation.getArgument(0);
            setProjectId(p, 100L);
            return p;
        });

        CreateProjectRequestDto request = new CreateProjectRequestDto("New Project", "Desc");
        ProjectSummaryDto result = projectService.createProject("owner@example.com", 10L, request);

        assertThat(result.name()).isEqualTo("New Project");
        verify(projectRepository).save(any(Project.class));
        verify(projectParticipantRepository).save(any(ProjectParticipant.class));
    }

    @Test
    void createProject_ShouldThrowAccessDenied_WhenUserIsJustAnnotator() {
        when(authApiService.findUserByEmail("annotator@example.com"))
                .thenReturn(new UserInfo(1L, "annotator@example.com", "Ann", "User"));
        when(researchGroupApiService.findGroupById(10L))
                .thenReturn(Optional.of(new ResearchGroupInfo(10L, "Test Group", "Description")));
        when(researchGroupApiService.findActiveMember(10L, 1L))
                .thenReturn(Optional.of(new ResearchGroupMemberInfo(1L, "ANNOTATOR")));

        CreateProjectRequestDto request = new CreateProjectRequestDto("New Project", "Desc");
        assertThatThrownBy(() -> projectService.createProject("annotator@example.com", 10L, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only owners or admins");
    }

    @Test
    void findMyAssignedProjects_ShouldReturnList() {
        User user = UserTestBuilder.validUser().withEmail("user@example.com").build();
        setId(user, 1L);

        when(authApiService.findUserByEmail("user@example.com"))
                .thenReturn(new UserInfo(1L, "user@example.com", "Test", "User"));

        ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
        setGroupId(group, 10L);

        Project project = ProjectTestBuilder.validProject().withResearchGroup(group).build();
        setProjectId(project, 100L);
        ProjectParticipant participant = ProjectParticipantTestBuilder.validParticipant()
                .withUser(user).withProject(project).withRole(ProjectParticipantRole.PARTICIPANT).build();

        when(projectParticipantRepository.findByUserIdOrderByProjectCreatedAtDesc(1L)).thenReturn(List.of(participant));

        List<ProjectAssignedSummaryDto> results = projectService.findMyAssignedProjects("user@example.com");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).participantRole()).isEqualTo(ProjectParticipantRole.PARTICIPANT);
    }

    @Test
    void updateProject_ShouldUpdateNameAndDescription_WhenRequesterIsCreator() {
        User creator = UserTestBuilder.validUser().withEmail("creator@example.com").build();
        setId(creator, 1L);

        when(authApiService.findUserByEmail("creator@example.com"))
                .thenReturn(new UserInfo(1L, "creator@example.com", "Creator", "User"));

        ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
        setGroupId(group, 10L);

        Project project = ProjectTestBuilder.validProject()
                .withName("Old Name")
                .withDescription("Old")
                .withResearchGroup(group)
                .build();
        setProjectId(project, 100L);

        ProjectParticipant creatorParticipant = ProjectParticipantTestBuilder.validParticipant()
                .withUser(creator).withProject(project).withRole(ProjectParticipantRole.CREATOR).build();

        when(projectRepository.findByIdAndResearchGroupId(100L, 10L)).thenReturn(Optional.of(project));
        when(projectParticipantRepository.findByProjectIdAndUserId(100L, 1L)).thenReturn(Optional.of(creatorParticipant));
        when(datasetItemRepository.findByProjectIdOrderByItemIndexAsc(100L)).thenReturn(List.of());

        UpdateProjectRequestDto request = new UpdateProjectRequestDto("New Name", "New Desc", List.of());
        projectService.updateProject("creator@example.com", 10L, 100L, request);

        assertThat(project.getName()).isEqualTo("New Name");
        assertThat(project.getDescription()).isEqualTo("New Desc");
        verify(projectRepository).save(project);
    }

    @Test
    void deleteProject_ShouldCascadeDelete_WhenRequesterIsCreator() {
        User creator = UserTestBuilder.validUser().withEmail("creator@example.com").build();
        setId(creator, 1L);

        when(authApiService.findUserByEmail("creator@example.com"))
                .thenReturn(new UserInfo(1L, "creator@example.com", "Creator", "User"));

        ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
        setGroupId(group, 10L);

        Project project = ProjectTestBuilder.validProject().withResearchGroup(group).build();
        setProjectId(project, 100L);

        ProjectParticipant creatorParticipant = ProjectParticipantTestBuilder.validParticipant()
                .withUser(creator).withProject(project).withRole(ProjectParticipantRole.CREATOR).build();

        when(projectRepository.findByIdAndResearchGroupId(100L, 10L)).thenReturn(Optional.of(project));
        when(projectParticipantRepository.findByProjectIdAndUserId(100L, 1L)).thenReturn(Optional.of(creatorParticipant));

        projectService.deleteProject("creator@example.com", 10L, 100L);

        verify(notificationService).deleteNotificationsByProjectId(100L);
        verify(annotationRepository).deleteByDatasetItemProjectId(100L);
        verify(datasetItemRepository).deleteByProjectId(100L);
        verify(projectParticipantRepository).deleteByProjectId(100L);
        verify(projectRepository).delete(project);
    }

    // Helper methods via reflection
    private void setId(User user, Long id) {
        try {
            java.lang.reflect.Field field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception e) {}
    }

    private void setGroupId(ResearchGroup group, Long id) {
        try {
            java.lang.reflect.Field field = ResearchGroup.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(group, id);
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
