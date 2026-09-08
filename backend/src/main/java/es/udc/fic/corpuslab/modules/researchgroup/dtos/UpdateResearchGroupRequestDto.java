package es.udc.fic.corpuslab.modules.researchgroup.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateResearchGroupRequestDto(
        @NotBlank @Size(max = 256) String name,
        @Size(max = 2048) String description) {
}
