package es.udc.fic.corpuslab.modules.researchgroup.exceptions;

import org.springframework.http.HttpStatus;

import es.udc.fic.corpuslab.common.exceptions.TranslatableApiException;
import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupMemberRole;

public class InvalidResearchGroupMemberRoleException extends RuntimeException implements TranslatableApiException {

    private final ResearchGroupMemberRole role;

    public InvalidResearchGroupMemberRoleException(ResearchGroupMemberRole role) {
        super("Invalid member role operation for role: " + role);
        this.role = role;
    }

    public ResearchGroupMemberRole getRole() {
        return role;
    }

    @Override
    public String getMessageKey() {
        return "researchgroup.member.role.invalid";
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
