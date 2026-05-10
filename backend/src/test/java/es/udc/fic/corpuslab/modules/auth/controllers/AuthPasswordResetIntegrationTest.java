package es.udc.fic.corpuslab.modules.auth.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import es.udc.fic.corpuslab.AbstractIntegrationTest;
import es.udc.fic.corpuslab.modules.auth.dtos.ForgotPasswordRequestDto;
import es.udc.fic.corpuslab.modules.auth.dtos.ResetPasswordRequestDto;
import es.udc.fic.corpuslab.modules.auth.entities.PasswordResetToken;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.fixtures.ForgotPasswordRequestTestBuilder;
import es.udc.fic.corpuslab.modules.auth.fixtures.ResetPasswordRequestTestBuilder;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserTestBuilder;
import es.udc.fic.corpuslab.modules.auth.repositories.PasswordResetTokenRepository;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.modules.auth.utils.SecureTokenUtils;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupInvitationRepository;
import es.udc.fic.corpuslab.modules.notification.services.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthPasswordResetIntegrationTest extends AbstractIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private ResearchGroupInvitationRepository invitationRepository;

        @Autowired
        private PasswordResetTokenRepository passwordResetTokenRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @MockitoBean
        private EmailService emailService;

        private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

        @BeforeEach
        void cleanData() {
                invitationRepository.deleteAll();
                passwordResetTokenRepository.deleteAll();
                userRepository.deleteAll();
        }

        @Test
        void forgotPasswordShouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
                ForgotPasswordRequestDto request = ForgotPasswordRequestTestBuilder.validRequest()
                                .withEmail("missing.user@example.com")
                                .build();

                mockMvc.perform(post("/api/auth/forgot-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message")
                                                .value("If the account exists, a reset link has been sent"));

                assertThat(passwordResetTokenRepository.findAll()).isEmpty();
        }

        @Test
        void forgotPasswordShouldGenerateResetTokenDataWhenUserExists() throws Exception {
                User user = UserTestBuilder.validUser()
                                .withEmail("recover.user@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                userRepository.save(user);

                ForgotPasswordRequestDto request = ForgotPasswordRequestTestBuilder.validRequest()
                                .withEmail("RECOVER.USER@EXAMPLE.COM")
                                .build();

                mockMvc.perform(post("/api/auth/forgot-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message")
                                                .value("If the account exists, a reset link has been sent"));

                PasswordResetToken token = passwordResetTokenRepository.findAll().stream().findFirst().orElseThrow();
                assertThat(token.getUser().getId()).isNotNull();
                assertThat(token.getToken()).isNotBlank();
                assertThat(token.getToken()).hasSize(64);
                assertThat(token.getExpiryDate()).isAfter(LocalDateTime.now());
        }

        @Test
        void forgotPasswordShouldReturnServiceUnavailableWhenEmailDeliveryFails() throws Exception {
                User user = UserTestBuilder.validUser()
                                .withEmail("recover.user@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                userRepository.save(user);

                doThrow(new RuntimeException("smtp down"))
                                .when(emailService)
                                .sendPasswordResetEmail(org.mockito.ArgumentMatchers.eq("recover.user@example.com"),
                                                org.mockito.ArgumentMatchers.any(String.class));

                ForgotPasswordRequestDto request = ForgotPasswordRequestTestBuilder.validRequest()
                                .withEmail("recover.user@example.com")
                                .build();

                mockMvc.perform(post("/api/auth/forgot-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isServiceUnavailable())
                                .andExpect(jsonPath("$.status").value(503))
                                .andExpect(jsonPath("$.message").value("Unable to send reset email at this time"));
        }

        @Test
        void resetPasswordShouldReturnNotFoundWhenTokenIsInvalid() throws Exception {
                ResetPasswordRequestDto request = ResetPasswordRequestTestBuilder.validRequest()
                                .withToken("invalid-token-value")
                                .build();

                mockMvc.perform(post("/api/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value(404))
                                .andExpect(jsonPath("$.message").value("Password reset token not found or expired"));
        }

        @Test
        void resetPasswordShouldUpdatePasswordAndClearTokenWhenTokenIsValid() throws Exception {
                String rawToken = "known-valid-reset-token";

                User user = UserTestBuilder.validUser()
                                .withEmail("recover.user@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                userRepository.save(user);

                PasswordResetToken passwordResetToken = new PasswordResetToken();
                passwordResetToken.setToken(SecureTokenUtils.sha256(rawToken));
                passwordResetToken.setUser(user);
                passwordResetToken.setExpiryDate(LocalDateTime.now().plusMinutes(15));
                passwordResetTokenRepository.save(passwordResetToken);

                ResetPasswordRequestDto request = ResetPasswordRequestTestBuilder.validRequest()
                                .withToken(rawToken)
                                .withNewPassword("new-strong-password")
                                .build();

                mockMvc.perform(post("/api/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message")
                                                .value("If the token is valid, password has been reset"));

                User persisted = userRepository.findByEmailIgnoreCase("recover.user@example.com").orElseThrow();
                assertThat(passwordEncoder.matches("new-strong-password", persisted.getPasswordHash())).isTrue();
                assertThat(passwordResetTokenRepository.findByToken(SecureTokenUtils.sha256(rawToken))).isEmpty();
        }

        @Test
        void resetPasswordShouldReturnNotFoundWhenTokenIsExpired() throws Exception {
                String rawToken = "known-expired-reset-token";

                User user = UserTestBuilder.validUser()
                                .withEmail("recover.user@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                userRepository.save(user);

                PasswordResetToken passwordResetToken = new PasswordResetToken();
                passwordResetToken.setToken(SecureTokenUtils.sha256(rawToken));
                passwordResetToken.setUser(user);
                passwordResetToken.setExpiryDate(LocalDateTime.now().minusMinutes(1));
                passwordResetTokenRepository.save(passwordResetToken);

                ResetPasswordRequestDto request = ResetPasswordRequestTestBuilder.validRequest()
                                .withToken(rawToken)
                                .withNewPassword("new-strong-password")
                                .build();

                mockMvc.perform(post("/api/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value(404))
                                .andExpect(jsonPath("$.message").value("Password reset token not found or expired"));

                User persisted = userRepository.findByEmailIgnoreCase("recover.user@example.com").orElseThrow();
                assertThat(passwordEncoder.matches("strong-password", persisted.getPasswordHash())).isTrue();
                assertThat(passwordResetTokenRepository.findByToken(SecureTokenUtils.sha256(rawToken))).isPresent();
        }

        @Test
        void resetPasswordShouldReturnBadRequestWhenValidationFails() throws Exception {
                ResetPasswordRequestDto request = ResetPasswordRequestTestBuilder.validRequest()
                                .withToken("short")
                                .withNewPassword("123")
                                .build();

                mockMvc.perform(post("/api/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.message").value("Validation failed"))
                                .andExpect(jsonPath("$.details.token").exists())
                                .andExpect(jsonPath("$.details.newPassword").exists());
        }
}
