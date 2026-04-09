package es.udc.fic.corpuslab.modules.project.dtos;

import java.time.Instant;

import es.udc.fic.corpuslab.modules.project.enums.ProjectParticipantRole;

public record ProjectAssignedSummaryDto(
        Long id,
        Long researchGroupId,
        String researchGroupName,
        String name,
        String description,
        boolean setupCompleted,
        ProjectParticipantRole participantRole,
        Instant createdAt) {
}
