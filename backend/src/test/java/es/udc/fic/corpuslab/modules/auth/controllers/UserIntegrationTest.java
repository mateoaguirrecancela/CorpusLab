package es.udc.fic.corpuslab.modules.auth.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import es.udc.fic.corpuslab.AbstractIntegrationTest;
import es.udc.fic.corpuslab.modules.auth.dtos.UserRegisterRequestDto;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.modules.auth.testing.fixtures.UserRegisterRequestTestBuilder;
import es.udc.fic.corpuslab.modules.auth.testing.fixtures.UserTestBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanData() {
        userRepository.deleteAll();
    }

    @Test
    void signupCreatesUserAndHashesPassword() throws Exception {
        UserRegisterRequestDto request = UserRegisterRequestTestBuilder.validRequest().build();

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("new.user@example.com"))
                .andExpect(jsonPath("$.firstName").value("New"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.createdAt").isString());

        User saved = userRepository.findByEmailIgnoreCase("new.user@example.com").orElseThrow();
        assertThat(saved.getPasswordHash()).isNotEqualTo("strong-password");
        assertThat(saved.getPasswordHash()).startsWith("$2");
        assertThat(saved.getCountryCode()).isEqualTo("ES");
    }

        @Test
        void signupNormalizesInputFields() throws Exception {
        UserRegisterRequestDto request = UserRegisterRequestTestBuilder.validRequest()
            .withEmail("NEW.USER@EXAMPLE.COM")
            .withFirstName("  New  ")
            .withLastName("  User  ")
            .withCountryCode("es")
            .withCity("  A Coruna  ")
            .build();

        mockMvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("new.user@example.com"))
            .andExpect(jsonPath("$.firstName").value("New"))
            .andExpect(jsonPath("$.lastName").value("User"));

        User saved = userRepository.findByEmailIgnoreCase("new.user@example.com").orElseThrow();
        assertThat(saved.getCountryCode()).isEqualTo("ES");
        assertThat(saved.getCity()).isEqualTo("A Coruna");
        }

    @Test
    void signupReturnsConflictWhenEmailAlreadyExists() throws Exception {
        User existing = UserTestBuilder.validUser().withEmail("existing@example.com").build();
        userRepository.save(existing);

        UserRegisterRequestDto request = UserRegisterRequestTestBuilder.validRequest()
            .withEmail("existing@example.com")
            .withFirstName("Another")
            .withLastName("Person")
            .withBirth(null)
            .withGender(null)
            .withCountryCode(null)
            .withCity(null)
            .withPassword("another-password")
            .build();

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message").value("Email already registered: existing@example.com"));
        }

        @Test
        void signupReturnsBadRequestWhenValidationFails() throws Exception {
        UserRegisterRequestDto request = UserRegisterRequestTestBuilder.validRequest()
            .withEmail("bad-email")
            .withFirstName("   ")
            .withLastName("   ")
            .withBirth(LocalDate.now().plusDays(1))
            .withCountryCode("ESP")
            .withPassword("123")
            .build();

        mockMvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.details.email").exists())
            .andExpect(jsonPath("$.details.firstName").exists())
            .andExpect(jsonPath("$.details.lastName").exists())
            .andExpect(jsonPath("$.details.birth").exists())
            .andExpect(jsonPath("$.details.countryCode").exists())
            .andExpect(jsonPath("$.details.password").exists());
    }
}
