package es.udc.fic.corpuslab.modules.researchgroup.dtos;

import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupMemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateResearchGroupInvitationRequestDto(
        @NotBlank(message = "Email is required") @Email(message = "Email format is invalid") String email,
        @NotNull(message = "Invitation role is required") ResearchGroupMemberRole role) {
}
