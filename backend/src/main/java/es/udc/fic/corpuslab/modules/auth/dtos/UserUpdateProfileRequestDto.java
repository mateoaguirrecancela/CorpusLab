package es.udc.fic.corpuslab.modules.auth.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

import es.udc.fic.corpuslab.modules.auth.enums.GenderType;

public record UserUpdateProfileRequestDto(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Past LocalDate birth,
        GenderType gender,
        @Pattern(regexp = "^[A-Za-z]{2}$", message = "{validation.countryCode.invalid}") String countryCode,
        @Size(max = 100) String city
) {
}
