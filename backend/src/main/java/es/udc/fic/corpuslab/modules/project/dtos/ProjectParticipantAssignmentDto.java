package es.udc.fic.corpuslab.modules.project.dtos;

import es.udc.fic.corpuslab.modules.project.enums.ProjectParticipantIaaGroup;

public record ProjectParticipantAssignmentDto(
        Long userId,
        ProjectParticipantIaaGroup iaaGroup) {
}
