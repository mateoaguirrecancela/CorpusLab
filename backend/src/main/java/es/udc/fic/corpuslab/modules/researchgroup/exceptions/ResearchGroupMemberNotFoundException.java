package es.udc.fic.corpuslab.modules.researchgroup.exceptions;

import org.springframework.http.HttpStatus;

import es.udc.fic.corpuslab.common.exceptions.TranslatableApiException;

public class ResearchGroupMemberNotFoundException extends RuntimeException implements TranslatableApiException {

    private final Long groupId;
    private final Long userId;

    public ResearchGroupMemberNotFoundException(Long groupId, Long userId) {
        super("Research group member not found. groupId=" + groupId + ", userId=" + userId);
        this.groupId = groupId;
        this.userId = userId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public String getMessageKey() {
        return "researchgroup.member.notfound";
    }

    @Override
    public Object[] getMessageArgs() {
        return new Object[] { groupId, userId };
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
