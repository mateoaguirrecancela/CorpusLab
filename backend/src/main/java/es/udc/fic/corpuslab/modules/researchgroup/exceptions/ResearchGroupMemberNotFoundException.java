package es.udc.fic.corpuslab.modules.researchgroup.exceptions;

public class ResearchGroupMemberNotFoundException extends RuntimeException {

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
}
