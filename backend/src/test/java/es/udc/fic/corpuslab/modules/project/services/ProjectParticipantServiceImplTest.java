package es.udc.fic.corpuslab.modules.project.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import es.udc.fic.corpuslab.modules.notification.services.EmailService;
import es.udc.fic.corpuslab.modules.notification.services.NotificationService;
import es.udc.fic.corpuslab.modules.project.entities.Project;
import es.udc.fic.corpuslab.modules.project.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.enums.ProjectParticipantRole;
import es.udc.fic.corpuslab.modules.project.exceptions.InvalidProjectParticipantsException;
import es.udc.fic.corpuslab.modules.project.fixtures.ProjectParticipantTestBuilder;
import es.udc.fic.corpuslab.modules.project.fixtures.ProjectTestBuilder;
import es.udc.fic.corpuslab.modules.project.repositories.AnnotationRepository;
import es.udc.fic.corpuslab.modules.project.repositories.ProjectParticipantRepository;
import es.udc.fic.corpuslab.modules.project.repositories.ProjectRepository;
import es.udc.fic.corpuslab.modules.researchgroup.api.ResearchGroupApiService;
import es.udc.fic.corpuslab.modules.researchgroup.api.dtos.ResearchGroupMemberInfo;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroup;
import es.udc.fic.corpuslab.modules.researchgroup.fixtures.ResearchGroupTestBuilder;
import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class ProjectParticipantServiceImplTest {

    @Mock private ProjectParticipantRepository projectParticipantRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private AuthApiService authApiService;
    @Mock private ResearchGroupApiService researchGroupApiService;
    @Mock private AnnotationRepository annotationRepository;
    @Mock private NotificationService notificationService;
    @Mock private EmailService emailService;
    @Mock private EntityManager entityManager;

    private ProjectParticipantService projectParticipantService;

    @BeforeEach
    void setUp() {
        projectParticipantService = new ProjectParticipantServiceImpl(
                projectParticipantRepository, projectRepository,
                annotationRepository, authApiService, researchGroupApiService,
                notificationService, emailService, entityManager,
                "http://localhost:5173");
    }

    @Test
    void assignParticipants_ShouldThrowException_WhenUserNotOwnerOrAdmin() {
        when(authApiService.findUserByEmail("member@example.com"))
                .thenReturn(new UserInfo(1L, "member@example.com", "Member", "User"));

        Project project = ProjectTestBuilder.validProject().build();
        setProjectId(project, 100L);

        when(projectRepository.findByIdAndResearchGroupId(100L, 10L)).thenReturn(Optional.of(project));
        when(researchGroupApiService.findActiveMember(10L, 1L))
                .thenReturn(Optional.of(new ResearchGroupMemberInfo(1L, "ANNOTATOR")));

        assertThatThrownBy(() -> projectParticipantService.assignParticipants("member@example.com", 10L, 100L, List.of(2L)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void assignParticipants_ShouldThrowException_WhenAssigningNonGroupMembers() {
        when(authApiService.findUserByEmail("owner@example.com"))
                .thenReturn(new UserInfo(1L, "owner@example.com", "Owner", "User"));

        Project project = ProjectTestBuilder.validProject().build();
        setProjectId(project, 100L);

        when(projectRepository.findByIdAndResearchGroupId(100L, 10L)).thenReturn(Optional.of(project));
        when(researchGroupApiService.findActiveMember(10L, 1L))
                .thenReturn(Optional.of(new ResearchGroupMemberInfo(1L, "OWNER")));

        // replaceProjectParticipants fetches via findById
        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));

        // Group only has member 1 and 2. We request to assign 3.
        when(researchGroupApiService.findActiveMemberUserIds(10L)).thenReturn(List.of(1L, 2L));

        assertThatThrownBy(() -> projectParticipantService.assignParticipants("owner@example.com", 10L, 100L, List.of(3L)))
                .isInstanceOf(InvalidProjectParticipantsException.class)
                .hasMessageContaining("must be active members");
    }

    @Test
    void assignParticipants_ShouldRemoveOldAndAddNewParticipants() {
        User owner = UserTestBuilder.validUser().withEmail("owner@example.com").build();
        setId(owner, 1L);

        User toKeep = UserTestBuilder.validUser().build(); setId(toKeep, 2L);
        User toRemove = UserTestBuilder.validUser().build(); setId(toRemove, 3L);
        User toAdd = UserTestBuilder.validUser().build(); setId(toAdd, 4L);

        Project project = ProjectTestBuilder.validProject().build();
        setProjectId(project, 100L);
        ResearchGroup rg = ResearchGroupTestBuilder.validGroup().withName("Test Group").build();
        setGroupId(rg, 10L);
        project.setResearchGroup(rg);

        when(authApiService.findUserByEmail("owner@example.com"))
                .thenReturn(new UserInfo(1L, "owner@example.com", "Owner", "User"));
        when(authApiService.findUserById(1L))
                .thenReturn(new UserInfo(1L, "owner@example.com", "Owner", "User"));
        when(projectRepository.findByIdAndResearchGroupId(100L, 10L)).thenReturn(Optional.of(project));
        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(researchGroupApiService.findActiveMember(10L, 1L))
                .thenReturn(Optional.of(new ResearchGroupMemberInfo(1L, "OWNER")));
        when(researchGroupApiService.findActiveMemberUserIds(10L)).thenReturn(List.of(1L, 2L, 3L, 4L));

        ProjectParticipant oldToKeep = new ProjectParticipant(); oldToKeep.setUser(toKeep); oldToKeep.setRole(ProjectParticipantRole.PARTICIPANT);
        ProjectParticipant oldToRemove = new ProjectParticipant(); oldToRemove.setUser(toRemove); oldToRemove.setRole(ProjectParticipantRole.PARTICIPANT);
        
        when(projectParticipantRepository.findByProjectIdOrderByRoleAscUserLastNameAscUserFirstNameAsc(100L))
            .thenReturn(List.of(oldToKeep, oldToRemove));

        when(authApiService.findUsersByIds(any())).thenReturn(List.of(
            new UserInfo(4L, "toAdd@example.com", "To", "Add")
        ));
        when(entityManager.getReference(User.class, 4L)).thenReturn(toAdd);

        projectParticipantService.assignParticipants("owner@example.com", 10L, 100L, List.of(2L, 4L));

        verify(annotationRepository).deleteByDatasetItemProjectIdAndUserIdIn(eq(100L), eq(Set.of(3L)));
        verify(projectParticipantRepository).deleteAll(any());
        verify(projectParticipantRepository).saveAll(any());
    }

    @Test
    void removeParticipantFromAllGroupProjects_ShouldRemoveParticipantFromProjects() {
        User user = UserTestBuilder.validUser().build();
        setId(user, 1L);

        Project project = ProjectTestBuilder.validProject().build();
        setProjectId(project, 100L);
        ProjectParticipant participant = ProjectParticipantTestBuilder.validParticipant()
                .withUser(user).withProject(project).withRole(ProjectParticipantRole.PARTICIPANT).build();

        when(projectRepository.findByResearchGroupId(10L)).thenReturn(List.of(project));
        when(projectParticipantRepository.findByProjectIdAndUserId(100L, 1L))
            .thenReturn(Optional.of(participant));

        projectParticipantService.removeParticipantFromAllGroupProjects(10L, 1L);

        verify(annotationRepository).deleteByDatasetItemProjectIdAndUserIdIn(eq(100L), eq(Set.of(1L)));
        verify(projectParticipantRepository).delete(participant);
    }

    private void setId(User user, Long id) {
        try {
            java.lang.reflect.Field field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception e) {}
    }

    private void setProjectId(Project project, Long id) {
        try {
            java.lang.reflect.Field field = Project.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(project, id);
        } catch (Exception e) {}
    }

    private void setGroupId(ResearchGroup group, Long id) {
        try {
            java.lang.reflect.Field field = ResearchGroup.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(group, id);
        } catch (Exception e) {}
    }
}
