package es.udc.fic.corpuslab.modules.researchgroup.exceptions;

import org.springframework.http.HttpStatus;

import es.udc.fic.corpuslab.common.exceptions.TranslatableApiException;
import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupMemberRole;

public class InvalidResearchGroupInvitationRoleException extends RuntimeException implements TranslatableApiException {
    private final ResearchGroupMemberRole role;

    public InvalidResearchGroupInvitationRoleException(ResearchGroupMemberRole role) {
        super("Invalid invitation role: " + role);
        this.role = role;
    }

    public ResearchGroupMemberRole getRole() {
        return role;
    }

    @Override
    public String getMessageKey() {
        return "researchgroup.invitation.role.invalid";
    }

    @Override
    public Object[] getMessageArgs() {
        return new Object[] { role.name() };
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
