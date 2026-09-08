package es.udc.fic.corpuslab.modules.researchgroup.dtos;

import java.time.Instant;

import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupInvitationStatus;
import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupMemberRole;

public record ResearchGroupInvitationDto(
        Long id,
        Long researchGroupId,
        String researchGroupName,
        String invitedEmail,
        String inviterFullName,
        ResearchGroupMemberRole role,
        ResearchGroupInvitationStatus status,
        Instant createdAt,
        Instant expiresAt) {
}
