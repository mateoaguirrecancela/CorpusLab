package es.udc.fic.corpuslab.modules.auth.exceptions;

import org.springframework.http.HttpStatus;

import es.udc.fic.corpuslab.common.exceptions.TranslatableApiException;

public class InvalidCredentialsException extends RuntimeException implements TranslatableApiException {
    public InvalidCredentialsException() {
        super("Invalid email or password");
    }

    @Override
    public String getMessageKey() {
        return "auth.error.invalid.credentials";
    }

    @Override
    public Object[] getMessageArgs() {
        return null;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.UNAUTHORIZED;
    }
}
