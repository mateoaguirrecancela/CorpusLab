package es.udc.fic.corpuslab.modules.auth.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

import es.udc.fic.corpuslab.common.validation.MinAge;
import es.udc.fic.corpuslab.modules.auth.enums.GenderType;

public record UserUpdateProfileRequestDto(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotNull @Past @MinAge(16) LocalDate birth,
        @NotNull GenderType gender,
        @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$", message = "{validation.countryCode.invalid}") String countryCode,
        @NotBlank @Size(max = 100) String city
) {
}
