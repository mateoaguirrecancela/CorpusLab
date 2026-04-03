package es.udc.fic.corpuslab.modules.researchgroup.dtos;

import java.time.Instant;

import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupMemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateResearchGroupInvitationRequestDto(
        @NotBlank(message = "Email is required") @Email(message = "Email format is invalid") String email,
        @NotNull(message = "Invitation role is required") ResearchGroupMemberRole role,
        @NotNull(message = "Invitation expiration is required") @Future(message = "Invitation expiration must be in the future") Instant expiresAt) {
}
