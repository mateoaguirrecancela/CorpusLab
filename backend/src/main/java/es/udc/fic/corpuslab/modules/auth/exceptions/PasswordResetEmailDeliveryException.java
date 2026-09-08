package es.udc.fic.corpuslab.modules.auth.exceptions;

import org.springframework.http.HttpStatus;

import es.udc.fic.corpuslab.common.exceptions.TranslatableApiException;

public class PasswordResetEmailDeliveryException extends RuntimeException implements TranslatableApiException {
    public PasswordResetEmailDeliveryException() {
        super("Unable to send reset email at this time");
    }

    @Override
    public String getMessageKey() {
        return "auth.error.reset.email.delivery";
    }

    @Override
    public Object[] getMessageArgs() {
        return null;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.SERVICE_UNAVAILABLE;
    }
}
