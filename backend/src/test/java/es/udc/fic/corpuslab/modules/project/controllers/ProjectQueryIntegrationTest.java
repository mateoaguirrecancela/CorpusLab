package es.udc.fic.corpuslab.modules.project.controllers;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

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
import es.udc.fic.corpuslab.modules.notification.repositories.NotificationRepository;
import es.udc.fic.corpuslab.modules.project.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.entities.Guideline;
import es.udc.fic.corpuslab.modules.project.entities.Label;
import es.udc.fic.corpuslab.modules.project.entities.Project;
import es.udc.fic.corpuslab.modules.project.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.enums.ProjectParticipantRole;
import es.udc.fic.corpuslab.modules.project.enums.ProjectType;
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
class ProjectQueryIntegrationTest extends AbstractIntegrationTest {

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

    private Project createProject(ResearchGroup group, String name, String description) {
        Project project = new Project();
        project.setResearchGroup(group);
        project.setName(name);
        project.setDescription(description);
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
    void shouldReturnUnauthorizedWhenListingMyProjectsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/projects/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldListOnlyProjectsAssignedToAuthenticatedUser() throws Exception {
        User alice = createUser("alice.projects@example.com");
        User bob = createUser("bob.projects@example.com");

        ResearchGroup nlpGroup = createGroup("NLP Group");
        ResearchGroup visionGroup = createGroup("Vision Group");

        addMembership(alice, nlpGroup, ResearchGroupMemberRole.ADMIN);
        addMembership(alice, visionGroup, ResearchGroupMemberRole.ANNOTATOR);
        addMembership(bob, nlpGroup, ResearchGroupMemberRole.ANNOTATOR);

        Project projectA = createProject(nlpGroup, "A", "Alpha");
        Project projectB = createProject(visionGroup, "B", "Beta");
        Project projectC = createProject(nlpGroup, "C", "Gamma");
        Project archivedCreatedByAlice = createProject(nlpGroup, "D", "Archived creator");
        archivedCreatedByAlice.setArchived(true);
        archivedCreatedByAlice = projectRepository.save(archivedCreatedByAlice);
        Project archivedParticipantForAlice = createProject(visionGroup, "E", "Archived participant");
        archivedParticipantForAlice.setArchived(true);
        archivedParticipantForAlice = projectRepository.save(archivedParticipantForAlice);

        assign(projectA, alice, ProjectParticipantRole.CREATOR);
        assign(projectB, alice, ProjectParticipantRole.PARTICIPANT);
        assign(projectC, bob, ProjectParticipantRole.PARTICIPANT);
        assign(archivedCreatedByAlice, alice, ProjectParticipantRole.CREATOR);
        assign(archivedParticipantForAlice, bob, ProjectParticipantRole.CREATOR);
        assign(archivedParticipantForAlice, alice, ProjectParticipantRole.PARTICIPANT);

        String session = loginAs("alice.projects@example.com");

        mockMvc.perform(get("/api/projects/my").header("Authorization", "Bearer " + session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].id", containsInAnyOrder(
                        Math.toIntExact(projectA.getId()),
                        Math.toIntExact(projectB.getId()))))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.content[0].participantRole").isNotEmpty())
                .andExpect(jsonPath("$.content[0].researchGroupName").isNotEmpty())
                .andExpect(jsonPath("$.content[0].completionPercentage").isNumber())
                .andExpect(jsonPath("$.content[0].archived").value(false));

