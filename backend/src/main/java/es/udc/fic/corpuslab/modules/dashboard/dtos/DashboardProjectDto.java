package es.udc.fic.corpuslab.modules.dashboard.dtos;

import java.time.Instant;

import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectParticipantRole;

public record DashboardProjectDto(
        Long id,
        String name,
        String description,
        int completionPercentage,
        long pendingAnnotations,
        ProjectParticipantRole participantRole,
        String researchGroupName,
        Instant updatedAt) {
}
