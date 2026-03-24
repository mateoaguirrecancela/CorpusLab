package es.udc.fic.corpuslab.modules.auth.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

import es.udc.fic.corpuslab.modules.auth.enums.GenderType;

public record UserRegisterRequestDto(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Past LocalDate birth,
        GenderType gender,
        @Pattern(regexp = "^[A-Za-z]{2}$", message = "invalid countryCode") String countryCode,
        @Size(max = 100) String city,
        @NotBlank @Size(min = 8, max = 255) String password
) {
}
