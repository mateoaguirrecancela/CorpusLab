package es.udc.fic.corpuslab.modules.auth.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

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
import es.udc.fic.corpuslab.modules.auth.dtos.ForgotPasswordRequestDto;
import es.udc.fic.corpuslab.modules.auth.dtos.ResetPasswordRequestDto;
import es.udc.fic.corpuslab.modules.auth.dtos.UserLoginRequestDto;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.fixtures.ForgotPasswordRequestTestBuilder;
import es.udc.fic.corpuslab.modules.auth.fixtures.ResetPasswordRequestTestBuilder;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserLoginRequestTestBuilder;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserTestBuilder;
import es.udc.fic.corpuslab.modules.auth.repositories.OAuthLoginCodeRepository;
import es.udc.fic.corpuslab.modules.auth.repositories.PasswordResetTokenRepository;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupInvitationRepository;

@SpringBootTest(properties = {
        "app.rate-limit.auth.login.ip.max-attempts=100",
        "app.rate-limit.auth.login.email.max-attempts=1",
        "app.rate-limit.auth.login.email.window-seconds=60",
        "app.rate-limit.auth.forgot-password.ip.max-attempts=100",
        "app.rate-limit.auth.forgot-password.email.max-attempts=1",
        "app.rate-limit.auth.forgot-password.email.window-seconds=60",
        "app.rate-limit.auth.reset-password.ip.max-attempts=100",
        "app.rate-limit.auth.reset-password.token.max-attempts=1",
        "app.rate-limit.auth.reset-password.token.window-seconds=60"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthRateLimitIntegrationTest extends AbstractIntegrationTest {

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
    void loginShouldReturnTooManyRequestsAfterRateLimitIsReached() throws Exception {
        User user = UserTestBuilder.validUser()
                .withEmail("ratelimit.login@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        userRepository.save(user);

        UserLoginRequestDto request = UserLoginRequestTestBuilder.validRequest()
                .withEmail("ratelimit.login@example.com")
                .withPassword("wrong-password")
                .build();

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        MvcResult rateLimitedResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.message").value("Too many attempts. Please try again later"))
                .andReturn();

        assertPositiveRetryAfter(rateLimitedResult);
    }

    @Test
    void forgotPasswordShouldReturnTooManyRequestsAfterRateLimitIsReached() throws Exception {
        ForgotPasswordRequestDto request = ForgotPasswordRequestTestBuilder.validRequest()
                .withEmail("ratelimit.forgot+" + UUID.randomUUID() + "@example.com")
                .build();

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        MvcResult rateLimitedResult = mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.message").value("Too many attempts. Please try again later"))
                .andReturn();

        assertPositiveRetryAfter(rateLimitedResult);
    }

    @Test
    void resetPasswordShouldReturnTooManyRequestsAfterRateLimitIsReached() throws Exception {
        ResetPasswordRequestDto request = ResetPasswordRequestTestBuilder.validRequest()
                .withToken("valid-token-" + UUID.randomUUID())
                .build();

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        MvcResult rateLimitedResult = mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.message").value("Too many attempts. Please try again later"))
                .andReturn();

        assertPositiveRetryAfter(rateLimitedResult);
    }

    private void assertPositiveRetryAfter(MvcResult result) {
        String retryAfterHeader = result.getResponse().getHeader("Retry-After");
        assertThat(retryAfterHeader).isNotBlank();
        assertThat(Long.parseLong(retryAfterHeader)).isPositive();
    }
}
