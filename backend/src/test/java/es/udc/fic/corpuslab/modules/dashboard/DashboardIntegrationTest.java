package es.udc.fic.corpuslab.modules.dashboard;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import es.udc.fic.corpuslab.modules.auth.fixtures.UserLoginRequestTestBuilder;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserTestBuilder;
import es.udc.fic.corpuslab.modules.auth.repositories.OAuthLoginCodeRepository;
import es.udc.fic.corpuslab.modules.auth.repositories.PasswordResetTokenRepository;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.modules.notification.repositories.NotificationRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.DatasetItemRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.ProjectParticipantRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.ProjectRepository;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupInvitationRepository;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupMemberRepository;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private OAuthLoginCodeRepository oauthLoginCodeRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private ResearchGroupInvitationRepository invitationRepository;

    @Autowired
    private ResearchGroupMemberRepository memberRepository;

    @Autowired
    private DatasetItemRepository datasetItemRepository;

    @Autowired
    private ProjectParticipantRepository projectParticipantRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ResearchGroupRepository researchGroupRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @BeforeEach
    void cleanData() {
        notificationRepository.deleteAll();
        oauthLoginCodeRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        invitationRepository.deleteAll();
        memberRepository.deleteAll();
        datasetItemRepository.deleteAll();
        projectParticipantRepository.deleteAll();
        projectRepository.deleteAll();
        researchGroupRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void metricsShouldReturnOkForAuthenticatedUser() throws Exception {
        createUser("dashboard.metrics@example.com");
        String session = loginAs("dashboard.metrics@example.com");

        mockMvc.perform(get("/api/dashboard/metrics")
                .header("Authorization", "Bearer " + session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeProjects").value(0))
                .andExpect(jsonPath("$.activeResearchGroups").value(0))
                .andExpect(jsonPath("$.pendingAnnotations").value(0));
    }

    @Test
    void annotationTrendsShouldReturnOkForAuthenticatedUser() throws Exception {
        createUser("dashboard.trends@example.com");
        String session = loginAs("dashboard.trends@example.com");

        mockMvc.perform(get("/api/dashboard/annotation-trends")
                .header("Authorization", "Bearer " + session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.series").isArray())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void projectsShouldReturnOkForAuthenticatedUser() throws Exception {
        createUser("dashboard.projects@example.com");
        String session = loginAs("dashboard.projects@example.com");

        mockMvc.perform(get("/api/dashboard/projects")
                .header("Authorization", "Bearer " + session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentProjects").isArray())
                .andExpect(jsonPath("$.advancedProjects").isArray());
    }

    @Test
    void metricsShouldReturnUnauthorizedWithoutSession() throws Exception {
        mockMvc.perform(get("/api/dashboard/metrics"))
                .andExpect(status().isUnauthorized());
    }

    private void createUser(String email) {
        userRepository.save(UserTestBuilder.validUser()
                .withEmail(email)
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build());
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
}
