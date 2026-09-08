package es.udc.fic.corpuslab.modules.auth.exceptions;

import org.springframework.http.HttpStatus;

import es.udc.fic.corpuslab.common.exceptions.TranslatableApiException;

public class PasswordResetTokenNotFoundException extends RuntimeException implements TranslatableApiException {
    public PasswordResetTokenNotFoundException() {
        super("Password reset token not found or expired");
    }

    @Override
    public String getMessageKey() {
        return "auth.error.reset.token.notfound";
    }

    @Override
    public Object[] getMessageArgs() {
        return null;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
