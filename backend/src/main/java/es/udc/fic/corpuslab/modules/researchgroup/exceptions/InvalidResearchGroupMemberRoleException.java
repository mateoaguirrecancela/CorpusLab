package es.udc.fic.corpuslab.modules.researchgroup.exceptions;

import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupMemberRole;

public class InvalidResearchGroupMemberRoleException extends RuntimeException {

    private final ResearchGroupMemberRole role;

    public InvalidResearchGroupMemberRoleException(ResearchGroupMemberRole role) {
        super("Invalid member role operation for role: " + role);
        this.role = role;
    }

    public ResearchGroupMemberRole getRole() {
        return role;
    }
}
