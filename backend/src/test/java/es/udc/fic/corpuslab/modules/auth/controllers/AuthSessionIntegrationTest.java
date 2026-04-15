package es.udc.fic.corpuslab.modules.auth.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import es.udc.fic.corpuslab.AbstractIntegrationTest;
import es.udc.fic.corpuslab.modules.auth.dtos.UserLoginRequestDto;
import es.udc.fic.corpuslab.modules.auth.dtos.UserUpdateProfileRequestDto;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserLoginRequestTestBuilder;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserTestBuilder;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserUpdateProfileRequestTestBuilder;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupInvitationRepository;

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

import java.time.LocalDate;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSessionIntegrationTest extends AbstractIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private ResearchGroupInvitationRepository invitationRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

        @BeforeEach
        void cleanData() {
                invitationRepository.deleteAll();
                userRepository.deleteAll();
        }

        @Test
        void loginShouldReturnUserDataAndToken() throws Exception {
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
                                .andExpect(jsonPath("$.token").isString())
                                .andReturn();

                String token = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
                assertThat(token).isNotBlank();
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

                assertThat(result.getResponse().getContentAsString()).isNotBlank();
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

                mockMvc.perform(post("/api/auth/logout")
                                .header("Authorization", "Bearer "
                                                + objectMapper.readTree(loginResult.getResponse().getContentAsString())
                                                                .get("token").asText()))
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

                mockMvc.perform(get("/api/auth/profile")
                                .header("Authorization", "Bearer "
                                                + objectMapper.readTree(loginResult.getResponse().getContentAsString())
                                                                .get("token").asText()))
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
        void updateProfileShouldPersistAllowedFieldsForAuthenticatedUser() throws Exception {
                User user = UserTestBuilder.validUser()
                                .withEmail("edit.profile.user@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .withFirstName("Before")
                                .withLastName("Edit")
                                .build();
                userRepository.save(user);

                UserLoginRequestDto loginRequest = UserLoginRequestTestBuilder.validRequest()
                                .withEmail("edit.profile.user@example.com")
                                .build();

                MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isOk())
                                .andReturn();

                UserUpdateProfileRequestDto updateRequest = UserUpdateProfileRequestTestBuilder.validRequest()
                                .withFirstName("  Alice  ")
                                .withLastName("  Smith  ")
                                .withCountryCode("pt")
                                .withCity("  ")
                                .build();

                mockMvc.perform(put("/api/auth/profile")
                                .header("Authorization", "Bearer "
                                                + objectMapper.readTree(loginResult.getResponse().getContentAsString())
                                                                .get("token").asText())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value("edit.profile.user@example.com"))
                                .andExpect(jsonPath("$.firstName").value("Alice"))
                                .andExpect(jsonPath("$.lastName").value("Smith"))
                                .andExpect(jsonPath("$.birth").value("1999-06-15"))
                                .andExpect(jsonPath("$.gender").value("FEMALE"))
                                .andExpect(jsonPath("$.countryCode").value("PT"))
                                .andExpect(jsonPath("$.city").isEmpty());

                mockMvc.perform(get("/api/auth/profile")
                                .header("Authorization", "Bearer "
                                                + objectMapper.readTree(loginResult.getResponse().getContentAsString())
                                                                .get("token").asText()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.firstName").value("Alice"))
                                .andExpect(jsonPath("$.lastName").value("Smith"))
                                .andExpect(jsonPath("$.countryCode").value("PT"))
                                .andExpect(jsonPath("$.city").isEmpty());
        }

        @Test
        void updateProfileShouldReturnForbiddenWhenSessionDoesNotExist() throws Exception {
                UserUpdateProfileRequestDto updateRequest = UserUpdateProfileRequestTestBuilder.validRequest().build();

                mockMvc.perform(put("/api/auth/profile")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                                .andExpect(status().isForbidden());
        }

        @Test
        void updateProfileShouldReturnBadRequestWhenPayloadIsInvalid() throws Exception {
                User user = UserTestBuilder.validUser()
                                .withEmail("validation.profile.user@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                userRepository.save(user);

                UserLoginRequestDto loginRequest = UserLoginRequestTestBuilder.validRequest()
                                .withEmail("validation.profile.user@example.com")
                                .build();

                MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isOk())
                                .andReturn();

                UserUpdateProfileRequestDto invalidRequest = UserUpdateProfileRequestTestBuilder.validRequest()
                                .withFirstName("   ")
                                .withCountryCode("XXXX")
                                .build();

                mockMvc.perform(put("/api/auth/profile")
                                .header("Authorization", "Bearer "
                                                + objectMapper.readTree(loginResult.getResponse().getContentAsString())
                                                                .get("token").asText())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.message").value("Validation failed"))
                                .andExpect(jsonPath("$.details.firstName").exists())
                                .andExpect(jsonPath("$.details.countryCode").exists());
        }

        @Test
        void updateProfileShouldReturnBadRequestWhenBirthDateIsInFuture() throws Exception {
                User user = UserTestBuilder.validUser()
                                .withEmail("future.birth.profile.user@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                userRepository.save(user);

                UserLoginRequestDto loginRequest = UserLoginRequestTestBuilder.validRequest()
                                .withEmail("future.birth.profile.user@example.com")
                                .build();

                MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isOk())
                                .andReturn();

                UserUpdateProfileRequestDto invalidRequest = UserUpdateProfileRequestTestBuilder.validRequest()
                                .withBirth(LocalDate.now().plusDays(1))
                                .build();

                mockMvc.perform(put("/api/auth/profile")
                                .header("Authorization", "Bearer "
                                                + objectMapper.readTree(loginResult.getResponse().getContentAsString())
                                                                .get("token").asText())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.message").value("Validation failed"))
                                .andExpect(jsonPath("$.details.birth").exists());
        }

        @Test
        void updateProfileShouldReturnBadRequestWhenLastNameIsBlank() throws Exception {
                User user = UserTestBuilder.validUser()
                                .withEmail("blank.lastname.profile.user@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                userRepository.save(user);

                UserLoginRequestDto loginRequest = UserLoginRequestTestBuilder.validRequest()
                                .withEmail("blank.lastname.profile.user@example.com")
                                .build();

                MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isOk())
                                .andReturn();

                UserUpdateProfileRequestDto invalidRequest = UserUpdateProfileRequestTestBuilder.validRequest()
                                .withLastName("   ")
                                .build();

                mockMvc.perform(put("/api/auth/profile")
                                .header("Authorization", "Bearer "
                                                + objectMapper.readTree(loginResult.getResponse().getContentAsString())
                                                                .get("token").asText())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.message").value("Validation failed"))
                                .andExpect(jsonPath("$.details.lastName").exists());
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

                invitationRepository.deleteAll();
                userRepository.deleteAll();

                mockMvc.perform(get("/api/auth/profile")
                                .header("Authorization", "Bearer "
                                                + objectMapper.readTree(loginResult.getResponse().getContentAsString())
                                                                .get("token").asText()))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value(404))
                                .andExpect(jsonPath("$.message")
                                                .value("No account found for email: deleted.profile.user@example.com"));
        }

        @Test
        void logoutShouldSucceedWhenSessionDoesNotExist() throws Exception {
                mockMvc.perform(post("/api/auth/logout"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Logged out successfully"));
        }
}
