package es.udc.fic.corpuslab.modules.auth.exceptions;

import org.springframework.http.HttpStatus;

import es.udc.fic.corpuslab.common.exceptions.TranslatableApiException;

public class EmailAlreadyRegisteredException extends RuntimeException implements TranslatableApiException {
    private final String email;

    public EmailAlreadyRegisteredException(String email) {
        super("Email already registered: " + email);
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String getMessageKey() {
        return "auth.error.email.exists";
    }

    @Override
    public Object[] getMessageArgs() {
        return new Object[] { email };
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.CONFLICT;
    }
}
