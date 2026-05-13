package es.udc.fic.corpuslab.modules.auth.services;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import es.udc.fic.corpuslab.modules.auth.exceptions.AuthRateLimitExceededException;
import es.udc.fic.corpuslab.modules.auth.utils.EmailNormalizer;
import es.udc.fic.corpuslab.modules.auth.utils.SecureTokenUtils;

@Service
public class AuthRateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(AuthRateLimiter.class);
    private static final String KEY_PREFIX = "corpuslab:rate-limit:auth:";
    private static final DefaultRedisScript<Long> INCREMENT_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final RateLimitRule loginIpRule;
    private final RateLimitRule loginEmailRule;
    private final RateLimitRule forgotPasswordIpRule;
    private final RateLimitRule forgotPasswordEmailRule;
    private final RateLimitRule resetPasswordIpRule;
    private final RateLimitRule resetPasswordTokenRule;

    public AuthRateLimiter(
            StringRedisTemplate redisTemplate,
            @Value("${app.rate-limit.auth.login.ip.max-attempts:100}") int loginIpMaxAttempts,
            @Value("${app.rate-limit.auth.login.ip.window-seconds:900}") long loginIpWindowSeconds,
            @Value("${app.rate-limit.auth.login.email.max-attempts:10}") int loginEmailMaxAttempts,
            @Value("${app.rate-limit.auth.login.email.window-seconds:900}") long loginEmailWindowSeconds,
            @Value("${app.rate-limit.auth.forgot-password.ip.max-attempts:30}") int forgotPasswordIpMaxAttempts,
            @Value("${app.rate-limit.auth.forgot-password.ip.window-seconds:900}") long forgotPasswordIpWindowSeconds,
            @Value("${app.rate-limit.auth.forgot-password.email.max-attempts:3}") int forgotPasswordEmailMaxAttempts,
            @Value("${app.rate-limit.auth.forgot-password.email.window-seconds:900}") long forgotPasswordEmailWindowSeconds,
            @Value("${app.rate-limit.auth.reset-password.ip.max-attempts:60}") int resetPasswordIpMaxAttempts,
            @Value("${app.rate-limit.auth.reset-password.ip.window-seconds:900}") long resetPasswordIpWindowSeconds,
            @Value("${app.rate-limit.auth.reset-password.token.max-attempts:10}") int resetPasswordTokenMaxAttempts,
            @Value("${app.rate-limit.auth.reset-password.token.window-seconds:900}") long resetPasswordTokenWindowSeconds) {
        this.redisTemplate = redisTemplate;
        this.loginIpRule = new RateLimitRule(loginIpMaxAttempts, Duration.ofSeconds(loginIpWindowSeconds));
        this.loginEmailRule = new RateLimitRule(loginEmailMaxAttempts, Duration.ofSeconds(loginEmailWindowSeconds));
        this.forgotPasswordIpRule = new RateLimitRule(forgotPasswordIpMaxAttempts,
                Duration.ofSeconds(forgotPasswordIpWindowSeconds));
        this.forgotPasswordEmailRule = new RateLimitRule(forgotPasswordEmailMaxAttempts,
                Duration.ofSeconds(forgotPasswordEmailWindowSeconds));
        this.resetPasswordIpRule = new RateLimitRule(resetPasswordIpMaxAttempts,
                Duration.ofSeconds(resetPasswordIpWindowSeconds));
        this.resetPasswordTokenRule = new RateLimitRule(resetPasswordTokenMaxAttempts,
                Duration.ofSeconds(resetPasswordTokenWindowSeconds));
    }

    public void checkLogin(String email, String clientIp) {
        check("login:ip:" + hash(clientIp), loginIpRule);
        check("login:email:" + hash(EmailNormalizer.canonicalizeGoogleEmail(email)), loginEmailRule);
    }

    public void clearSuccessfulLogin(String email) {
        delete("login:email:" + hash(EmailNormalizer.canonicalizeGoogleEmail(email)));
    }

    public void checkForgotPassword(String email, String clientIp) {
        check("forgot-password:ip:" + hash(clientIp), forgotPasswordIpRule);
        check("forgot-password:email:" + hash(EmailNormalizer.canonicalizeGoogleEmail(email)), forgotPasswordEmailRule);
    }

    public void checkResetPassword(String token, String clientIp) {
        check("reset-password:ip:" + hash(clientIp), resetPasswordIpRule);
        check("reset-password:token:" + hash(token.trim()), resetPasswordTokenRule);
    }

    public void clearSuccessfulResetPassword(String token) {
        delete("reset-password:token:" + hash(token.trim()));
    }

    private void check(String keySuffix, RateLimitRule rule) {
        if (rule.maxAttempts() <= 0 || rule.window().isZero() || rule.window().isNegative()) {
            return;
        }

        String key = KEY_PREFIX + keySuffix;
        try {
            Long attempts = redisTemplate.execute(
                    INCREMENT_SCRIPT,
                    List.of(key),
                    String.valueOf(rule.window().toMillis()));
            if (attempts != null && attempts > rule.maxAttempts()) {
                throw new AuthRateLimitExceededException(retryAfterSeconds(key, rule));
            }
        } catch (AuthRateLimitExceededException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            logger.warn("Auth rate limit check skipped because Redis is unavailable for key {}", keySuffix, ex);
        }
    }

    private long retryAfterSeconds(String key, RateLimitRule rule) {
        Long expireSeconds = redisTemplate.getExpire(key);
        if (expireSeconds == null || expireSeconds < 0) {
            return Math.max(1, rule.window().toSeconds());
        }
        return Math.max(1, expireSeconds);
    }

    private void delete(String keySuffix) {
        try {
            redisTemplate.delete(KEY_PREFIX + keySuffix);
        } catch (RuntimeException ex) {
            logger.debug("Could not clear auth rate limit key {}", keySuffix, ex);
        }
    }

    private String hash(String value) {
        return SecureTokenUtils.sha256(value == null ? "" : value.toLowerCase(Locale.ROOT));
    }

    private record RateLimitRule(int maxAttempts, Duration window) {
    }
}
