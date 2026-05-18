package es.udc.fic.corpuslab.modules.researchgroup.exceptions;

import org.springframework.http.HttpStatus;

import es.udc.fic.corpuslab.common.exceptions.TranslatableApiException;

public class ResearchGroupMemberAlreadyExistsException extends RuntimeException implements TranslatableApiException {
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

    @Override
    public String getMessageKey() {
        return "researchgroup.invitation.error.already.member";
    }

    @Override
    public Object[] getMessageArgs() {
        return new Object[] { email, groupId };
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.CONFLICT;
    }
}
