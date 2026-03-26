package es.udc.fic.corpuslab.modules.auth.dtos;

import java.time.LocalDate;

import es.udc.fic.corpuslab.modules.auth.enums.GenderType;

public record UserProfileResponseDto(
        String email,
        String firstName,
        String lastName,
        LocalDate birth,
        GenderType gender,
        String countryCode,
        String city
) {
}