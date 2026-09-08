package es.udc.fic.corpuslab.modules.auth.exceptions;

public class AuthRateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public AuthRateLimitExceededException(long retryAfterSeconds) {
        super("Too many authentication requests");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
