package es.udc.fic.corpuslab.modules.auth.exceptions;

import org.springframework.http.HttpStatus;

import es.udc.fic.corpuslab.common.exceptions.TranslatableApiException;

public class EmailNotFoundException extends RuntimeException implements TranslatableApiException {
    private final String email;

    public EmailNotFoundException(String email) {
        super("No account found for email: " + email);
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String getMessageKey() {
        return "auth.error.email.notfound";
    }

    @Override
    public Object[] getMessageArgs() {
        return new Object[] { email };
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
