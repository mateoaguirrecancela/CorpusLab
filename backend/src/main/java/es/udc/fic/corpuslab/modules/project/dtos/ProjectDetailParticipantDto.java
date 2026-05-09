package es.udc.fic.corpuslab.modules.project.dtos;

import es.udc.fic.corpuslab.modules.project.enums.ProjectParticipantRole;
import es.udc.fic.corpuslab.modules.project.enums.ProjectParticipantIaaGroup;

public record ProjectDetailParticipantDto(
        Long userId,
        String firstName,
        String lastName,
        String email,
        ProjectParticipantRole role,
        ProjectParticipantIaaGroup iaaGroup,
        int completionPercentage) {
}
