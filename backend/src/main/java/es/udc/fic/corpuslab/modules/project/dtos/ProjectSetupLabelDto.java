package es.udc.fic.corpuslab.modules.project.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProjectSetupLabelDto(
        @NotBlank @Size(max = 128) String name,
        @Pattern(regexp = "^#([A-Fa-f0-9]{6})$") String color) {
}
