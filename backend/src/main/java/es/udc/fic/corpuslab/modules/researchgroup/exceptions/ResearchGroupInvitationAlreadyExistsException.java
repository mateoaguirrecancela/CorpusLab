package es.udc.fic.corpuslab.modules.researchgroup.exceptions;

public class ResearchGroupInvitationAlreadyExistsException extends RuntimeException {
    private final Long groupId;
    private final String email;

    public ResearchGroupInvitationAlreadyExistsException(Long groupId, String email) {
        super("An active invitation already exists for email " + email + " in group " + groupId);
        this.groupId = groupId;
        this.email = email;
    }

    public Long getGroupId() {
        return groupId;
    }

    public String getEmail() {
        return email;
    }
}
