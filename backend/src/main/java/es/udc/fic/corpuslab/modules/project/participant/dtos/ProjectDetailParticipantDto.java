package es.udc.fic.corpuslab.modules.project.participant.dtos;

import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectParticipantRole;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectParticipantIaaGroup;

public record ProjectDetailParticipantDto(
        Long userId,
        String firstName,
        String lastName,
        String email,
        ProjectParticipantRole role,
        ProjectParticipantIaaGroup iaaGroup,
        int completionPercentage) {
}
