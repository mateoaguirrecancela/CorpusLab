package es.udc.fic.corpuslab.modules.project.participant.dtos;

public record ProjectAssignableMemberDto(
        Long userId,
        String firstName,
        String lastName,
        String email) {
}
