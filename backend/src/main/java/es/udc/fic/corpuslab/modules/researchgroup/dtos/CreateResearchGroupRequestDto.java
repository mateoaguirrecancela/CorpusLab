package es.udc.fic.corpuslab.modules.researchgroup.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateResearchGroupRequestDto(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 500) String description
) {
}
