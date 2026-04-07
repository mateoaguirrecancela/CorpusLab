package es.udc.fic.corpuslab.modules.project.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequestDto(
        @NotBlank @Size(max = 256) String name,
        @Size(max = 2048) String description) {
}
