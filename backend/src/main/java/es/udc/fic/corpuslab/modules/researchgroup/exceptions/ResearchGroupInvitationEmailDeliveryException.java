package es.udc.fic.corpuslab.modules.researchgroup.exceptions;

public class ResearchGroupInvitationEmailDeliveryException extends RuntimeException {

    public ResearchGroupInvitationEmailDeliveryException(Throwable cause) {
        super("Unable to send invitation email", cause);
    }
}
