package es.udc.fic.corpuslab.modules.auth.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.udc.fic.corpuslab.AbstractIntegrationTest;
import es.udc.fic.corpuslab.modules.auth.dtos.UserRegisterRequestDto;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;

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

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanData() {
        userRepository.deleteAll();
    }

    @Test
    void signupCreatesUserAndHashesPassword() throws Exception {
        UserRegisterRequestDto request = new UserRegisterRequestDto(
                "new.user@example.com",
                "New",
                "User",
                LocalDate.of(1997, 5, 20),
                null,
                "ES",
                "A Coruna",
                "strong-password"
        );

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
    void signupReturnsConflictWhenEmailAlreadyExists() throws Exception {
        User existing = new User();
        existing.setEmail("existing@example.com");
        existing.setFirstName("Existing");
        existing.setLastName("User");
        existing.setPasswordHash("$2a$10$abcdefghijklmnopqrstuv123456789012345678901234567890");
        userRepository.save(existing);

        UserRegisterRequestDto request = new UserRegisterRequestDto(
                "existing@example.com",
                "Another",
                "Person",
                null,
                null,
                null,
                null,
                "another-password"
        );

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
