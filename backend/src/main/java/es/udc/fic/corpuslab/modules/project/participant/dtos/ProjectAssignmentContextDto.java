package es.udc.fic.corpuslab.modules.project.participant.dtos;

import java.util.List;

public record ProjectAssignmentContextDto(
        Long researchGroupId,
        Long currentUserId,
        List<ProjectAssignableMemberDto> members,
        List<ProjectParticipantAssignmentDto> defaultAssignments) {
}
