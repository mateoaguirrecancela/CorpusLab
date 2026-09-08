package es.udc.fic.corpuslab.modules.researchgroup.exceptions;

import org.springframework.http.HttpStatus;

import es.udc.fic.corpuslab.common.exceptions.TranslatableApiException;

public class ResearchGroupInvitationEmailDeliveryException extends RuntimeException implements TranslatableApiException {

    public ResearchGroupInvitationEmailDeliveryException(Throwable cause) {
        super("Unable to send invitation email", cause);
    }

    @Override
    public String getMessageKey() {
        return "researchgroup.invitation.error.email.delivery";
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
