package es.udc.fic.corpuslab.modules.auth.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import es.udc.fic.corpuslab.AbstractIntegrationTest;
import es.udc.fic.corpuslab.modules.auth.dtos.UserLoginRequestDto;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserLoginRequestTestBuilder;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserTestBuilder;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSessionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @BeforeEach
    void cleanData() {
        userRepository.deleteAll();
    }

    @Test
    void loginShouldReturnUserDataAndCreateSession() throws Exception {
        User user = UserTestBuilder.validUser()
                .withEmail("login.user@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        userRepository.save(user);

        UserLoginRequestDto request = UserLoginRequestTestBuilder.validRequest()
                .withEmail("LOGIN.USER@EXAMPLE.COM")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("login.user@example.com"))
                .andExpect(jsonPath("$.firstName").value("New"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andReturn();

        Object securityContext = result.getRequest()
                .getSession(false)
                .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(securityContext).isNotNull();
    }

    @Test
    void loginShouldReturnUnauthorizedWhenCredentialsAreInvalid() throws Exception {
        User user = UserTestBuilder.validUser()
                .withEmail("login.user@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        userRepository.save(user);

        UserLoginRequestDto request = UserLoginRequestTestBuilder.validRequest()
                .withEmail("login.user@example.com")
                .withPassword("wrong-password")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andReturn();

        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @Test
    void loginShouldReturnBadRequestWhenPayloadIsInvalid() throws Exception {
        UserLoginRequestDto request = UserLoginRequestTestBuilder.validRequest()
                .withEmail("not-an-email")
                .withPassword("short")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.email").exists())
                .andExpect(jsonPath("$.details.password").exists());
    }

    @Test
    void logoutShouldInvalidateExistingSession() throws Exception {
        User user = UserTestBuilder.validUser()
                .withEmail("logout.user@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        userRepository.save(user);

        UserLoginRequestDto loginRequest = UserLoginRequestTestBuilder.validRequest()
                .withEmail("logout.user@example.com")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(post("/api/auth/logout").session((MockHttpSession) loginResult.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    @Test
    void profileShouldReturnAuthenticatedUserData() throws Exception {
        User user = UserTestBuilder.validUser()
                .withEmail("profile.user@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        userRepository.save(user);

        UserLoginRequestDto loginRequest = UserLoginRequestTestBuilder.validRequest()
                .withEmail("profile.user@example.com")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(get("/api/auth/profile").session((MockHttpSession) loginResult.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("profile.user@example.com"))
                .andExpect(jsonPath("$.firstName").value("New"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.birth").value("1997-05-20"))
                .andExpect(jsonPath("$.gender").value("OTHER"))
                .andExpect(jsonPath("$.countryCode").value("ES"))
                .andExpect(jsonPath("$.city").value("A Coruna"))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist())
                .andExpect(jsonPath("$.updatedAt").doesNotExist());
    }

    @Test
        void profileShouldReturnForbiddenWhenSessionDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/auth/profile"))
                                .andExpect(status().isForbidden());
    }

            @Test
            void profileShouldReturnNotFoundWhenSessionExistsButUserIsDeleted() throws Exception {
                User user = UserTestBuilder.validUser()
                        .withEmail("deleted.profile.user@example.com")
                        .withPasswordHash(passwordEncoder.encode("strong-password"))
                        .build();
                userRepository.save(user);

                UserLoginRequestDto loginRequest = UserLoginRequestTestBuilder.validRequest()
                        .withEmail("deleted.profile.user@example.com")
                        .build();

                MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                        .andExpect(status().isOk())
                        .andReturn();

                userRepository.deleteAll();

                mockMvc.perform(get("/api/auth/profile").session((MockHttpSession) loginResult.getRequest().getSession(false)))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.status").value(404))
                        .andExpect(jsonPath("$.message").value("No account found for email: deleted.profile.user@example.com"));
            }

        @Test
        void logoutShouldSucceedWhenSessionDoesNotExist() throws Exception {
                mockMvc.perform(post("/api/auth/logout"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Logged out successfully"));
        }
}
