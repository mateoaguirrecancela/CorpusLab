package es.udc.fic.corpuslab.modules.researchgroup.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import es.udc.fic.corpuslab.modules.researchgroup.dtos.CreateResearchGroupRequestDto;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupInvitationRepository;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupMemberRepository;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupRepository;

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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResearchGroupCreateIntegrationTest extends AbstractIntegrationTest {

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
        private PasswordEncoder passwordEncoder;

        private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

        @BeforeEach
        void cleanData() {
                notificationRepository.deleteAll();
                invitationRepository.deleteAll();
                memberRepository.deleteAll();
                researchGroupRepository.deleteAll();
                userRepository.deleteAll();
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

        private void createUser(String email) {
                User user = UserTestBuilder.validUser()
                                .withEmail(email)
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                userRepository.save(user);
        }

        @Test
        void shouldCreateGroupAndReturnSummaryWithOwnerRole() throws Exception {
                createUser("creator@example.com");
                MockHttpSession session = loginAs("creator@example.com");

                CreateResearchGroupRequestDto request = new CreateResearchGroupRequestDto(
                                "NLP Research Lab", "Computational linguistics research");

                mockMvc.perform(post("/api/research-groups")
                                .session(session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").isNumber())
                                .andExpect(jsonPath("$.name").value("NLP Research Lab"))
                                .andExpect(jsonPath("$.description").value("Computational linguistics research"))
                                .andExpect(jsonPath("$.role").value("OWNER"))
                                .andExpect(jsonPath("$.memberCount").value(1))
                                .andExpect(jsonPath("$.createdAt").isNotEmpty());
        }

        @Test
        void shouldCreateGroupWithNullDescription() throws Exception {
                createUser("nodesc@example.com");
                MockHttpSession session = loginAs("nodesc@example.com");

                CreateResearchGroupRequestDto request = new CreateResearchGroupRequestDto(
                                "Minimal Group", null);

                mockMvc.perform(post("/api/research-groups")
                                .session(session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.name").value("Minimal Group"))
                                .andExpect(jsonPath("$.description").isEmpty())
                                .andExpect(jsonPath("$.role").value("OWNER"))
                                .andExpect(jsonPath("$.memberCount").value(1));
        }

        @Test
        void shouldTrimNameAndDescription() throws Exception {
                createUser("trim@example.com");
                MockHttpSession session = loginAs("trim@example.com");

                CreateResearchGroupRequestDto request = new CreateResearchGroupRequestDto(
                                "  Trimmed Name  ", "  Trimmed Desc  ");

                mockMvc.perform(post("/api/research-groups")
                                .session(session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.name").value("Trimmed Name"))
                                .andExpect(jsonPath("$.description").value("Trimmed Desc"));
        }

        @Test
        void shouldAppearInListAfterCreation() throws Exception {
                createUser("lister@example.com");
                MockHttpSession session = loginAs("lister@example.com");

                CreateResearchGroupRequestDto request = new CreateResearchGroupRequestDto(
                                "Listed Group", "Should appear in my groups");

                mockMvc.perform(post("/api/research-groups")
                                .session(session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated());

                mockMvc.perform(get("/api/research-groups").session(session))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1))
                                .andExpect(jsonPath("$[0].name").value("Listed Group"))
                                .andExpect(jsonPath("$[0].role").value("OWNER"));
        }

        @Test
        void shouldRejectBlankName() throws Exception {
                createUser("blank@example.com");
                MockHttpSession session = loginAs("blank@example.com");

                CreateResearchGroupRequestDto request = new CreateResearchGroupRequestDto(
                                "   ", "Some description");

                mockMvc.perform(post("/api/research-groups")
                                .session(session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectMissingName() throws Exception {
                createUser("missing@example.com");
                MockHttpSession session = loginAs("missing@example.com");

                mockMvc.perform(post("/api/research-groups")
                                .session(session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"description\": \"No name\"}"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturnForbiddenWhenNotAuthenticated() throws Exception {
                CreateResearchGroupRequestDto request = new CreateResearchGroupRequestDto(
                                "Unauthorized Group", null);

                mockMvc.perform(post("/api/research-groups")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        void shouldRejectDescriptionTooLong() throws Exception {
                createUser("longdesc@example.com");
                MockHttpSession session = loginAs("longdesc@example.com");

                String tooLongDescription = "a".repeat(2049);
                CreateResearchGroupRequestDto request = new CreateResearchGroupRequestDto(
                                "Group With Long Desc", tooLongDescription);

                mockMvc.perform(post("/api/research-groups")
                                .session(session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }
}
