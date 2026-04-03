package es.udc.fic.corpuslab.modules.researchgroup.dtos;

import jakarta.validation.constraints.NotNull;

import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupMemberRole;

public record UpdateResearchGroupMemberRoleRequestDto(
        @NotNull ResearchGroupMemberRole role) {
}
