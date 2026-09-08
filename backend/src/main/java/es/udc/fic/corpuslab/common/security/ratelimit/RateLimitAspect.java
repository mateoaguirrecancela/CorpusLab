package es.udc.fic.corpuslab.common.security.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import es.udc.fic.corpuslab.modules.auth.services.AuthRateLimiter;
import es.udc.fic.corpuslab.common.security.ClientIpResolver;
import es.udc.fic.corpuslab.modules.auth.dtos.UserLoginRequestDto;
import es.udc.fic.corpuslab.modules.auth.dtos.ForgotPasswordRequestDto;
import es.udc.fic.corpuslab.modules.auth.dtos.ResetPasswordRequestDto;

/**
 * Aspecto AOP que intercepta los controladores anotados con {@link RateLimited}
 * para aplicar el control de rate-limiting de forma centralizada y declarativa.
 */
@Aspect
@Component
public class RateLimitAspect {

    private final AuthRateLimiter authRateLimiter;
    private final ClientIpResolver clientIpResolver;

    public RateLimitAspect(AuthRateLimiter authRateLimiter, ClientIpResolver clientIpResolver) {
        this.authRateLimiter = authRateLimiter;
        this.clientIpResolver = clientIpResolver;
    }

    /**
     * Intercepta métodos anotados con {@link RateLimited} antes de su ejecución.
     *
     * @param joinPoint   Punto de unión que representa la invocación del método.
     * @param rateLimited La anotación que decora el método interceptado.
     */
    @Before("@annotation(rateLimited)")
    public void enforceRateLimit(JoinPoint joinPoint, RateLimited rateLimited) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }

        HttpServletRequest request = attributes.getRequest();
        String ip = clientIpResolver.resolve(request);
        Object[] args = joinPoint.getArgs();

        switch (rateLimited.value()) {
            case LOGIN -> {
                UserLoginRequestDto dto = findArg(args, UserLoginRequestDto.class);
                if (dto != null) {
                    authRateLimiter.checkLogin(dto.email(), ip);
                }
            }
            case FORGOT_PASSWORD -> {
                ForgotPasswordRequestDto dto = findArg(args, ForgotPasswordRequestDto.class);
                if (dto != null) {
                    authRateLimiter.checkForgotPassword(dto.email(), ip);
                }
            }
            case RESET_PASSWORD -> {
                ResetPasswordRequestDto dto = findArg(args, ResetPasswordRequestDto.class);
                if (dto != null) {
                    authRateLimiter.checkResetPassword(dto.token(), ip);
                }
            }
        }
    }

    /**
     * Busca un argumento del tipo solicitado dentro de la lista de argumentos de invocación del método.
     */
    @SuppressWarnings("unchecked")
    private <T> T findArg(Object[] args, Class<T> clazz) {
        for (Object arg : args) {
            if (clazz.isInstance(arg)) {
                return (T) arg;
            }
        }
        return null;
    }
}
