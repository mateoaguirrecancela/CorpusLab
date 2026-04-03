package es.udc.fic.corpuslab.modules.researchgroup.exceptions;

import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupMemberRole;

public class InvalidResearchGroupInvitationRoleException extends RuntimeException {
    private final ResearchGroupMemberRole role;

    public InvalidResearchGroupInvitationRoleException(ResearchGroupMemberRole role) {
        super("Invalid invitation role: " + role);
        this.role = role;
    }

    public ResearchGroupMemberRole getRole() {
        return role;
    }
}
