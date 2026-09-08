package es.udc.fic.corpuslab.modules.researchgroup.exceptions;

import org.springframework.http.HttpStatus;

import es.udc.fic.corpuslab.common.exceptions.TranslatableApiException;

public class ResearchGroupInvitationAlreadyExistsException extends RuntimeException implements TranslatableApiException {
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

    @Override
    public String getMessageKey() {
        return "researchgroup.invitation.error.already.exists";
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
