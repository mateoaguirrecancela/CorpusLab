package es.udc.fic.corpuslab.modules.researchgroup.dtos;

import jakarta.validation.constraints.NotBlank;

public record JoinResearchGroupByCodeRequestDto(
        @NotBlank(message = "Invitation code is required") String code) {
}
