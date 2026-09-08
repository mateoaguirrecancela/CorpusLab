package es.udc.fic.corpuslab.modules.auth.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import es.udc.fic.corpuslab.AbstractIntegrationTest;
import es.udc.fic.corpuslab.modules.auth.entities.OAuthLoginCode;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserTestBuilder;
import es.udc.fic.corpuslab.modules.auth.repositories.OAuthLoginCodeRepository;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.modules.auth.utils.SecureTokenUtils;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupInvitationRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthOAuthExchangeIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OAuthLoginCodeRepository oauthLoginCodeRepository;

    @Autowired
    private ResearchGroupInvitationRepository invitationRepository;

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @BeforeEach
    void cleanData() {
        oauthLoginCodeRepository.deleteAll();
        invitationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void exchangeOAuthCodeShouldAuthenticateUserAndConsumeCode() throws Exception {
        User user = userRepository.save(UserTestBuilder.validUser()
                .withEmail("oauth.exchange.success@example.com")
                .build());
        String rawCode = "oauth-code-abcdefghijklmnopqrstuvwxyz123456";
        String codeHash = SecureTokenUtils.sha256(rawCode);

        OAuthLoginCode loginCode = new OAuthLoginCode();
        loginCode.setCodeHash(codeHash);
        loginCode.setProvider("google");
        loginCode.setUser(user);
        loginCode.setExpiresAt(Instant.now().plusSeconds(120));
        oauthLoginCodeRepository.save(loginCode);

        mockMvc.perform(post("/api/auth/oauth/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("code", rawCode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("oauth.exchange.success@example.com"))
                .andExpect(jsonPath("$.token").isString());

        OAuthLoginCode consumedCode = oauthLoginCodeRepository.findByCodeHash(codeHash).orElseThrow();
        assertThat(consumedCode.getConsumedAt()).isNotNull();
    }

    @Test
    void exchangeOAuthCodeShouldReturnUnauthorizedWhenCodeDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/auth/oauth/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("code", "oauth-code-abcdefghijklmnopqrstuvwxyz654321"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("OAuth login code not found or expired"));
    }

    @Test
    void exchangeOAuthCodeShouldReturnBadRequestWhenPayloadIsInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/oauth/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("code", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.code").exists());
    }
}
