package es.udc.fic.corpuslab.modules.project.participant.dtos;

import java.util.List;

public record AssignProjectParticipantsRequestDto(
        List<Long> participantUserIds,
        List<ProjectParticipantAssignmentDto> participantAssignments) {

    public AssignProjectParticipantsRequestDto(List<Long> participantUserIds) {
        this(participantUserIds, List.of());
    }
}
