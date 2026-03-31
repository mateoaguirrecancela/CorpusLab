package es.udc.fic.corpuslab.modules.researchgroup.exceptions;

public class ResearchGroupMemberAlreadyExistsException extends RuntimeException {
    private final Long groupId;
    private final String email;

    public ResearchGroupMemberAlreadyExistsException(Long groupId, String email) {
        super("Email " + email + " is already a member of group " + groupId);
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
