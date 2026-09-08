package es.udc.fic.corpuslab.modules.project.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import es.udc.fic.corpuslab.modules.auth.api.AuthApiService;
import es.udc.fic.corpuslab.modules.auth.api.dtos.UserInfo;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserTestBuilder;
import es.udc.fic.corpuslab.modules.notification.services.NotificationService;
import es.udc.fic.corpuslab.modules.project.core.dtos.CreateProjectRequestDto;
import es.udc.fic.corpuslab.modules.project.core.dtos.ProjectAssignedSummaryDto;
import es.udc.fic.corpuslab.modules.project.core.dtos.ProjectDetailDto;
import es.udc.fic.corpuslab.modules.project.core.dtos.ProjectSummaryDto;
import es.udc.fic.corpuslab.modules.project.core.dtos.UpdateProjectRequestDto;
import es.udc.fic.corpuslab.modules.project.guideline.ProjectGuidelineService;
import es.udc.fic.corpuslab.modules.project.metrics.ProjectMetricsCacheService;
import es.udc.fic.corpuslab.modules.project.participant.ProjectParticipantService;
import es.udc.fic.corpuslab.modules.project.progress.ProjectProgressCalculator;
import es.udc.fic.corpuslab.modules.project.shared.entities.Project;
import es.udc.fic.corpuslab.modules.project.shared.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectParticipantRole;
import es.udc.fic.corpuslab.modules.project.fixtures.ProjectParticipantTestBuilder;
import es.udc.fic.corpuslab.modules.project.fixtures.ProjectTestBuilder;
import es.udc.fic.corpuslab.modules.project.shared.references.ProjectEntityReferenceService;
import es.udc.fic.corpuslab.modules.project.shared.repositories.AnnotationRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.DatasetItemRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.ProjectParticipantRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.ProjectRepository;
import es.udc.fic.corpuslab.modules.researchgroup.api.ResearchGroupApiService;
import es.udc.fic.corpuslab.modules.researchgroup.api.dtos.ResearchGroupInfo;
import es.udc.fic.corpuslab.modules.researchgroup.api.dtos.ResearchGroupMemberInfo;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroup;
import es.udc.fic.corpuslab.modules.researchgroup.fixtures.ResearchGroupTestBuilder;

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
    @Mock private ProjectMetricsCacheService projectMetricsCacheService;
    @Mock private ProjectEntityReferenceService entityReferenceService;
    @Mock private ProjectGuidelineService projectGuidelineService;

    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        ProjectProgressCalculator projectProgressCalculator = new ProjectProgressCalculator(
                projectParticipantRepository,
                datasetItemRepository,
                annotationRepository);
        projectService = new ProjectServiceImpl(
                projectRepository, projectParticipantRepository,
                datasetItemRepository, annotationRepository,
                authApiService, researchGroupApiService,
                notificationService, projectParticipantService, projectMetricsCacheService,
                entityReferenceService, projectProgressCalculator,
                projectGuidelineService,
                30L);
    }

    @Test
    void createProject_ShouldCreateProjectAndAssignCreator_WhenUserIsOwner() {
        when(authApiService.findUserByEmail("owner@example.com"))
                .thenReturn(new UserInfo(1L, "owner@example.com", "Owner", "User"));
        when(researchGroupApiService.findGroupById(10L))
                .thenReturn(Optional.of(new ResearchGroupInfo(10L, "Test Group", "Description")));
        when(researchGroupApiService.findActiveMember(10L, 1L))
                .thenReturn(Optional.of(new ResearchGroupMemberInfo(1L, "OWNER")));

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
    void findMyAssignedProjects_ShouldReturnActiveSlice() {
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

        when(projectParticipantRepository.findByUserIdAndProjectArchivedFalseOrderByProjectCreatedAtDesc(
                eq(1L), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(participant), PageRequest.of(0, 12), false));

        Slice<ProjectAssignedSummaryDto> results = projectService.findMyAssignedProjects(
                "user@example.com",
                0,
                12,
                false);

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).participantRole()).isEqualTo(ProjectParticipantRole.PARTICIPANT);
        assertThat(results.getContent().get(0).archived()).isFalse();
    }

    @Test
    void findMyAssignedProjects_ShouldReturnArchivedProjectsForParticipantsToo() {
        User participantUser = UserTestBuilder.validUser().withEmail("participant@example.com").build();
        setId(participantUser, 1L);

        when(authApiService.findUserByEmail("participant@example.com"))
                .thenReturn(new UserInfo(1L, "participant@example.com", "Participant", "User"));

        ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
        setGroupId(group, 10L);

        Project project = ProjectTestBuilder.validProject().withResearchGroup(group).build();
        project.setArchived(true);
        setProjectId(project, 100L);

        ProjectParticipant archivedParticipant = ProjectParticipantTestBuilder.validParticipant()
                .withUser(participantUser).withProject(project).withRole(ProjectParticipantRole.PARTICIPANT).build();

        when(projectParticipantRepository.findByUserIdAndProjectArchivedTrueOrderByProjectCreatedAtDesc(
                eq(1L), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(archivedParticipant), PageRequest.of(0, 12), false));

        Slice<ProjectAssignedSummaryDto> results = projectService.findMyAssignedProjects(
                "participant@example.com",
                0,
                12,
                true);

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).participantRole()).isEqualTo(ProjectParticipantRole.PARTICIPANT);
        assertThat(results.getContent().get(0).archived()).isTrue();
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
        when(datasetItemRepository.findSummariesByProjectId(100L)).thenReturn(List.of());
        when(projectParticipantRepository.findByProjectIdWithUserAndProject(100L)).thenReturn(List.of(creatorParticipant));
        when(annotationRepository.countCompletedStepsByUser(100L)).thenReturn(List.of());

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

    @Test
    void archiveProject_ShouldArchiveProject_WhenRequesterIsCreator() {
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

        when(projectParticipantRepository.findByProjectIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(creatorParticipant));
        when(projectParticipantRepository.findByProjectIdWithUserAndProject(100L))
                .thenReturn(List.of(creatorParticipant));
        when(datasetItemRepository.findSummariesByProjectId(100L)).thenReturn(List.of());
        when(annotationRepository.countCompletedStepsByUser(100L)).thenReturn(List.of());

        ProjectDetailDto result = projectService.archiveProject("creator@example.com", 100L);

        assertThat(project.isArchived()).isTrue();
        assertThat(result.archived()).isTrue();
        verify(projectRepository).save(project);
    }

    @Test
    void unarchiveProject_ShouldUnarchiveProject_WhenRequesterIsCreator() {
        User creator = UserTestBuilder.validUser().withEmail("creator@example.com").build();
        setId(creator, 1L);

        when(authApiService.findUserByEmail("creator@example.com"))
                .thenReturn(new UserInfo(1L, "creator@example.com", "Creator", "User"));

        ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
        setGroupId(group, 10L);

        Project project = ProjectTestBuilder.validProject().withResearchGroup(group).build();
        project.setArchived(true);
        setProjectId(project, 100L);

        ProjectParticipant creatorParticipant = ProjectParticipantTestBuilder.validParticipant()
                .withUser(creator).withProject(project).withRole(ProjectParticipantRole.CREATOR).build();

        when(projectParticipantRepository.findByProjectIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(creatorParticipant));
        when(projectParticipantRepository.findByProjectIdWithUserAndProject(100L))
                .thenReturn(List.of(creatorParticipant));
        when(datasetItemRepository.findSummariesByProjectId(100L)).thenReturn(List.of());
        when(annotationRepository.countCompletedStepsByUser(100L)).thenReturn(List.of());

        ProjectDetailDto result = projectService.unarchiveProject("creator@example.com", 100L);

        assertThat(project.isArchived()).isFalse();
        assertThat(result.archived()).isFalse();
        verify(projectRepository).save(project);
    }

    @Test
    void archiveProject_ShouldThrowAccessDenied_WhenRequesterIsParticipant() {
        User participantUser = UserTestBuilder.validUser().withEmail("participant@example.com").build();
        setId(participantUser, 2L);

        when(authApiService.findUserByEmail("participant@example.com"))
                .thenReturn(new UserInfo(2L, "participant@example.com", "Participant", "User"));

        ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
        setGroupId(group, 10L);

        Project project = ProjectTestBuilder.validProject().withResearchGroup(group).build();
        setProjectId(project, 100L);

        ProjectParticipant participant = ProjectParticipantTestBuilder.validParticipant()
                .withUser(participantUser).withProject(project).withRole(ProjectParticipantRole.PARTICIPANT).build();

        when(projectParticipantRepository.findByProjectIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(participant));

        assertThatThrownBy(() -> projectService.archiveProject("participant@example.com", 100L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only project creators");
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
