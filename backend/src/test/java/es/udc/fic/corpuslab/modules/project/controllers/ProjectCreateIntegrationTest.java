package es.udc.fic.corpuslab.modules.project.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import es.udc.fic.corpuslab.AbstractIntegrationTest;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserLoginRequestTestBuilder;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserTestBuilder;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.modules.notification.repositories.NotificationRepository;
import es.udc.fic.corpuslab.modules.project.dtos.CreateProjectRequestDto;
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
class ProjectCreateIntegrationTest extends AbstractIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @BeforeEach
    void cleanData() {
        notificationRepository.deleteAll();
        invitationRepository.deleteAll();
        memberRepository.deleteAll();
        projectRepository.deleteAll();
        researchGroupRepository.deleteAll();
        userRepository.deleteAll();
    }

    private void createUser(String email) {
        User user = UserTestBuilder.validUser()
                .withEmail(email)
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        userRepository.save(user);
    }

    private MockHttpSession loginAs(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        UserLoginRequestTestBuilder.validRequest()
                                .withEmail(email)
                                .build())))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }

    @Test
    void shouldCreateProjectWhenRequesterIsOwner() throws Exception {
        createUser("owner@example.com");
        User owner = userRepository.findByEmailIgnoreCase("owner@example.com").orElseThrow();

        ResearchGroup group = researchGroupRepository.save(ResearchGroupTestBuilder.validGroup().build());
        memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                .withUser(owner)
                .withResearchGroup(group)
                .withRole(ResearchGroupMemberRole.OWNER)
                .build());

        MockHttpSession session = loginAs("owner@example.com");

        CreateProjectRequestDto request = new CreateProjectRequestDto("  First Project  ", "  Baseline corpus  ");

        mockMvc.perform(post("/api/research-groups/{groupId}/projects", group.getId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.researchGroupId").value(group.getId()))
                .andExpect(jsonPath("$.name").value("First Project"))
                .andExpect(jsonPath("$.description").value("Baseline corpus"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        mockMvc.perform(get("/api/research-groups/{groupId}", group.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeProjects").value(1));
    }

    @Test
    void shouldCreateProjectWhenRequesterIsAdmin() throws Exception {
        createUser("admin@example.com");
        User admin = userRepository.findByEmailIgnoreCase("admin@example.com").orElseThrow();

        ResearchGroup group = researchGroupRepository.save(ResearchGroupTestBuilder.validGroup().build());
        memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                .withUser(admin)
                .withResearchGroup(group)
                .withRole(ResearchGroupMemberRole.ADMIN)
                .build());

        MockHttpSession session = loginAs("admin@example.com");

        CreateProjectRequestDto request = new CreateProjectRequestDto("Project as admin", null);

        mockMvc.perform(post("/api/research-groups/{groupId}/projects", group.getId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Project as admin"))
                .andExpect(jsonPath("$.description").isEmpty());
    }

    @Test
    void shouldReturnForbiddenWhenRequesterIsAnnotator() throws Exception {
        createUser("annotator@example.com");
        User annotator = userRepository.findByEmailIgnoreCase("annotator@example.com").orElseThrow();

        ResearchGroup group = researchGroupRepository.save(ResearchGroupTestBuilder.validGroup().build());
        memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                .withUser(annotator)
                .withResearchGroup(group)
                .withRole(ResearchGroupMemberRole.ANNOTATOR)
                .build());

        MockHttpSession session = loginAs("annotator@example.com");

        CreateProjectRequestDto request = new CreateProjectRequestDto("Forbidden Project", "No permission");

        mockMvc.perform(post("/api/research-groups/{groupId}/projects", group.getId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbiddenWhenRequesterIsNotGroupMember() throws Exception {
        createUser("outsider@example.com");
        ResearchGroup group = researchGroupRepository.save(ResearchGroupTestBuilder.validGroup().build());
        MockHttpSession session = loginAs("outsider@example.com");

        CreateProjectRequestDto request = new CreateProjectRequestDto("Forbidden Project", "No membership");

        mockMvc.perform(post("/api/research-groups/{groupId}/projects", group.getId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbiddenWhenNotAuthenticated() throws Exception {
        ResearchGroup group = researchGroupRepository.save(ResearchGroupTestBuilder.validGroup().build());

        CreateProjectRequestDto request = new CreateProjectRequestDto("Unauthorized", null);

        mockMvc.perform(post("/api/research-groups/{groupId}/projects", group.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectBlankName() throws Exception {
        createUser("owner2@example.com");
        User owner = userRepository.findByEmailIgnoreCase("owner2@example.com").orElseThrow();

        ResearchGroup group = researchGroupRepository.save(ResearchGroupTestBuilder.validGroup().build());
        memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                .withUser(owner)
                .withResearchGroup(group)
                .withRole(ResearchGroupMemberRole.OWNER)
                .build());

        MockHttpSession session = loginAs("owner2@example.com");

        CreateProjectRequestDto request = new CreateProjectRequestDto("   ", "Any");

        mockMvc.perform(post("/api/research-groups/{groupId}/projects", group.getId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
