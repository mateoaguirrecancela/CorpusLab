package es.udc.fic.corpuslab.common.exceptions;

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
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserLoginRequestTestBuilder;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserTestBuilder;
import es.udc.fic.corpuslab.modules.auth.repositories.OAuthLoginCodeRepository;
import es.udc.fic.corpuslab.modules.auth.repositories.PasswordResetTokenRepository;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupInvitationRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiLocalizationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OAuthLoginCodeRepository oauthLoginCodeRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private ResearchGroupInvitationRepository invitationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @BeforeEach
    void cleanData() {
        oauthLoginCodeRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        invitationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void loginShouldReturnEnglishInvalidCredentialsByDefault() throws Exception {
        User user = UserTestBuilder.validUser()
                .withEmail("i18n.en@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        userRepository.save(user);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        UserLoginRequestTestBuilder.validRequest()
                                .withEmail("i18n.en@example.com")
                                .withPassword("wrong-password")
                                .build())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void loginShouldReturnSpanishInvalidCredentialsWhenAcceptLanguageIsEs() throws Exception {
        User user = UserTestBuilder.validUser()
                .withEmail("i18n.es@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        userRepository.save(user);

        mockMvc.perform(post("/api/auth/login")
                .header("Accept-Language", "es")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        UserLoginRequestTestBuilder.validRequest()
                                .withEmail("i18n.es@example.com")
                                .withPassword("wrong-password")
                                .build())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Correo o contrasena invalidos"));
    }

    @Test
    void loginShouldReturnGalicianInvalidCredentialsWhenAcceptLanguageIsGl() throws Exception {
        User user = UserTestBuilder.validUser()
                .withEmail("i18n.gl@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        userRepository.save(user);

        mockMvc.perform(post("/api/auth/login")
                .header("Accept-Language", "gl")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        UserLoginRequestTestBuilder.validRequest()
                                .withEmail("i18n.gl@example.com")
                                .withPassword("wrong-password")
                                .build())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Correo ou contrasinal invalidos"));
    }

    @Test
    void projectNotFoundShouldReturnEnglishMessageByDefault() throws Exception {
        User user = UserTestBuilder.validUser()
                .withEmail("i18n.project.en@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        userRepository.save(user);

        String token = loginAs("i18n.project.en@example.com");

        mockMvc.perform(get("/api/projects/{projectId}", 999L)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Project not found with id: 999"));
    }

    @Test
    void projectNotFoundShouldReturnSpanishMessageWhenAcceptLanguageIsEs() throws Exception {
        User user = UserTestBuilder.validUser()
                .withEmail("i18n.project.es@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        userRepository.save(user);

        String token = loginAs("i18n.project.es@example.com");

        mockMvc.perform(get("/api/projects/{projectId}", 999L)
                .header("Authorization", "Bearer " + token)
                .header("Accept-Language", "es"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("proyecto con id: 999")));
    }

    @Test
    void projectNotFoundShouldReturnGalicianMessageWhenAcceptLanguageIsGl() throws Exception {
        User user = UserTestBuilder.validUser()
                .withEmail("i18n.project.gl@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        userRepository.save(user);

        String token = loginAs("i18n.project.gl@example.com");

        mockMvc.perform(get("/api/projects/{projectId}", 999L)
                .header("Authorization", "Bearer " + token)
                .header("Accept-Language", "gl"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("proxecto co id: 999")));
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
