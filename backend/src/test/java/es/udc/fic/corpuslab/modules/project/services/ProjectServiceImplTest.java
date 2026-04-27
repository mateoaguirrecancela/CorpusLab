package es.udc.fic.corpuslab.modules.project.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserTestBuilder;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.common.utils.FileSecurityService;
import es.udc.fic.corpuslab.modules.notification.repositories.NotificationRepository;
import es.udc.fic.corpuslab.modules.notification.services.EmailService;
import es.udc.fic.corpuslab.modules.notification.services.NotificationService;
import es.udc.fic.corpuslab.modules.project.dtos.UpdateProjectRequestDto;
import es.udc.fic.corpuslab.modules.project.entities.Project;
import es.udc.fic.corpuslab.modules.project.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.enums.ProjectParticipantRole;
import es.udc.fic.corpuslab.modules.project.exceptions.ProjectNotFoundException;
import es.udc.fic.corpuslab.modules.project.fixtures.ProjectParticipantTestBuilder;
import es.udc.fic.corpuslab.modules.project.fixtures.ProjectTestBuilder;
import es.udc.fic.corpuslab.modules.project.repositories.AnnotationRepository;
import es.udc.fic.corpuslab.modules.project.repositories.DatasetItemRepository;
import es.udc.fic.corpuslab.modules.project.repositories.ProjectParticipantRepository;
import es.udc.fic.corpuslab.modules.project.repositories.ProjectRepository;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroup;
import es.udc.fic.corpuslab.modules.researchgroup.fixtures.ResearchGroupTestBuilder;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupMemberDto;
import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupMemberRole;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupMemberRepository;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupRepository;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ResearchGroupRepository researchGroupRepository;

    @Mock
    private ResearchGroupMemberRepository researchGroupMemberRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectParticipantRepository projectParticipantRepository;

    @Mock
    private DatasetItemRepository datasetItemRepository;

    @Mock
    private AnnotationRepository annotationRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private FileSecurityService fileSecurityService;

    @Mock
    private EmailService emailService;

    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectServiceImpl(
                userRepository,
                researchGroupRepository,
                researchGroupMemberRepository,
                projectRepository,
                projectParticipantRepository,
                datasetItemRepository,
                annotationRepository,
                notificationRepository,
                notificationService,
                emailService,
                fileSecurityService,
                "http://localhost:5173");
    }

    @Test
    void updateProjectShouldUpdateNameAndDescriptionWhenRequesterIsCreator() {
        User creator = UserTestBuilder.validUser().withEmail("creator@example.com").build();
        setId(creator, 1L);

        ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
        setGroupId(group, 10L);

        Project project = ProjectTestBuilder.validProject()
                .withName("Old Name")
                .withDescription("Old Description")
                .withResearchGroup(group)
                .build();
        setProjectId(project, 100L);

        ProjectParticipant creatorParticipant = ProjectParticipantTestBuilder.validParticipant()
                .withUser(creator)
                .withProject(project)
                .withRole(ProjectParticipantRole.CREATOR)
                .build();

        when(userRepository.findByEmailIgnoreCase("creator@example.com")).thenReturn(Optional.of(creator));
        when(projectRepository.findByIdAndResearchGroupId(100L, 10L)).thenReturn(Optional.of(project));
        when(projectParticipantRepository.findByProjectIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(creatorParticipant));
        when(datasetItemRepository.findByProjectIdOrderByItemIndexAsc(100L)).thenReturn(List.of());
        when(projectParticipantRepository.findByProjectIdOrderByRoleAscUserLastNameAscUserFirstNameAsc(100L))
                .thenReturn(List.of(creatorParticipant));

        UpdateProjectRequestDto request = new UpdateProjectRequestDto("New Name", "New Description", List.of());
        projectService.updateProject("creator@example.com", 10L, 100L, request);

        assertThat(project.getName()).isEqualTo("New Name");
        assertThat(project.getDescription()).isEqualTo("New Description");
        verify(projectRepository).save(project);
    }

    @Test
    void updateProjectShouldReplaceParticipantsWhenRequesterIsCreator() {
        User creator = UserTestBuilder.validUser().withEmail("creator@example.com").build();
        setId(creator, 1L);

        User existingParticipant = UserTestBuilder.validUser().withEmail("existing@example.com").build();
        setId(existingParticipant, 2L);

        User newParticipant = UserTestBuilder.validUser().withEmail("new@example.com").build();
        setId(newParticipant, 3L);

        ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
        setGroupId(group, 10L);

        Project project = ProjectTestBuilder.validProject().withResearchGroup(group).build();
        setProjectId(project, 100L);

        ProjectParticipant creatorParticipant = ProjectParticipantTestBuilder.validParticipant()
                .withUser(creator)
                .withProject(project)
                .withRole(ProjectParticipantRole.CREATOR)
                .build();

        ProjectParticipant existingParticipantRecord = ProjectParticipantTestBuilder.validParticipant()
                .withUser(existingParticipant)
                .withProject(project)
                .withRole(ProjectParticipantRole.PARTICIPANT)
                .build();

        // Mocks for updateProject authorization and data fetching
        when(userRepository.findByEmailIgnoreCase("creator@example.com")).thenReturn(Optional.of(creator));
        when(projectRepository.findByIdAndResearchGroupId(100L, 10L)).thenReturn(Optional.of(project));
        when(projectParticipantRepository.findByProjectIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(creatorParticipant));

        // Mocks for replaceProjectParticipants
        ResearchGroupMemberDto creatorMemberDto = new ResearchGroupMemberDto(1L, "C", "R", "creator@example.com",
                ResearchGroupMemberRole.OWNER, 0L);
        ResearchGroupMemberDto existingMemberDto = new ResearchGroupMemberDto(2L, "E", "P", "existing@example.com",
                ResearchGroupMemberRole.ANNOTATOR, 0L);
        ResearchGroupMemberDto newMemberDto = new ResearchGroupMemberDto(3L, "N", "P", "new@example.com",
                ResearchGroupMemberRole.ANNOTATOR, 0L);

        when(researchGroupMemberRepository.findMembersByGroupId(10L))
                .thenReturn(List.of(creatorMemberDto, existingMemberDto, newMemberDto));
        when(projectParticipantRepository.findByProjectIdOrderByRoleAscUserLastNameAscUserFirstNameAsc(100L))
                .thenReturn(List.of(creatorParticipant, existingParticipantRecord));
        when(datasetItemRepository.findByProjectIdOrderByItemIndexAsc(100L)).thenReturn(List.of());
        when(userRepository.findAllById(any())).thenReturn(List.of(newParticipant));

        // Mock getAssignedProjectDetail (called at the end of updateProject)
        // For simplicity, we just return null or mock the behavior.
        // Actually, updateProject returns ProjectDetailDto.
        // Let's just verify the interactions for now.

        UpdateProjectRequestDto request = new UpdateProjectRequestDto("Project", "Desc", List.of(3L));
        projectService.updateProject("creator@example.com", 10L, 100L, request);

        verify(projectParticipantRepository).deleteAll(any());
        verify(projectParticipantRepository).saveAll(any());
        verify(projectRepository).save(project);
    }

    @Test
    void updateProjectShouldThrowWhenRequesterIsNotCreator() {
        User participant = UserTestBuilder.validUser().withEmail("participant@example.com").build();
        setId(participant, 2L);

        ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
        setGroupId(group, 10L);

        Project project = ProjectTestBuilder.validProject().withResearchGroup(group).build();
        setProjectId(project, 100L);

        ProjectParticipant participantRecord = ProjectParticipantTestBuilder.validParticipant()
                .withUser(participant)
                .withProject(project)
                .withRole(ProjectParticipantRole.PARTICIPANT)
                .build();

        when(userRepository.findByEmailIgnoreCase("participant@example.com")).thenReturn(Optional.of(participant));
        when(projectRepository.findByIdAndResearchGroupId(100L, 10L)).thenReturn(Optional.of(project));
        when(projectParticipantRepository.findByProjectIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(participantRecord));

        UpdateProjectRequestDto request = new UpdateProjectRequestDto("New Name", "New Description", List.of());
        assertThatThrownBy(() -> projectService.updateProject("participant@example.com", 10L, 100L, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteProjectShouldDeleteDataWhenRequesterIsCreator() {
        User creator = UserTestBuilder.validUser().withEmail("creator@example.com").build();
        setId(creator, 1L);

        ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
        setGroupId(group, 10L);

        Project project = ProjectTestBuilder.validProject().withResearchGroup(group).build();
        setProjectId(project, 100L);

        ProjectParticipant creatorParticipant = ProjectParticipantTestBuilder.validParticipant()
                .withUser(creator)
                .withProject(project)
                .withRole(ProjectParticipantRole.CREATOR)
                .build();

        when(userRepository.findByEmailIgnoreCase("creator@example.com")).thenReturn(Optional.of(creator));
        when(projectRepository.findByIdAndResearchGroupId(100L, 10L)).thenReturn(Optional.of(project));
        when(projectParticipantRepository.findByProjectIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(creatorParticipant));

        projectService.deleteProject("creator@example.com", 10L, 100L);

        verify(notificationRepository).deleteByProjectId(100L);
        verify(annotationRepository).deleteByDatasetItemProjectId(100L);
        verify(datasetItemRepository).deleteByProjectId(100L);
        verify(projectParticipantRepository).deleteByProjectId(100L);
        verify(projectRepository).delete(project);
    }

    @Test
    void deleteProjectShouldThrowWhenRequesterIsNotCreator() {
        User participant = UserTestBuilder.validUser().withEmail("participant@example.com").build();
        setId(participant, 2L);

        ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
        setGroupId(group, 10L);

        Project project = ProjectTestBuilder.validProject().withResearchGroup(group).build();
        setProjectId(project, 100L);

        ProjectParticipant participantRecord = ProjectParticipantTestBuilder.validParticipant()
                .withUser(participant)
                .withProject(project)
                .withRole(ProjectParticipantRole.PARTICIPANT)
                .build();

        when(userRepository.findByEmailIgnoreCase("participant@example.com")).thenReturn(Optional.of(participant));
        when(projectRepository.findByIdAndResearchGroupId(100L, 10L)).thenReturn(Optional.of(project));
        when(projectParticipantRepository.findByProjectIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(participantRecord));

        assertThatThrownBy(() -> projectService.deleteProject("participant@example.com", 10L, 100L))
                .isInstanceOf(AccessDeniedException.class);
    }

    private void setId(User user, Long id) {
        try {
            java.lang.reflect.Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void setGroupId(ResearchGroup group, Long id) {
        try {
            java.lang.reflect.Field idField = ResearchGroup.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(group, id);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void setProjectId(Project project, Long id) {
        try {
            java.lang.reflect.Field idField = Project.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(project, id);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }
}
