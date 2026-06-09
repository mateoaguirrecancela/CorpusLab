package es.udc.fic.corpuslab.common.security.ratelimit;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import es.udc.fic.corpuslab.common.security.ClientIpResolver;
import es.udc.fic.corpuslab.modules.auth.dtos.ForgotPasswordRequestDto;
import es.udc.fic.corpuslab.modules.auth.dtos.ResetPasswordRequestDto;
import es.udc.fic.corpuslab.modules.auth.dtos.UserLoginRequestDto;
import es.udc.fic.corpuslab.modules.auth.services.AuthRateLimiter;

@ExtendWith(MockitoExtension.class)
class RateLimitAspectTest {

    @Mock
    private AuthRateLimiter authRateLimiter;

    @Mock
    private ClientIpResolver clientIpResolver;

    @Mock
    private JoinPoint joinPoint;

    private RateLimitAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new RateLimitAspect(authRateLimiter, clientIpResolver);
    }

    @AfterEach
    void cleanUp() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void enforceRateLimitShouldReturnWhenNoRequestContextIsAvailable() {
        aspect.enforceRateLimit(joinPoint, annotationFor("login"));

        verify(authRateLimiter, never()).checkLogin(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(authRateLimiter, never()).checkForgotPassword(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(authRateLimiter, never()).checkResetPassword(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void enforceRateLimitShouldApplyLoginRuleWhenLoginDtoExists() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        when(clientIpResolver.resolve(org.mockito.ArgumentMatchers.any())).thenReturn("203.0.113.10");
        when(joinPoint.getArgs()).thenReturn(new Object[] { new UserLoginRequestDto("user@example.com", "secret1234") });

        aspect.enforceRateLimit(joinPoint, annotationFor("login"));

        verify(authRateLimiter).checkLogin("user@example.com", "203.0.113.10");
    }

    @Test
    void enforceRateLimitShouldApplyForgotPasswordRuleWhenDtoExists() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        when(clientIpResolver.resolve(org.mockito.ArgumentMatchers.any())).thenReturn("203.0.113.11");
        when(joinPoint.getArgs()).thenReturn(new Object[] { new ForgotPasswordRequestDto("user@example.com") });

        aspect.enforceRateLimit(joinPoint, annotationFor("forgotPassword"));

        verify(authRateLimiter).checkForgotPassword("user@example.com", "203.0.113.11");
    }

    @Test
    void enforceRateLimitShouldApplyResetPasswordRuleWhenDtoExists() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        when(clientIpResolver.resolve(org.mockito.ArgumentMatchers.any())).thenReturn("203.0.113.12");
        when(joinPoint.getArgs()).thenReturn(new Object[] { new ResetPasswordRequestDto("token-value-1234567890", "password123") });

        aspect.enforceRateLimit(joinPoint, annotationFor("resetPassword"));

        verify(authRateLimiter).checkResetPassword("token-value-1234567890", "203.0.113.12");
    }

    @Test
    void enforceRateLimitShouldSkipRuleWhenExpectedDtoIsMissing() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        when(clientIpResolver.resolve(org.mockito.ArgumentMatchers.any())).thenReturn("203.0.113.13");
        when(joinPoint.getArgs()).thenReturn(new Object[] { "not-a-dto" });

        aspect.enforceRateLimit(joinPoint, annotationFor("login"));

        verify(authRateLimiter, never()).checkLogin(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private RateLimited annotationFor(String methodName) {
        try {
            return RateLimitTarget.class.getDeclaredMethod(methodName).getAnnotation(RateLimited.class);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static final class RateLimitTarget {

        @RateLimited(RateLimitType.LOGIN)
        public void login() {
        }

        @RateLimited(RateLimitType.FORGOT_PASSWORD)
        public void forgotPassword() {
        }

        @RateLimited(RateLimitType.RESET_PASSWORD)
        public void resetPassword() {
        }
    }
}