        mockMvc.perform(get("/api/projects/my")
                .param("showArchived", "true")
                .header("Authorization", "Bearer " + session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].id", containsInAnyOrder(
                        Math.toIntExact(archivedCreatedByAlice.getId()),
                        Math.toIntExact(archivedParticipantForAlice.getId()))))
                .andExpect(jsonPath("$.content[*].participantRole", containsInAnyOrder("CREATOR", "PARTICIPANT")))
                .andExpect(jsonPath("$.content[0].archived").value(true));
    }

    @Test
    void shouldReturnUnauthorizedWhenListingProjectsByGroupWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/research-groups/{groupId}/projects/my", 999L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldListAssignedProjectsByGroupForGroupMember() throws Exception {
        User user = createUser("group.viewer@example.com");

        ResearchGroup groupA = createGroup("Group A");
        ResearchGroup groupB = createGroup("Group B");

        addMembership(user, groupA, ResearchGroupMemberRole.ADMIN);
        addMembership(user, groupB, ResearchGroupMemberRole.ANNOTATOR);

        Project groupAProject = createProject(groupA, "Project A", "in group A");
        Project groupBProject = createProject(groupB, "Project B", "in group B");

        assign(groupAProject, user, ProjectParticipantRole.CREATOR);
        assign(groupBProject, user, ProjectParticipantRole.PARTICIPANT);

        String session = loginAs("group.viewer@example.com");

        mockMvc.perform(get("/api/research-groups/{groupId}/projects/my", groupA.getId())
                .header("Authorization", "Bearer " + session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(groupAProject.getId()))
                .andExpect(jsonPath("$.content[0].researchGroupId").value(groupA.getId()))
                .andExpect(jsonPath("$.content[0].participantRole").value("CREATOR"));
    }

    @Test
    void shouldReturnForbiddenWhenListingProjectsByGroupAsNonMember() throws Exception {
        createUser("outsider.group.viewer@example.com");
        ResearchGroup group = createGroup("Private Group");

        String session = loginAs("outsider.group.viewer@example.com");

        mockMvc.perform(get("/api/research-groups/{groupId}/projects/my", group.getId())
                .header("Authorization", "Bearer " + session))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnUnauthorizedWhenReadingProjectDetailWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}", 999L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnProjectDetailWhenUserIsAssigned() throws Exception {
        User owner = createUser("owner.detail@example.com");
        User investigator = createUser("investigator.detail@example.com");

        ResearchGroup group = createGroup("Detail Group");
        addMembership(owner, group, ResearchGroupMemberRole.OWNER);
        addMembership(investigator, group, ResearchGroupMemberRole.ANNOTATOR);

        Project project = createProject(group, "Detail Project", "Detailed description");
        project.setProjectType(ProjectType.NER);
        Label labelA = new Label();
        labelA.setProject(project);
        labelA.setName("PERSON");
        labelA.setColor("#10B981");

        Label labelB = new Label();
        labelB.setProject(project);
        labelB.setName("ORG");
        labelB.setColor("#F43F5E");

        project.getLabels().add(labelA);
        project.getLabels().add(labelB);

        Guideline guideline = new Guideline();
        guideline.setProject(project);
        guideline.setContent("Annotate named entities.");
        guideline.setFileUrl(null);
        project.setGuideline(guideline);

        project = projectRepository.save(project);

        assign(project, owner, ProjectParticipantRole.CREATOR);
        assign(project, investigator, ProjectParticipantRole.PARTICIPANT);

        DatasetItem item1 = new DatasetItem();
        item1.setProject(project);
        item1.setItemIndex(0);
        item1.setContent(Map.of(
                "text", "Alice works at OpenAI",
                "completedAnnotations", 1));

        DatasetItem item2 = new DatasetItem();
        item2.setProject(project);
        item2.setItemIndex(1);
        item2.setContent(Map.of(
                "text", "Bob moved to Paris",
                "completedAnnotations", 2));

        datasetItemRepository.save(item1);
        datasetItemRepository.save(item2);

        String session = loginAs("investigator.detail@example.com");

        mockMvc.perform(get("/api/projects/{projectId}", project.getId()).header("Authorization", "Bearer " + session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(project.getId()))
                .andExpect(jsonPath("$.researchGroupId").value(group.getId()))
                .andExpect(jsonPath("$.researchGroupName").value(group.getName()))
                .andExpect(jsonPath("$.name").value("Detail Project"))
                .andExpect(jsonPath("$.description").value("Detailed description"))
                .andExpect(jsonPath("$.projectType").value("NER"))
                .andExpect(jsonPath("$.completionPercentage").value(0))
                .andExpect(jsonPath("$.participantRole").value("PARTICIPANT"))
                .andExpect(jsonPath("$.participants", hasSize(2)))
                .andExpect(jsonPath("$.participants[*].role", containsInAnyOrder("CREATOR", "PARTICIPANT")))
                .andExpect(jsonPath("$.participants[0].completionPercentage").value(0))
                .andExpect(jsonPath("$.participants[1].completionPercentage").value(0))
                .andExpect(jsonPath("$.labels", hasSize(2)))
                .andExpect(jsonPath("$.guidelineText").value("Annotate named entities."))
                .andExpect(jsonPath("$.guidelinePdfBase64").isEmpty())
                .andExpect(jsonPath("$.datasetItemsCount").value(2))
                .andExpect(jsonPath("$.archived").value(false))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void shouldArchiveProjectWhenRequesterIsCreator() throws Exception {
        User owner = createUser("archive.creator@example.com");
        ResearchGroup group = createGroup("Archive Group");
        addMembership(owner, group, ResearchGroupMemberRole.OWNER);

        Project project = createProject(group, "Archive Me", "soft hide");
        assign(project, owner, ProjectParticipantRole.CREATOR);

        String session = loginAs("archive.creator@example.com");

        mockMvc.perform(put("/api/projects/{projectId}/archive", project.getId())
                .header("Authorization", "Bearer " + session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(project.getId()))
                .andExpect(jsonPath("$.archived").value(true));
    }

    @Test
    void shouldUnarchiveProjectWhenRequesterIsCreator() throws Exception {
        User owner = createUser("unarchive.creator@example.com");
        ResearchGroup group = createGroup("Unarchive Group");
        addMembership(owner, group, ResearchGroupMemberRole.OWNER);

        Project project = createProject(group, "Unarchive Me", "restore");
        project.setArchived(true);
        project = projectRepository.save(project);
        assign(project, owner, ProjectParticipantRole.CREATOR);

        String session = loginAs("unarchive.creator@example.com");

        mockMvc.perform(put("/api/projects/{projectId}/unarchive", project.getId())
                .header("Authorization", "Bearer " + session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(project.getId()))
                .andExpect(jsonPath("$.archived").value(false));
    }

    @Test
    void shouldReturnForbiddenWhenArchivingAsParticipant() throws Exception {
        User owner = createUser("archive.owner@example.com");
        User participant = createUser("archive.participant@example.com");
        ResearchGroup group = createGroup("Forbidden Archive Group");
        addMembership(owner, group, ResearchGroupMemberRole.OWNER);
        addMembership(participant, group, ResearchGroupMemberRole.ANNOTATOR);

        Project project = createProject(group, "Forbidden Archive", "private");
        assign(project, owner, ProjectParticipantRole.CREATOR);
        assign(project, participant, ProjectParticipantRole.PARTICIPANT);

        String session = loginAs("archive.participant@example.com");

        mockMvc.perform(put("/api/projects/{projectId}/archive", project.getId())
                .header("Authorization", "Bearer " + session))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnNotFoundWhenReadingProjectDetailWithoutAssignment() throws Exception {
        User owner = createUser("owner.only@example.com");
        createUser("outsider.detail@example.com");

        ResearchGroup group = createGroup("No Access Group");
        addMembership(owner, group, ResearchGroupMemberRole.OWNER);

        Project project = createProject(group, "Owner Project", "private detail");
        assign(project, owner, ProjectParticipantRole.CREATOR);

        String session = loginAs("outsider.detail@example.com");

        mockMvc.perform(get("/api/projects/{projectId}", project.getId()).header("Authorization", "Bearer " + session))
                .andExpect(status().isNotFound());
    }
}
