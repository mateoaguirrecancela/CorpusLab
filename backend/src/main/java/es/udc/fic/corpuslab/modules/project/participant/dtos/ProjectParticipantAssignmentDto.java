package es.udc.fic.corpuslab.modules.project.participant.dtos;

import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectParticipantIaaGroup;

public record ProjectParticipantAssignmentDto(
        Long userId,
        ProjectParticipantIaaGroup iaaGroup) {
}
