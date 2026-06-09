package es.udc.fic.corpuslab.common.security.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotación declarativa para aplicar límites de peticiones (rate-limiting) en métodos de controlador.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {

    /**
     * El perfil o tipo de rate-limiting que se aplicará a este método.
     */
    RateLimitType value();
}
