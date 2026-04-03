package es.udc.fic.corpuslab.modules.researchgroup.exceptions;

public class ResearchGroupInvitationNotFoundException extends RuntimeException {

    private final Long invitationId;

    public ResearchGroupInvitationNotFoundException(Long invitationId) {
        super("Research group invitation not found with id: " + invitationId);
        this.invitationId = invitationId;
    }

    public Long getInvitationId() {
        return invitationId;
    }
}
