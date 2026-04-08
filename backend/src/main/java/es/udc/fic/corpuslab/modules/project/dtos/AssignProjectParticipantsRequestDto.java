package es.udc.fic.corpuslab.modules.project.dtos;

import java.util.List;

public record AssignProjectParticipantsRequestDto(List<Long> participantUserIds) {
}
