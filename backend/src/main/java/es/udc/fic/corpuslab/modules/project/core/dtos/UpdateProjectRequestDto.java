package es.udc.fic.corpuslab.modules.project.core.dtos;

import java.util.List;

import es.udc.fic.corpuslab.modules.project.participant.dtos.ProjectParticipantAssignmentDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProjectRequestDto(
        @NotBlank @Size(max = 256) String name,
        @Size(max = 2048) String description,
        List<Long> participantUserIds,
        List<ProjectParticipantAssignmentDto> participantAssignments) {

    public UpdateProjectRequestDto(
            @NotBlank @Size(max = 256) String name,
            @Size(max = 2048) String description,
            List<Long> participantUserIds) {
        this(name, description, participantUserIds, List.of());
    }
}
