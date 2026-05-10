package es.udc.fic.corpuslab.modules.project.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import es.udc.fic.corpuslab.AbstractIntegrationTest;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserLoginRequestTestBuilder;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserTestBuilder;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.modules.notification.entities.Notification;
import es.udc.fic.corpuslab.modules.notification.enums.NotificationType;
import es.udc.fic.corpuslab.modules.notification.repositories.NotificationRepository;
import es.udc.fic.corpuslab.modules.project.dtos.AssignProjectParticipantsRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectParticipantAssignmentDto;
import es.udc.fic.corpuslab.modules.project.entities.Project;
import es.udc.fic.corpuslab.modules.project.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.enums.ProjectParticipantIaaGroup;
import es.udc.fic.corpuslab.modules.project.enums.ProjectParticipantRole;
import es.udc.fic.corpuslab.modules.project.repositories.DatasetItemRepository;
import es.udc.fic.corpuslab.modules.project.repositories.ProjectParticipantRepository;
import es.udc.fic.corpuslab.modules.project.repositories.ProjectRepository;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroup;
import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupMemberRole;
import es.udc.fic.corpuslab.modules.researchgroup.fixtures.ResearchGroupMemberTestBuilder;
import es.udc.fic.corpuslab.modules.researchgroup.fixtures.ResearchGroupTestBuilder;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupInvitationRepository;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupMemberRepository;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectAssignParticipantsIntegrationTest extends AbstractIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private ResearchGroupRepository researchGroupRepository;

        @Autowired
        private ResearchGroupMemberRepository memberRepository;

        @Autowired
        private ResearchGroupInvitationRepository invitationRepository;

        @Autowired
        private NotificationRepository notificationRepository;

        @Autowired
        private ProjectRepository projectRepository;

        @Autowired
        private ProjectParticipantRepository projectParticipantRepository;

        @Autowired
        private DatasetItemRepository datasetItemRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

        @BeforeEach
        void cleanData() {
                notificationRepository.deleteAll();
                invitationRepository.deleteAll();
                memberRepository.deleteAll();
                datasetItemRepository.deleteAll();
                projectParticipantRepository.deleteAll();
                projectRepository.deleteAll();
                researchGroupRepository.deleteAll();
                userRepository.deleteAll();
        }

        private User createUser(String email) {
                User user = UserTestBuilder.validUser()
                                .withEmail(email)
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                return userRepository.save(user);
        }

        private String loginAs(String email) throws Exception {
                MvcResult result = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                UserLoginRequestTestBuilder.validRequest().withEmail(email).build())))
                                .andExpect(status().isOk())
                                .andReturn();

                return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
        }

        private ResearchGroup createGroup(String name) {
                return researchGroupRepository.save(ResearchGroupTestBuilder.validGroup().withName(name).build());
        }

        private void addMembership(User user, ResearchGroup group, ResearchGroupMemberRole role) {
                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(user)
                                .withResearchGroup(group)
                                .withRole(role)
                                .build());
        }

        private Project createProject(ResearchGroup group, String name) {
                Project project = new Project();
                project.setResearchGroup(group);
                project.setName(name);
                return projectRepository.save(project);
        }

        private void assign(Project project, User user, ProjectParticipantRole role) {
                ProjectParticipant participant = new ProjectParticipant();
                participant.setProject(project);
                participant.setUser(user);
                participant.setRole(role);
                projectParticipantRepository.save(participant);
        }

        @Test
        void shouldPersistIaaGroupsWhenAssigningGroupedParticipants() throws Exception {
                User owner = createUser("owner.grouped@example.com");
                User participantA = createUser("grouped.a@example.com");
                User participantB = createUser("grouped.b@example.com");

                ResearchGroup group = createGroup("Grouped Assignment");
                addMembership(owner, group, ResearchGroupMemberRole.OWNER);
                addMembership(participantA, group, ResearchGroupMemberRole.ANNOTATOR);
                addMembership(participantB, group, ResearchGroupMemberRole.ANNOTATOR);

                Project project = createProject(group, "Grouped Project");
                assign(project, owner, ProjectParticipantRole.CREATOR);

                String session = loginAs("owner.grouped@example.com");

                AssignProjectParticipantsRequestDto request = new AssignProjectParticipantsRequestDto(
                                null,
                                List.of(
                                                new ProjectParticipantAssignmentDto(
                                                                participantA.getId(),
                                                                ProjectParticipantIaaGroup.GROUP_A),
                                                new ProjectParticipantAssignmentDto(
                                                                participantB.getId(),
                                                                ProjectParticipantIaaGroup.GROUP_B)));

                mockMvc.perform(post("/api/research-groups/{groupId}/projects/{projectId}/participants", group.getId(),
                                project.getId())
                                .header("Authorization", "Bearer " + session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNoContent());

                List<ProjectParticipant> projectParticipants = projectParticipantRepository.findAll().stream()
                                .filter(pp -> pp.getProject().getId().equals(project.getId()))
                                .toList();

                assertThat(projectParticipants)
                                .filteredOn(pp -> pp.getUser().getId().equals(participantA.getId()))
                                .singleElement()
                                .extracting(ProjectParticipant::getIaaGroup)
                                .isEqualTo(ProjectParticipantIaaGroup.GROUP_A);
                assertThat(projectParticipants)
                                .filteredOn(pp -> pp.getUser().getId().equals(participantB.getId()))
                                .singleElement()
                                .extracting(ProjectParticipant::getIaaGroup)
                                .isEqualTo(ProjectParticipantIaaGroup.GROUP_B);
        }

        @Test
        void shouldAssignAndReplaceProjectParticipantsWhenRequesterIsOwner() throws Exception {
                User owner = createUser("owner.assign@example.com");
                User oldParticipant = createUser("old.assign@example.com");
                User newParticipantA = createUser("newa.assign@example.com");
                User newParticipantB = createUser("newb.assign@example.com");

                ResearchGroup group = createGroup("Assignment Group");
                addMembership(owner, group, ResearchGroupMemberRole.OWNER);
                addMembership(oldParticipant, group, ResearchGroupMemberRole.ANNOTATOR);
                addMembership(newParticipantA, group, ResearchGroupMemberRole.ANNOTATOR);
                addMembership(newParticipantB, group, ResearchGroupMemberRole.ANNOTATOR);

                Project project = createProject(group, "Assignment Project");
                assign(project, owner, ProjectParticipantRole.CREATOR);
                assign(project, oldParticipant, ProjectParticipantRole.PARTICIPANT);

                String session = loginAs("owner.assign@example.com");

                AssignProjectParticipantsRequestDto request = new AssignProjectParticipantsRequestDto(List.of(
                                owner.getId(),
                                newParticipantA.getId(),
                                newParticipantB.getId()));

                mockMvc.perform(post("/api/research-groups/{groupId}/projects/{projectId}/participants", group.getId(),
                                project.getId())
                                .header("Authorization", "Bearer " + session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNoContent());

                List<ProjectParticipant> projectParticipants = projectParticipantRepository.findAll().stream()
                                .filter(pp -> pp.getProject().getId().equals(project.getId()))
                                .toList();

                List<Long> participantUserIds = projectParticipants.stream()
                                .filter(pp -> pp.getRole() == ProjectParticipantRole.PARTICIPANT)
                                .map(pp -> pp.getUser().getId())
                                .toList();

                assertThat(participantUserIds)
                                .containsExactlyInAnyOrder(newParticipantA.getId(), newParticipantB.getId())
                                .doesNotContain(owner.getId(), oldParticipant.getId());

                assertThat(projectParticipants.stream().filter(pp -> pp.getRole() == ProjectParticipantRole.CREATOR)
                                .count())
                                .isEqualTo(1L);

                List<Notification> notifications = notificationRepository.findAll();
                assertThat(notifications).hasSize(2);
                assertThat(notifications)
                                .extracting(Notification::getType)
                                .containsOnly(NotificationType.PROJECT_PARTICIPANT_ASSIGNED);
                assertThat(notifications)
                                .extracting(notification -> notification.getRecipientUser().getId())
                                .containsExactlyInAnyOrder(newParticipantA.getId(), newParticipantB.getId());
                assertThat(notifications)
                                .extracting(Notification::getProjectId)
                                .containsOnly(project.getId());
        }

        @Test
        void shouldAllowRequesterAdminToAssignParticipants() throws Exception {
                User admin = createUser("admin.assign@example.com");
                User participant = createUser("participant.assign@example.com");

                ResearchGroup group = createGroup("Admin Assignment Group");
                addMembership(admin, group, ResearchGroupMemberRole.ADMIN);
                addMembership(participant, group, ResearchGroupMemberRole.ANNOTATOR);

                Project project = createProject(group, "Admin Assignment Project");
                assign(project, admin, ProjectParticipantRole.CREATOR);

                String session = loginAs("admin.assign@example.com");

                AssignProjectParticipantsRequestDto request = new AssignProjectParticipantsRequestDto(
                                List.of(participant.getId()));

                mockMvc.perform(post("/api/research-groups/{groupId}/projects/{projectId}/participants", group.getId(),
                                project.getId())
                                .header("Authorization", "Bearer " + session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNoContent());
        }

        @Test
        void shouldReturnForbiddenWhenRequesterRoleCannotAssignParticipants() throws Exception {
                User annotator = createUser("annotator.assign@example.com");
                User participant = createUser("participant2.assign@example.com");

                ResearchGroup group = createGroup("Forbidden Assignment Group");
                addMembership(annotator, group, ResearchGroupMemberRole.ANNOTATOR);
                addMembership(participant, group, ResearchGroupMemberRole.ANNOTATOR);

                Project project = createProject(group, "Forbidden Assignment Project");
                assign(project, annotator, ProjectParticipantRole.CREATOR);

                String session = loginAs("annotator.assign@example.com");

                AssignProjectParticipantsRequestDto request = new AssignProjectParticipantsRequestDto(
                                List.of(participant.getId()));

                mockMvc.perform(post("/api/research-groups/{groupId}/projects/{projectId}/participants", group.getId(),
                                project.getId())
                                .header("Authorization", "Bearer " + session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        void shouldReturnBadRequestWhenParticipantIsNotActiveGroupMember() throws Exception {
                User owner = createUser("owner.badrequest@example.com");
                User outsider = createUser("outsider.badrequest@example.com");

                ResearchGroup group = createGroup("Bad Request Group");
                addMembership(owner, group, ResearchGroupMemberRole.OWNER);

                Project project = createProject(group, "Bad Request Project");
                assign(project, owner, ProjectParticipantRole.CREATOR);

                String session = loginAs("owner.badrequest@example.com");

                AssignProjectParticipantsRequestDto request = new AssignProjectParticipantsRequestDto(
                                List.of(outsider.getId()));

                mockMvc.perform(post("/api/research-groups/{groupId}/projects/{projectId}/participants", group.getId(),
                                project.getId())
                                .header("Authorization", "Bearer " + session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturnUnauthorizedWhenRequesterIsNotAuthenticated() throws Exception {
                User owner = createUser("owner.noauth@example.com");
                ResearchGroup group = createGroup("No Auth Group");
                addMembership(owner, group, ResearchGroupMemberRole.OWNER);

                Project project = createProject(group, "No Auth Project");
                assign(project, owner, ProjectParticipantRole.CREATOR);

                AssignProjectParticipantsRequestDto request = new AssignProjectParticipantsRequestDto(
                                List.of(owner.getId()));

                mockMvc.perform(post("/api/research-groups/{groupId}/projects/{projectId}/participants", group.getId(),
                                project.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldReturnNotFoundWhenProjectDoesNotExist() throws Exception {
                User owner = createUser("owner.notfound@example.com");
                ResearchGroup group = createGroup("Not Found Group");
                addMembership(owner, group, ResearchGroupMemberRole.OWNER);

                String session = loginAs("owner.notfound@example.com");

                AssignProjectParticipantsRequestDto request = new AssignProjectParticipantsRequestDto(
                                List.of(owner.getId()));

                mockMvc.perform(post("/api/research-groups/{groupId}/projects/{projectId}/participants", group.getId(),
                                99999L)
                                .header("Authorization", "Bearer " + session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound());
        }
}
