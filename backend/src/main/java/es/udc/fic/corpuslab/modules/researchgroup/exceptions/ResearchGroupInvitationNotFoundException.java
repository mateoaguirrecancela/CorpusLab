package es.udc.fic.corpuslab.modules.researchgroup.exceptions;

import org.springframework.http.HttpStatus;

import es.udc.fic.corpuslab.common.exceptions.TranslatableApiException;

public class ResearchGroupInvitationNotFoundException extends RuntimeException implements TranslatableApiException {

    private final Long invitationId;

    public ResearchGroupInvitationNotFoundException(Long invitationId) {
        super("Research group invitation not found with id: " + invitationId);
        this.invitationId = invitationId;
    }

    public Long getInvitationId() {
        return invitationId;
    }

    @Override
    public String getMessageKey() {
        return "researchgroup.invitation.notfound";
    }

    @Override
    public Object[] getMessageArgs() {
        return new Object[] { invitationId };
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
