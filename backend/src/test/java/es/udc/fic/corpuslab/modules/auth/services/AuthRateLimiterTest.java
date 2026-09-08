package es.udc.fic.corpuslab.modules.auth.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import es.udc.fic.corpuslab.AbstractIntegrationTest;
import es.udc.fic.corpuslab.modules.auth.exceptions.AuthRateLimitExceededException;

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
@ActiveProfiles("test")
class AuthRateLimiterTest extends AbstractIntegrationTest {

    @Autowired
    private AuthRateLimiter authRateLimiter;

    @Test
    void checkLoginShouldLimitRepeatedAttemptsForSameEmail() {
        String email = uniqueEmail();

        authRateLimiter.checkLogin(email, "203.0.113.10");

        assertThatThrownBy(() -> authRateLimiter.checkLogin(email, "203.0.113.11"))
                .isInstanceOf(AuthRateLimitExceededException.class);
    }

    @Test
    void clearSuccessfulLoginShouldResetEmailLimit() {
        String email = uniqueEmail();

        authRateLimiter.checkLogin(email, "203.0.113.20");
        authRateLimiter.clearSuccessfulLogin(email);
        authRateLimiter.checkLogin(email, "203.0.113.21");
    }

    @Test
    void checkResetPasswordShouldLimitRepeatedAttemptsForSameToken() {
        String token = UUID.randomUUID() + "-known-reset-token";

        authRateLimiter.checkResetPassword(token, "203.0.113.30");

        assertThatThrownBy(() -> authRateLimiter.checkResetPassword(token, "203.0.113.31"))
                .isInstanceOf(AuthRateLimitExceededException.class);
    }

    private String uniqueEmail() {
        return "rate-limit-" + UUID.randomUUID() + "@example.com";
    }
}
