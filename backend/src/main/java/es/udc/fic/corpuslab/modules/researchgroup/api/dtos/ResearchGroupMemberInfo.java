package es.udc.fic.corpuslab.modules.researchgroup.api.dtos;

/**
 * Public DTO exposing membership info.
 * Uses String for role to avoid coupling consumers to ResearchGroupMemberRole enum.
 */
public record ResearchGroupMemberInfo(
        Long userId,
        String role) {

    public boolean isOwner() {
        return "OWNER".equals(role);
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public boolean isOwnerOrAdmin() {
        return isOwner() || isAdmin();
    }
}
