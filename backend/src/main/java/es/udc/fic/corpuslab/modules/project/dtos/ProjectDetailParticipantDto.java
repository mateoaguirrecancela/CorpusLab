package es.udc.fic.corpuslab.modules.project.dtos;

import es.udc.fic.corpuslab.modules.project.enums.ProjectParticipantRole;

public record ProjectDetailParticipantDto(
        Long userId,
        String firstName,
        String lastName,
        String email,
        ProjectParticipantRole role,
        int completionPercentage) {
}