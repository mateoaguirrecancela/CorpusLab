package es.udc.fic.corpuslab.modules.project.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import es.udc.fic.corpuslab.AbstractIntegrationTest;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserLoginRequestTestBuilder;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserTestBuilder;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.modules.notification.repositories.NotificationRepository;
import es.udc.fic.corpuslab.modules.project.participant.dtos.ProjectParticipantAssignmentDto;
import es.udc.fic.corpuslab.modules.project.core.dtos.UpdateProjectRequestDto;
import es.udc.fic.corpuslab.modules.project.shared.entities.Project;
import es.udc.fic.corpuslab.modules.project.shared.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectParticipantIaaGroup;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectParticipantRole;
import es.udc.fic.corpuslab.modules.project.shared.repositories.DatasetItemRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.ProjectParticipantRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.ProjectRepository;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroup;
import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupMemberRole;
import es.udc.fic.corpuslab.modules.researchgroup.fixtures.ResearchGroupMemberTestBuilder;
import es.udc.fic.corpuslab.modules.researchgroup.fixtures.ResearchGroupTestBuilder;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupInvitationRepository;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupMemberRepository;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupRepository;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectUpdateDeleteIntegrationTest extends AbstractIntegrationTest {

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

    private String loginAs(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        UserLoginRequestTestBuilder.validRequest()
                                .withEmail(email)
                                .build())))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void shouldUpdateProjectWhenRequesterIsCreator() throws Exception {
        User creator = UserTestBuilder.validUser()
                .withEmail("creator.update@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        creator = userRepository.save(creator);

        ResearchGroup group = researchGroupRepository.save(ResearchGroupTestBuilder.validGroup().build());
        memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                .withUser(creator)
                .withResearchGroup(group)
                .withRole(ResearchGroupMemberRole.OWNER)
                .build());

        Project project = new Project();
        project.setName("Old Name");
        project.setResearchGroup(group);
        project = projectRepository.save(project);

        ProjectParticipant participant = new ProjectParticipant();
        participant.setProject(project);
        participant.setUser(creator);
        participant.setRole(ProjectParticipantRole.CREATOR);
        projectParticipantRepository.save(participant);

        String session = loginAs("creator.update@example.com");

        UpdateProjectRequestDto request = new UpdateProjectRequestDto("New Name", "New Description", List.of());

        mockMvc.perform(put("/api/research-groups/{groupId}/projects/{projectId}", group.getId(), project.getId())
                .header("Authorization", "Bearer " + session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.description").value("New Description"));
    }

    @Test
    void shouldUpdateProjectParticipantGroupsWhenRequesterIsCreator() throws Exception {
        User creator = UserTestBuilder.validUser()
                .withEmail("creator.groups@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        creator = userRepository.save(creator);
        User participantA = UserTestBuilder.validUser().withEmail("participant.a@example.com").build();
        participantA = userRepository.save(participantA);
        User participantB = UserTestBuilder.validUser().withEmail("participant.b@example.com").build();
        participantB = userRepository.save(participantB);

        ResearchGroup group = researchGroupRepository.save(ResearchGroupTestBuilder.validGroup().build());
        memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                .withUser(creator)
                .withResearchGroup(group)
                .withRole(ResearchGroupMemberRole.OWNER)
                .build());
        memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                .withUser(participantA)
                .withResearchGroup(group)
                .withRole(ResearchGroupMemberRole.ANNOTATOR)
                .build());
        memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                .withUser(participantB)
                .withResearchGroup(group)
                .withRole(ResearchGroupMemberRole.ANNOTATOR)
                .build());

        Project project = new Project();
        project.setName("Grouped Project");
        project.setResearchGroup(group);
        project = projectRepository.save(project);

        ProjectParticipant creatorParticipant = new ProjectParticipant();
        creatorParticipant.setProject(project);
        creatorParticipant.setUser(creator);
        creatorParticipant.setRole(ProjectParticipantRole.CREATOR);
        projectParticipantRepository.save(creatorParticipant);

        Long participantAId = participantA.getId();
        Long participantBId = participantB.getId();
        Long projectId = project.getId();
        String session = loginAs("creator.groups@example.com");
        UpdateProjectRequestDto request = new UpdateProjectRequestDto(
                "Grouped Project",
                null,
                null,
                List.of(
                        new ProjectParticipantAssignmentDto(participantAId, ProjectParticipantIaaGroup.GROUP_A),
                        new ProjectParticipantAssignmentDto(participantBId, ProjectParticipantIaaGroup.GROUP_B)));

        mockMvc.perform(put("/api/research-groups/{groupId}/projects/{projectId}", group.getId(), projectId)
                .header("Authorization", "Bearer " + session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        List<ProjectParticipant> participants = projectParticipantRepository.findAll().stream()
                .filter(projectParticipant -> projectParticipant.getProject().getId().equals(projectId))
                .toList();

        assertThat(participants)
                .filteredOn(projectParticipant -> projectParticipant.getUser().getId().equals(participantAId))
                .singleElement()
                .extracting(ProjectParticipant::getIaaGroup)
                .isEqualTo(ProjectParticipantIaaGroup.GROUP_A);
        assertThat(participants)
                .filteredOn(projectParticipant -> projectParticipant.getUser().getId().equals(participantBId))
                .singleElement()
                .extracting(ProjectParticipant::getIaaGroup)
                .isEqualTo(ProjectParticipantIaaGroup.GROUP_B);
    }

    @Test
    void shouldReturnForbiddenWhenRequesterIsNotCreatorOnUpdate() throws Exception {
        User creator = UserTestBuilder.validUser().withEmail("creator.forbidden@example.com").build();
        userRepository.save(creator);

        User annotator = UserTestBuilder.validUser()
                .withEmail("annotator.forbidden@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        annotator = userRepository.save(annotator);

        ResearchGroup group = researchGroupRepository.save(ResearchGroupTestBuilder.validGroup().build());
        memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                .withUser(creator)
                .withResearchGroup(group)
                .withRole(ResearchGroupMemberRole.OWNER)
                .build());
        memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                .withUser(annotator)
                .withResearchGroup(group)
                .withRole(ResearchGroupMemberRole.ANNOTATOR)
                .build());

        Project project = new Project();
        project.setName("Not Mine");
        project.setResearchGroup(group);
        project = projectRepository.save(project);

        ProjectParticipant creatorParticipant = new ProjectParticipant();
        creatorParticipant.setProject(project);
        creatorParticipant.setUser(creator);
        creatorParticipant.setRole(ProjectParticipantRole.CREATOR);
        projectParticipantRepository.save(creatorParticipant);

        ProjectParticipant annotatorParticipant = new ProjectParticipant();
        annotatorParticipant.setProject(project);
        annotatorParticipant.setUser(annotator);
        annotatorParticipant.setRole(ProjectParticipantRole.PARTICIPANT);
        projectParticipantRepository.save(annotatorParticipant);

        String session = loginAs("annotator.forbidden@example.com");
        UpdateProjectRequestDto request = new UpdateProjectRequestDto("New Name", "New Description", List.of());

        mockMvc.perform(put("/api/research-groups/{groupId}/projects/{projectId}", group.getId(), project.getId())
                .header("Authorization", "Bearer " + session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectBlankNameOnUpdate() throws Exception {
        User creator = UserTestBuilder.validUser()
                .withEmail("creator.blank@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        creator = userRepository.save(creator);

        ResearchGroup group = researchGroupRepository.save(ResearchGroupTestBuilder.validGroup().build());
        memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                .withUser(creator)
                .withResearchGroup(group)
                .withRole(ResearchGroupMemberRole.OWNER)
                .build());

        Project project = new Project();
        project.setName("Old Name");
        project.setResearchGroup(group);
        project = projectRepository.save(project);

        ProjectParticipant creatorParticipant = new ProjectParticipant();
        creatorParticipant.setProject(project);
        creatorParticipant.setUser(creator);
        creatorParticipant.setRole(ProjectParticipantRole.CREATOR);
        projectParticipantRepository.save(creatorParticipant);

        String session = loginAs("creator.blank@example.com");
        UpdateProjectRequestDto request = new UpdateProjectRequestDto("   ", "New Description", List.of());

        mockMvc.perform(put("/api/research-groups/{groupId}/projects/{projectId}", group.getId(), project.getId())
                .header("Authorization", "Bearer " + session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectDescriptionTooLongOnUpdate() throws Exception {
        User creator = UserTestBuilder.validUser()
                .withEmail("creator.long@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        creator = userRepository.save(creator);

        ResearchGroup group = researchGroupRepository.save(ResearchGroupTestBuilder.validGroup().build());
        memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                .withUser(creator)
                .withResearchGroup(group)
                .withRole(ResearchGroupMemberRole.OWNER)
                .build());

        Project project = new Project();
        project.setName("Old Name");
        project.setResearchGroup(group);
        project = projectRepository.save(project);

        ProjectParticipant creatorParticipant = new ProjectParticipant();
        creatorParticipant.setProject(project);
        creatorParticipant.setUser(creator);
        creatorParticipant.setRole(ProjectParticipantRole.CREATOR);
        projectParticipantRepository.save(creatorParticipant);

        String session = loginAs("creator.long@example.com");
        UpdateProjectRequestDto request = new UpdateProjectRequestDto("New Name", "a".repeat(2049), List.of());

        mockMvc.perform(put("/api/research-groups/{groupId}/projects/{projectId}", group.getId(), project.getId())
                .header("Authorization", "Bearer " + session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnNotFoundWhenProjectDoesNotExistOnUpdate() throws Exception {
        User creator = UserTestBuilder.validUser()
                .withEmail("creator.notfound@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        userRepository.save(creator);

        ResearchGroup group = researchGroupRepository.save(ResearchGroupTestBuilder.validGroup().build());
        memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                .withUser(creator)
                .withResearchGroup(group)
                .withRole(ResearchGroupMemberRole.OWNER)
                .build());

        String session = loginAs("creator.notfound@example.com");
        UpdateProjectRequestDto request = new UpdateProjectRequestDto("New Name", "New Description", List.of());

        mockMvc.perform(put("/api/research-groups/{groupId}/projects/{projectId}", group.getId(), 999999L)
                .header("Authorization", "Bearer " + session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteProjectWhenRequesterIsCreator() throws Exception {
        User creator = UserTestBuilder.validUser()
                .withEmail("creator.delete@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        creator = userRepository.save(creator);

        ResearchGroup group = researchGroupRepository.save(ResearchGroupTestBuilder.validGroup().build());
        memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                .withUser(creator)
                .withResearchGroup(group)
                .withRole(ResearchGroupMemberRole.OWNER)
                .build());

        Project project = new Project();
        project.setName("To Delete");
        project.setResearchGroup(group);
        project = projectRepository.save(project);

        ProjectParticipant participant = new ProjectParticipant();
        participant.setProject(project);
        participant.setUser(creator);
        participant.setRole(ProjectParticipantRole.CREATOR);
        projectParticipantRepository.save(participant);

        String session = loginAs("creator.delete@example.com");

        mockMvc.perform(delete("/api/research-groups/{groupId}/projects/{projectId}", group.getId(), project.getId())
                .header("Authorization", "Bearer " + session))
                .andExpect(status().isNoContent());

        assertThat(projectRepository.existsById(project.getId())).isFalse();
    }

    @Test
    void shouldReturnForbiddenWhenRequesterIsNotCreatorOnDelete() throws Exception {
        User creator = UserTestBuilder.validUser()
                .withEmail("creator2@example.com")
                .build();
        userRepository.save(creator);

        User annotator = UserTestBuilder.validUser()
                .withEmail("annotator.delete@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        annotator = userRepository.save(annotator);

        ResearchGroup group = researchGroupRepository.save(ResearchGroupTestBuilder.validGroup().build());
        memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                .withUser(creator)
                .withResearchGroup(group)
                .withRole(ResearchGroupMemberRole.OWNER)
                .build());
        memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                .withUser(annotator)
                .withResearchGroup(group)
                .withRole(ResearchGroupMemberRole.ANNOTATOR)
                .build());

        Project project = new Project();
        project.setName("Not Mine");
        project.setResearchGroup(group);
        project = projectRepository.save(project);

        ProjectParticipant creatorParticipant = new ProjectParticipant();
        creatorParticipant.setProject(project);
        creatorParticipant.setUser(creator);
        creatorParticipant.setRole(ProjectParticipantRole.CREATOR);
        projectParticipantRepository.save(creatorParticipant);

        ProjectParticipant annotatorParticipant = new ProjectParticipant();
        annotatorParticipant.setProject(project);
        annotatorParticipant.setUser(annotator);
        annotatorParticipant.setRole(ProjectParticipantRole.PARTICIPANT);
        projectParticipantRepository.save(annotatorParticipant);

        String session = loginAs("annotator.delete@example.com");

        mockMvc.perform(delete("/api/research-groups/{groupId}/projects/{projectId}", group.getId(), project.getId())
                .header("Authorization", "Bearer " + session))
                .andExpect(status().isForbidden());

        assertThat(projectRepository.existsById(project.getId())).isTrue();
    }

    @Test
    void shouldReturnNotFoundWhenProjectDoesNotExistOnDelete() throws Exception {
        User creator = UserTestBuilder.validUser()
                .withEmail("creator.notfound.del@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        userRepository.save(creator);

        ResearchGroup group = researchGroupRepository.save(ResearchGroupTestBuilder.validGroup().build());
        memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                .withUser(creator)
                .withResearchGroup(group)
                .withRole(ResearchGroupMemberRole.OWNER)
                .build());

        String session = loginAs("creator.notfound.del@example.com");

        mockMvc.perform(delete("/api/research-groups/{groupId}/projects/{projectId}", group.getId(), 999999L)
                .header("Authorization", "Bearer " + session))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteParticipantsAndDatasetItemsWhenProjectIsDeleted() throws Exception {
        User creator = UserTestBuilder.validUser()
                .withEmail("creator.sideeffects@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        creator = userRepository.save(creator);

        ResearchGroup group = researchGroupRepository.save(ResearchGroupTestBuilder.validGroup().build());
        memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                .withUser(creator)
                .withResearchGroup(group)
                .withRole(ResearchGroupMemberRole.OWNER)
                .build());

        Project project = new Project();
        project.setName("Side Effects");
        project.setResearchGroup(group);
        project = projectRepository.save(project);

        ProjectParticipant creatorParticipant = new ProjectParticipant();
        creatorParticipant.setProject(project);
        creatorParticipant.setUser(creator);
        creatorParticipant.setRole(ProjectParticipantRole.CREATOR);
        projectParticipantRepository.save(creatorParticipant);
        
        // Add a dataset item
        es.udc.fic.corpuslab.modules.project.shared.entities.DatasetItem item = new es.udc.fic.corpuslab.modules.project.shared.entities.DatasetItem();
        item.setProject(project);
        item.setItemIndex(0);
        item.setContent(java.util.Map.of("text", "content"));
        datasetItemRepository.save(item);

        String session = loginAs("creator.sideeffects@example.com");

        mockMvc.perform(delete("/api/research-groups/{groupId}/projects/{projectId}", group.getId(), project.getId())
                .header("Authorization", "Bearer " + session))
                .andExpect(status().isNoContent());

        assertThat(projectRepository.existsById(project.getId())).isFalse();
        assertThat(projectParticipantRepository.findByProjectIdOrderByRoleAscUserLastNameAscUserFirstNameAsc(project.getId())).isEmpty();
        assertThat(datasetItemRepository.findByProjectIdOrderByItemIndexAsc(project.getId())).isEmpty();
    }

    @Test
    void shouldCleanupIncompleteProjectWhenRequesterIsCreator() throws Exception {
        User creator = UserTestBuilder.validUser()
                .withEmail("creator.cleanup@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        creator = userRepository.save(creator);

        ResearchGroup group = researchGroupRepository.save(ResearchGroupTestBuilder.validGroup().build());
        memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                .withUser(creator)
                .withResearchGroup(group)
                .withRole(ResearchGroupMemberRole.OWNER)
                .build());

        Project project = new Project();
        project.setName("Cleanup Candidate");
        project.setResearchGroup(group);
        project = projectRepository.save(project);

        ProjectParticipant participant = new ProjectParticipant();
        participant.setProject(project);
        participant.setUser(creator);
        participant.setRole(ProjectParticipantRole.CREATOR);
        projectParticipantRepository.save(participant);

        String session = loginAs("creator.cleanup@example.com");

        mockMvc.perform(delete("/api/research-groups/{groupId}/projects/{projectId}/wizard-cleanup", group.getId(),
                project.getId())
                .header("Authorization", "Bearer " + session))
                .andExpect(status().isNoContent());

        assertThat(projectRepository.existsById(project.getId())).isFalse();
        assertThat(projectParticipantRepository.findByProjectIdOrderByRoleAscUserLastNameAscUserFirstNameAsc(project.getId()))
                .isEmpty();
    }

    @Test
    void shouldReturnForbiddenWhenRequesterIsNotCreatorOnWizardCleanup() throws Exception {
        User creator = UserTestBuilder.validUser()
                .withEmail("creator.cleanup.forbidden@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        creator = userRepository.save(creator);

        User participantUser = UserTestBuilder.validUser()
                .withEmail("participant.cleanup.forbidden@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        participantUser = userRepository.save(participantUser);

        ResearchGroup group = researchGroupRepository.save(ResearchGroupTestBuilder.validGroup().build());
        memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                .withUser(creator)
                .withResearchGroup(group)
                .withRole(ResearchGroupMemberRole.OWNER)
                .build());
        memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                .withUser(participantUser)
                .withResearchGroup(group)
                .withRole(ResearchGroupMemberRole.ANNOTATOR)
                .build());

        Project project = new Project();
        project.setName("Cleanup Forbidden");
        project.setResearchGroup(group);
        project = projectRepository.save(project);

        ProjectParticipant creatorParticipant = new ProjectParticipant();
        creatorParticipant.setProject(project);
        creatorParticipant.setUser(creator);
        creatorParticipant.setRole(ProjectParticipantRole.CREATOR);
        projectParticipantRepository.save(creatorParticipant);

        ProjectParticipant participant = new ProjectParticipant();
        participant.setProject(project);
        participant.setUser(participantUser);
        participant.setRole(ProjectParticipantRole.PARTICIPANT);
        projectParticipantRepository.save(participant);

        String session = loginAs("participant.cleanup.forbidden@example.com");

        mockMvc.perform(delete("/api/research-groups/{groupId}/projects/{projectId}/wizard-cleanup", group.getId(),
                project.getId())
                .header("Authorization", "Bearer " + session))
                .andExpect(status().isForbidden());

        assertThat(projectRepository.existsById(project.getId())).isTrue();
    }

    @Test
    void shouldReturnForbiddenWhenProjectIsNotEligibleForWizardCleanup() throws Exception {
        User creator = UserTestBuilder.validUser()
                .withEmail("creator.cleanup.ineligible@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        creator = userRepository.save(creator);

        User assignedParticipant = UserTestBuilder.validUser()
                .withEmail("participant.cleanup.ineligible@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        assignedParticipant = userRepository.save(assignedParticipant);

        ResearchGroup group = researchGroupRepository.save(ResearchGroupTestBuilder.validGroup().build());
        memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                .withUser(creator)
                .withResearchGroup(group)
                .withRole(ResearchGroupMemberRole.OWNER)
                .build());
        memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                .withUser(assignedParticipant)
                .withResearchGroup(group)
                .withRole(ResearchGroupMemberRole.ANNOTATOR)
                .build());

        Project project = new Project();
        project.setName("Cleanup Not Eligible");
        project.setResearchGroup(group);
        project = projectRepository.save(project);

        ProjectParticipant creatorParticipant = new ProjectParticipant();
        creatorParticipant.setProject(project);
        creatorParticipant.setUser(creator);
        creatorParticipant.setRole(ProjectParticipantRole.CREATOR);
        projectParticipantRepository.save(creatorParticipant);

        ProjectParticipant participant = new ProjectParticipant();
        participant.setProject(project);
        participant.setUser(assignedParticipant);
        participant.setRole(ProjectParticipantRole.PARTICIPANT);
        projectParticipantRepository.save(participant);

        String session = loginAs("creator.cleanup.ineligible@example.com");

        mockMvc.perform(delete("/api/research-groups/{groupId}/projects/{projectId}/wizard-cleanup", group.getId(),
                project.getId())
                .header("Authorization", "Bearer " + session))
                .andExpect(status().isForbidden());

        assertThat(projectRepository.existsById(project.getId())).isTrue();
    }
}
