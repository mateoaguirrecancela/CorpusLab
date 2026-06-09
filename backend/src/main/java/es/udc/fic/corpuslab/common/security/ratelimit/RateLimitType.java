package es.udc.fic.corpuslab.common.security.ratelimit;

/**
 * Representa los diferentes tipos o perfiles de rate-limiting soportados por el aspecto AOP.
 */
public enum RateLimitType {
    LOGIN,
    FORGOT_PASSWORD,
    RESET_PASSWORD
}
