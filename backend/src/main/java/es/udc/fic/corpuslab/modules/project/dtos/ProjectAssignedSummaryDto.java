package es.udc.fic.corpuslab.modules.project.dtos;

import java.time.Instant;

import es.udc.fic.corpuslab.modules.project.enums.ProjectParticipantRole;

public record ProjectAssignedSummaryDto(
                Long id,
                Long researchGroupId,
                String researchGroupName,
                String name,
                String description,
                int completionPercentage,
                ProjectParticipantRole participantRole,
                boolean archived,
                Instant createdAt) {
}
